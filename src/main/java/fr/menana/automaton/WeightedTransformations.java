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
package fr.menana.automaton;

import java.util.*;

/**
 * Cost-preserving transformations with explicit family restrictions.
 * Residual determinization and weighted minimization are certified here only
 * for DAGs with bounded integer costs. Arbitrary cyclic determinization is
 * deliberately rejected: a finite result need not exist.
 */
public final class WeightedTransformations {
    private WeightedTransformations() { }

    /**
     * Eliminates epsilon by shortest epsilon-prefix costs and new terminal costs.
     * Preserves minimum word costs, not all paths or vector relations. A reachable
     * negative epsilon cycle is rejected, since finite edge costs cannot encode it.
     * Worst case O(V^2 Eepsilon + VE) time, O(V+E) scratch plus possibly quadratic output.
     * @param input operand
     * @return epsilon-free weighting
     */
    public static WeightedAutomaton removeEpsilon(WeightedAutomaton input) {
        if (!input.hasEpsilon) return input;
        WeightedAutomaton a = input.trim(); AutomatonGraph g = a.graph(); int n = g.getNbStates();
        if (g.initial < 0) return a;
        WeightedAutomaton.Builder eps = WeightedAutomaton.builder();
        for (int s = 0; s < n; s++) eps.addState();
        for (int e = 0; e < g.source.length; e++) if (g.epsilon[e]) eps.addEpsilonTransition(g.source[e], g.target[e], a.weight(e));
        WeightedAutomaton epsilon = eps.build(); WeightedAutomaton.Builder out = WeightedAutomaton.builder();
        for (int s = 0; s < n; s++) out.addState(); out.setInitial(g.initial, a.initialWeight());
        for (int s = 0; s < n; s++) {
            double[] distances = WeightedPaths.shortestPaths(epsilon, s).distances(); double last = Double.POSITIVE_INFINITY;
            for (int t = 0; t < n; t++) {
                if (distances[t] == Double.NEGATIVE_INFINITY) throw new IllegalArgumentException("Negative epsilon cycle cannot be eliminated into finite costs");
                if (g.accept[t] && Double.isFinite(distances[t])) last = Math.min(last, WeightMath.add(distances[t], a.finalWeight(t)));
            }
            if (Double.isFinite(last)) out.setAccept(s, last);
            for (int e = 0; e < g.source.length; e++) if (!g.epsilon[e] && Double.isFinite(distances[g.source[e]]))
                out.addTransition(s, g.target[e], g.labels(e), WeightMath.add(distances[g.source[e]], a.weight(e)));
        }
        return out.build();
    }

