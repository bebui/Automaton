/*
 * Automaton
 * Copyright (c) 2026, Julien Menana, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package fr.menana.automaton.weighted;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.internal.WeightMath;
import fr.menana.automaton.weighted.compiled.*;

import java.util.*;

/**
 * Operations preserving the full word/vector relation. No Pareto pruning is performed.
 */
public final class MultiWeightedOperations {
    private MultiWeightedOperations() { }
    private static void sameDimensions(MultiWeightedAutomaton a, MultiWeightedAutomaton b) {
        if (a.dimensions() != b.dimensions()) throw new IllegalArgumentException("Equal dimensions required for componentwise addition");
    }
    private static double[] add(double[] a, double[] b) { for (int d = 0; d < a.length; d++) a[d] = WeightMath.add(a[d], b[d]); return a; }
    private static int[] copy(MultiWeightedAutomaton a, MultiWeightedAutomaton.Builder b, boolean finals) {
        AutomatonGraph g = a.graph(); int[] map = new int[g.getNbStates()];
        for (int s = 0; s < map.length; s++) { map[s] = b.addState(); if (finals && g.accept[s]) b.setAccept(map[s], a.finalVector(s)); }
        for (int e = 0; e < g.source.length; e++) {
            if (g.epsilon[e]) b.addEpsilonTransition(map[g.source[e]], map[g.target[e]], a.weights(e));
            else b.addTransition(map[g.source[e]], map[g.target[e]], g.labels(e), a.weights(e));
        }
        return map;
    }
    /**
     * @param a left
     * @param b right
     * @return union of all vectors for each word; dimensions must agree
     */
    public static MultiWeightedAutomaton union(MultiWeightedAutomaton a, MultiWeightedAutomaton b) {
        sameDimensions(a, b); MultiWeightedAutomaton.Builder out = MultiWeightedAutomaton.builder(a.dimensions());
        int root = out.addState(); out.setInitial(root);
        int[] left = copy(a, out, true), right = copy(b, out, true);
        if (a.graph().initial >= 0) out.addEpsilonTransition(root, left[a.graph().initial], a.initialVector());
        if (b.graph().initial >= 0) out.addEpsilonTransition(root, right[b.graph().initial], b.initialVector());
        return out.build();
    }
    /**
     * @param a prefix
     * @param b suffix
     * @return every split with componentwise summed costs; dimensions must agree
     */
    public static MultiWeightedAutomaton concatenate(MultiWeightedAutomaton a, MultiWeightedAutomaton b) {
        sameDimensions(a, b); MultiWeightedAutomaton.Builder out = MultiWeightedAutomaton.builder(a.dimensions());
        int[] left = copy(a, out, false), right = copy(b, out, true);
        if (a.graph().initial >= 0 && b.graph().initial >= 0) {
            out.setInitial(left[a.graph().initial], a.initialVector());
            for (int s = 0; s < left.length; s++) if (a.graph().accept[s]) out.addEpsilonTransition(left[s], right[b.graph().initial], add(a.finalVector(s), b.initialVector()));
        }
        return out.build();
    }
    /**
     * @param a operand
     * @return reversed full relation; summation rounding may differ
     */
    public static MultiWeightedAutomaton revert(MultiWeightedAutomaton a) {
        AutomatonGraph g = a.graph(); MultiWeightedAutomaton.Builder out = MultiWeightedAutomaton.builder(a.dimensions());
        if (g.initial < 0) return out.build();
        for (int s = 0; s < g.getNbStates(); s++) out.addState(); int root = out.addState(); out.setInitial(root);
        out.setAccept(g.initial, a.initialVector());
        for (int s = 0; s < g.getNbStates(); s++) if (g.accept[s]) out.addEpsilonTransition(root, s, a.finalVector(s));
        for (int e = 0; e < g.source.length; e++) {
            if (g.epsilon[e]) out.addEpsilonTransition(g.target[e], g.source[e], a.weights(e));
            else out.addTransition(g.target[e], g.source[e], g.labels(e), a.weights(e));
        }
        return out.build();
    }
    private record Pair(int left, int right, int phase) { }
    /**
     * Uses the canonical epsilon interleaving filter; paired consuming labels are intersected symbolically.
     * @param a left
     * @param b right
     * @return product with componentwise sums, without dominance pruning
     */
    public static MultiWeightedAutomaton intersection(MultiWeightedAutomaton a, MultiWeightedAutomaton b) {
        sameDimensions(a, b); AutomatonGraph ga = a.graph(), gb = b.graph();
        MultiWeightedAutomaton.Builder out = MultiWeightedAutomaton.builder(a.dimensions());
        if (ga.initial < 0 || gb.initial < 0) return out.build();
        Map<Pair, Integer> ids = new HashMap<>(); List<Pair> queue = new ArrayList<>();
        Pair root = new Pair(ga.initial, gb.initial, 0); ids.put(root, out.addState()); queue.add(root);
        out.setInitial(0, add(a.initialVector(), b.initialVector()));
        for (int at = 0; at < queue.size(); at++) {
            Pair p = queue.get(at); int from = ids.get(p);
            if (ga.accept[p.left] && gb.accept[p.right]) out.setAccept(from, add(a.finalVector(p.left), b.finalVector(p.right)));
            for (int i = ga.outStart[p.left]; i < ga.outStart[p.left + 1]; i++) {
                int e = ga.outEdges[i];
                if (ga.epsilon[e]) {
                    if (p.phase != 2) out.addEpsilonTransition(from, pairState(new Pair(ga.target[e], p.right, 1), ids, queue, out), a.weights(e));
                    continue;
                }
                for (int j = gb.outStart[p.right]; j < gb.outStart[p.right + 1]; j++) {
                    int f = gb.outEdges[j]; if (gb.epsilon[f]) continue;
                    IntervalSet common = ga.labels(e).intersection(gb.labels(f));
                    if (!common.isEmpty()) out.addTransition(from, pairState(new Pair(ga.target[e], gb.target[f], 0), ids, queue, out), common, add(a.weights(e), b.weights(f)));
                }
            }
            for (int j = gb.outStart[p.right]; j < gb.outStart[p.right + 1]; j++) {
                int e = gb.outEdges[j]; if (gb.epsilon[e]) out.addEpsilonTransition(from, pairState(new Pair(p.left, gb.target[e], 2), ids, queue, out), b.weights(e));
            }
        }
        return out.build();
    }
    private static int pairState(Pair p, Map<Pair, Integer> ids, List<Pair> queue, MultiWeightedAutomaton.Builder out) {
        Integer found = ids.get(p); if (found != null) return found;
        int id = out.addState(); ids.put(p, id); queue.add(p); return id;
    }
    private static MultiWeightedAutomaton embed(MultiWeightedAutomaton a, int dimensions, int offset) {
        return new MultiWeightedAutomaton(a.graph(), dimensions, new MultiWeightedAutomaton.Vectors() {
            public double edge(int e, int d) { return d < offset || d >= offset + a.dimensions() ? 0 : a.weight(e, d - offset); }
            public double initial(int d) { return d < offset || d >= offset + a.dimensions() ? 0 : a.initialWeight(d - offset); }
            public double terminal(int s, int d) { return d < offset || d >= offset + a.dimensions() ? 0 : a.finalWeight(s, d - offset); }
        });
    }
    /**
     * @param a left resources
     * @param b right independent resources
     * @return synchronized product of dimension da+db
     */
    public static MultiWeightedAutomaton productIndependent(MultiWeightedAutomaton a, MultiWeightedAutomaton b) {
        int d = Math.addExact(a.dimensions(), b.dimensions()); return intersection(embed(a, d, 0), embed(b, d, a.dimensions()));
    }
    /**
     * Synchronizes scalar operands on their common words, keeping one independent
     * dimension per operand, including initial and terminal costs. All combinations
     * of accepting paths are retained, even when operands share a graph. This is
     * different from the correlated view {@link MultiWeightedAutomaton#fromWeightings}.
     * Labels are intersected symbolically; epsilon interleavings use the existing
     * canonical filter. Output can be a product of operand graph sizes.
     * @param automata one or more scalar operands
     * @return full word/vector relation, with no dominance filtering
     */
    public static MultiWeightedAutomaton synchronizedProduct(WeightedAutomaton... automata) {
        if (automata.length == 0) throw new IllegalArgumentException("At least one operand required");
        WeightedAutomaton[] operands = automata.clone();
        for (WeightedAutomaton a : operands) Objects.requireNonNull(a);
        MultiWeightedAutomaton result = MultiWeightedAutomaton.fromWeightings(operands[0]);
        for (int i = 1; i < operands.length; i++)
            result = productIndependent(result, MultiWeightedAutomaton.fromWeightings(operands[i]));
        return result;
    }
    /**
     * @param a prefix resources
     * @param b suffix independent resources
     * @return concatenation with disjoint resource blocks
     */
    public static MultiWeightedAutomaton concatenateIndependent(MultiWeightedAutomaton a, MultiWeightedAutomaton b) {
        int d = Math.addExact(a.dimensions(), b.dimensions()); return concatenate(embed(a, d, 0), embed(b, d, a.dimensions()));
    }
    /**
     * @param a operand
     * @return useful-state subgraph preserving every accepting path/vector
     */
    public static MultiWeightedAutomaton trim(MultiWeightedAutomaton a) {
        AutomatonGraph g = a.graph(); boolean[] useful = g.usefulStates(); int[] map = new int[useful.length]; Arrays.fill(map, -1);
        MultiWeightedAutomaton.Builder out = MultiWeightedAutomaton.builder(a.dimensions());
        for (int s = 0; s < map.length; s++) if (useful[s]) { map[s] = out.addState(); if (g.accept[s]) out.setAccept(map[s], a.finalVector(s)); }
        if (g.initial >= 0 && useful[g.initial]) out.setInitial(map[g.initial], a.initialVector());
        for (int e = 0; e < g.source.length; e++) if (useful[g.source[e]] && useful[g.target[e]]) {
            if (g.epsilon[e]) out.addEpsilonTransition(map[g.source[e]], map[g.target[e]], a.weights(e));
            else out.addTransition(map[g.source[e]], map[g.target[e]], g.labels(e), a.weights(e));
        }
        return out.build();
    }
    private record Merge(int source, int target, boolean epsilon, List<Double> vector) { }
    /**
     * Trims and unions labels having identical endpoint/mode/vector. No dominated
     * vector is discarded; path multiplicity and original IDs are not preserved.
     * @param input operand
     * @return equivalent full word/vector relation
     */
    public static MultiWeightedAutomaton simplify(MultiWeightedAutomaton input) {
        MultiWeightedAutomaton a = trim(input); AutomatonGraph g = a.graph();
        Map<Merge, IntervalSet> groups = new LinkedHashMap<>();
        for (int e = 0; e < g.source.length; e++) {
            List<Double> vector = new ArrayList<>(); for (int d = 0; d < a.dimensions(); d++) vector.add(a.weight(e, d));
            Merge key = new Merge(g.source[e], g.target[e], g.epsilon[e], List.copyOf(vector));
            IntervalSet labels = groups.computeIfAbsent(key, k -> new IntervalSet()); if (!g.epsilon[e]) labels.add(g.labels(e));
        }
        MultiWeightedAutomaton.Builder out = MultiWeightedAutomaton.builder(a.dimensions());
        for (int s = 0; s < g.getNbStates(); s++) { out.addState(); if (g.accept[s]) out.setAccept(s, a.finalVector(s)); }
        if (g.initial >= 0) out.setInitial(g.initial, a.initialVector());
        for (Map.Entry<Merge, IntervalSet> entry : groups.entrySet()) {
            Merge key = entry.getKey(); double[] vector = new double[a.dimensions()]; for (int d = 0; d < vector.length; d++) vector[d] = key.vector.get(d);
            if (key.epsilon) out.addEpsilonTransition(key.source, key.target, vector); else out.addTransition(key.source, key.target, entry.getValue(), vector);
        }
        return out.build();
    }
}