    // A deliberately conservative sufficient bound: all normalizations, path sums
    // and potential differences in these finite-depth constructions stay exact.
    private static void certifiedIntegerDag(WeightedAutomaton a) {
        a.graph().requireDag();
        double n = a.graph().getNbStates() + 1.0, limit = 0x1.0p48 / (n * n);
        requireInteger(a.initialWeight(), limit);
        for (int s = 0; s < a.graph().getNbStates(); s++) requireInteger(a.finalWeight(s), limit);
        for (int e = 0; e < a.graph().getNbTransitions(); e++) requireInteger(a.weight(e), limit);
    }
    private static void requireInteger(double value, double bound) {
        if (value != Math.rint(value) || Math.abs(value) > bound)
            throw new IllegalArgumentException("This transformation requires bounded integer costs for exact normalization");
    }
    private static final class Residual {
        final double[] values; final int hash;
        Residual(double[] values) { this.values = values; hash = Arrays.hashCode(values); }
        public int hashCode() { return hash; }
        public boolean equals(Object other) { return other instanceof Residual r && Arrays.equals(values, r.values); }
    }
    /**
     * @param a operand
     * @return deterministic equivalent in the supported certified family
     */
    public static WeightedAutomaton determinizeDag(WeightedAutomaton a) { return determinizeDag(a, Integer.MAX_VALUE); }
    /**
     * Residual subset construction on integer-weighted DAGs; output may be exponential.
     * Epsilon removal is performed after certification. An already deterministic
     * input is returned unchanged, including when cyclic or nonintegral.
     * @param input operand
     * @param maxStates explicit output-state budget
     * @return deterministic cost equivalent
     * @throws IllegalStateException on budget exhaustion (not a proof of nondeterminizability)
     */
    public static WeightedAutomaton determinizeDag(WeightedAutomaton input, int maxStates) {
        if (maxStates <= 0) throw new IllegalArgumentException("Positive state budget required");
        if (input.graph().isDeterministic()) return input;
        certifiedIntegerDag(input);
        WeightedAutomaton a = removeEpsilon(input); AutomatonGraph g = a.graph();
        WeightedAutomaton.Builder out = WeightedAutomaton.builder(); if (g.initial < 0) return out.build();
        List<IntervalSet> sets = new ArrayList<>(); for (int e = 0; e < g.source.length; e++) sets.add(g.labels(e));
        Alphabet alphabet = Alphabet.of(sets); int n = g.getNbStates();
        double[] initial = new double[n]; Arrays.fill(initial, Double.POSITIVE_INFINITY); initial[g.initial] = 0;
        List<Residual> queue = new ArrayList<>(); Map<Residual, Integer> ids = new HashMap<>();
        Residual root = new Residual(initial); queue.add(root); ids.put(root, out.addState()); out.setInitial(0, a.initialWeight());
        for (int at = 0; at < queue.size(); at++) {
            Residual current = queue.get(at); double terminal = Double.POSITIVE_INFINITY;
            for (int s = 0; s < n; s++) if (Double.isFinite(current.values[s]) && g.accept[s]) terminal = Math.min(terminal, WeightMath.add(current.values[s], a.finalWeight(s)));
            if (Double.isFinite(terminal)) out.setAccept(at, terminal);
            for (int c = 0; c < alphabet.size(); c++) {
                double[] next = new double[n]; Arrays.fill(next, Double.POSITIVE_INFINITY);
                for (int e = 0; e < g.source.length; e++) if (Double.isFinite(current.values[g.source[e]]) && g.accepts(e, alphabet.min(c)))
                    next[g.target[e]] = Math.min(next[g.target[e]], WeightMath.add(current.values[g.source[e]], a.weight(e)));
                double common = Double.POSITIVE_INFINITY; for (double value : next) common = Math.min(common, value);
                if (!Double.isFinite(common)) continue;
                for (int s = 0; s < n; s++) if (Double.isFinite(next[s])) next[s] = WeightMath.add(next[s], -common);
                Residual target = new Residual(next); Integer id = ids.get(target);
                if (id == null) {
                    if (queue.size() == maxStates) throw new IllegalStateException("Determinization state budget exhausted; no complete result");
                    id = out.addState(); ids.put(target, id); queue.add(target);
                }
                out.addTransition(at, id, new Interval(alphabet.min(c), alphabet.max(c)), common);
            }
        }
        return WeightedOperations.simplify(out.build());
    }
    private record Segment(int min, int max, int target, double weight) { }
    private record Signature(boolean accept, double terminal, List<Segment> edges) { }
    /**
     * Pushes exact integer suffix potentials then identifies equal normalized
     * right-cost functions bottom-up. On deterministic DAGs this gives a minimal
     * weighted deterministic automaton; it never applies boolean equivalence.
     * Sorting dominates O(I log I), with O(V+E+I) structural storage.
     * @param input deterministic integer-weighted DAG
     * @return minimal weighted DAG
     */
    public static WeightedAutomaton minimizeDag(WeightedAutomaton input) {
        if (!input.graph().isDeterministic()) throw new IllegalArgumentException("Deterministic weighting required");
        certifiedIntegerDag(input);
        WeightedAutomaton a = input.trim(); AutomatonGraph g = a.graph(); if (g.initial < 0) return a;
        double[] potential = WeightedPaths.backwardDistances(a); int[] group = new int[g.getNbStates()];
        WeightedAutomaton.Builder out = WeightedAutomaton.builder(); Map<Signature, Integer> groups = new HashMap<>();
        for (int k = g.order.length - 1; k >= 0; k--) {
            int s = g.order[k]; List<Segment> segments = new ArrayList<>();
            for (int i = g.outStart[s]; i < g.outStart[s + 1]; i++) {
                int e = g.outEdges[i]; double w = WeightMath.add(WeightMath.add(a.weight(e), potential[g.target[e]]), -potential[s]);
                for (int j = g.labelStart[e]; j < g.labelStart[e + 1]; j++) segments.add(new Segment(g.mins[j], g.maxs[j], group[g.target[e]], w));
            }
            segments.sort(Comparator.comparingInt(Segment::min)); List<Segment> merged = new ArrayList<>();
            for (Segment edge : segments) {
                if (!merged.isEmpty()) {
                    Segment last = merged.get(merged.size() - 1);
                    if ((long) last.max + 1 == edge.min && last.target == edge.target && last.weight == edge.weight) {
                        merged.set(merged.size() - 1, new Segment(last.min, edge.max, edge.target, edge.weight)); continue;
                    }
                }
                merged.add(edge);
            }
            double terminal = g.accept[s] ? WeightMath.add(a.finalWeight(s), -potential[s]) : 0;
            Signature signature = new Signature(g.accept[s], terminal, List.copyOf(merged)); Integer id = groups.get(signature);
            if (id == null) {
                id = out.addState(); groups.put(signature, id); if (g.accept[s]) out.setAccept(id, terminal);
                for (Segment e : merged) out.addTransition(id, e.target, new Interval(e.min, e.max), e.weight);
            }
            group[s] = id;
        }
        out.setInitial(group[g.initial], WeightMath.add(a.initialWeight(), potential[g.initial]));
        return out.build();
    }
}
