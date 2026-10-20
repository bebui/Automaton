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
 * Cost-function-preserving constructions, leaving both operands unchanged. No boolean determinization/minimization is used.
 */
public final class WeightedOperations {
    private WeightedOperations() { }
    static int[] copy(WeightedAutomaton a, WeightedAutomaton.Builder b, boolean finals) {
        AutomatonGraph g = a.graph(); int[] map = new int[g.getNbStates()];
        for (int s = 0; s < map.length; s++) {
            map[s] = b.addState(); if (finals && g.accept[s]) b.setAccept(map[s], a.finalWeight(s));
        }
        for (int e = 0; e < g.source.length; e++) {
            if (g.epsilon[e]) b.addEpsilonTransition(map[g.source[e]], map[g.target[e]], a.weight(e));
            else b.addTransition(map[g.source[e]], map[g.target[e]], g.labels(e), a.weight(e));
        }
        return map;
    }
    /**
     * @param a first
     * @param b second
     * @return union, taking the minimum for ambiguous words
     */
    public static WeightedAutomaton union(WeightedAutomaton a, WeightedAutomaton b) {
        WeightedAutomaton.Builder out = WeightedAutomaton.builder(); int root = out.addState(); out.setInitial(root);
        int[] left = copy(a, out, true), right = copy(b, out, true);
        if (a.graph().initial >= 0) out.addEpsilonTransition(root, left[a.graph().initial], a.initialWeight());
        if (b.graph().initial >= 0) out.addEpsilonTransition(root, right[b.graph().initial], b.initialWeight());
        return out.build();
    }
    /**
     * @param a first
     * @param b second
     * @return concatenation over every possible split, with summed boundaries
     */
    public static WeightedAutomaton concatenate(WeightedAutomaton a, WeightedAutomaton b) {
        WeightedAutomaton.Builder out = WeightedAutomaton.builder();
        int[] left = copy(a, out, false), right = copy(b, out, true);
        if (a.graph().initial >= 0 && b.graph().initial >= 0) {
            out.setInitial(left[a.graph().initial], a.initialWeight());
            for (int s = 0; s < left.length; s++) if (a.graph().accept[s])
                out.addEpsilonTransition(left[s], right[b.graph().initial], WeightMath.add(a.finalWeight(s), b.initialWeight()));
        }
        return out.build();
    }
    private record Pair(int left, int right, int phase) { }
    /**
     * Synchronized product with summed vector-free scalar costs. Epsilon moves
     * use a canonical left-before-right filter between consuming transitions,
     * so an operand-path pair has one interleaving rather than artificial duplicates.
     * Interval intersections never enumerate the alphabet.
     * @param a left
     * @param b right
     * @return synchronized weighting
     */
    public static WeightedAutomaton intersection(WeightedAutomaton a, WeightedAutomaton b) {
        AutomatonGraph ga = a.graph(), gb = b.graph(); WeightedAutomaton.Builder out = WeightedAutomaton.builder();
        if (ga.initial < 0 || gb.initial < 0) return out.build();
        Map<Pair, Integer> ids = new HashMap<>(); List<Pair> queue = new ArrayList<>();
        Pair root = new Pair(ga.initial, gb.initial, 0); ids.put(root, out.addState()); queue.add(root);
        out.setInitial(0, WeightMath.add(a.initialWeight(), b.initialWeight()));
        for (int at = 0; at < queue.size(); at++) {
            Pair p = queue.get(at); int from = ids.get(p);
            if (ga.accept[p.left] && gb.accept[p.right]) out.setAccept(from, WeightMath.add(a.finalWeight(p.left), b.finalWeight(p.right)));
            for (int i = ga.outStart[p.left]; i < ga.outStart[p.left + 1]; i++) {
                int e = ga.outEdges[i];
                if (ga.epsilon[e]) {
                    if (p.phase != 2) out.addEpsilonTransition(from, pairState(new Pair(ga.target[e], p.right, 1), ids, queue, out), a.weight(e));
                    continue;
                }
                for (int j = gb.outStart[p.right]; j < gb.outStart[p.right + 1]; j++) {
                    int f = gb.outEdges[j]; if (gb.epsilon[f]) continue;
                    IntervalSet common = ga.labels(e).intersection(gb.labels(f));
                    if (!common.isEmpty()) out.addTransition(from, pairState(new Pair(ga.target[e], gb.target[f], 0), ids, queue, out), common, WeightMath.add(a.weight(e), b.weight(f)));
                }
            }
            for (int j = gb.outStart[p.right]; j < gb.outStart[p.right + 1]; j++) {
                int f = gb.outEdges[j]; if (gb.epsilon[f])
                    out.addEpsilonTransition(from, pairState(new Pair(p.left, gb.target[f], 2), ids, queue, out), b.weight(f));
            }
        }
        return out.build();
    }
    private static int pairState(Pair pair, Map<Pair, Integer> ids, List<Pair> queue, WeightedAutomaton.Builder out) {
        Integer found = ids.get(pair); if (found != null) return found;
        int id = out.addState(); ids.put(pair, id); queue.add(pair); return id;
    }
    /**
     * @param a operand
     * @return reversed word-cost function (floating summation order can change rounding)
     */
    public static WeightedAutomaton revert(WeightedAutomaton a) {
        AutomatonGraph g = a.graph(); WeightedAutomaton.Builder out = WeightedAutomaton.builder();
        if (g.initial < 0) return out.build();
        for (int s = 0; s < g.getNbStates(); s++) out.addState();
        int root = out.addState(); out.setInitial(root); out.setAccept(g.initial, a.initialWeight());
        for (int s = 0; s < g.getNbStates(); s++) if (g.accept[s]) out.addEpsilonTransition(root, s, a.finalWeight(s));
        for (int e = 0; e < g.source.length; e++) {
            if (g.epsilon[e]) out.addEpsilonTransition(g.target[e], g.source[e], a.weight(e));
            else out.addTransition(g.target[e], g.source[e], g.labels(e), a.weight(e));
        }
        return out.build();
    }
    /**
     * @param a operand
     * @return Kleene closure; negative empty-word repetitions can make costs unbounded
     */
    public static WeightedAutomaton star(WeightedAutomaton a) {
        WeightedAutomaton.Builder out = WeightedAutomaton.builder(); int root = out.addState(); out.setInitial(root); out.setAccept(root);
        int[] map = copy(a, out, false);
        if (a.graph().initial >= 0) {
            out.addEpsilonTransition(root, map[a.graph().initial], a.initialWeight());
            for (int s = 0; s < map.length; s++) if (a.graph().accept[s]) out.addEpsilonTransition(map[s], root, a.finalWeight(s));
        }
        return out.build();
    }
    /**
     * @param a repeated operand
     * @param min minimum repetitions
     * @param max maximum, -1 for unbounded
     * @return repetition, preserving every split and its boundary costs
     */
    public static WeightedAutomaton repeat(WeightedAutomaton a, int min, int max) {
        if (min < 0 || (max != -1 && max < min)) throw new IllegalArgumentException("Invalid repetition bounds");
        if (max == -1) return concatenate(repeat(a, min, min), star(a));
        AutomatonGraph g = a.graph(); Math.multiplyExact(Math.addExact(max, 1), g.getNbStates());
        WeightedAutomaton.Builder out = WeightedAutomaton.builder(); int root = out.addState(); out.setInitial(root);
        if (min == 0) out.setAccept(root);
        int[] previous = null;
        for (int k = 1; k <= max; k++) {
            int[] current = copy(a, out, false);
            if (g.initial >= 0) {
                if (k == 1) out.addEpsilonTransition(root, current[g.initial], a.initialWeight());
                else for (int s = 0; s < previous.length; s++) if (g.accept[s])
                    out.addEpsilonTransition(previous[s], current[g.initial], WeightMath.add(a.finalWeight(s), a.initialWeight()));
            }
            if (k >= min) for (int s = 0; s < current.length; s++) if (g.accept[s]) out.setAccept(current[s], a.finalWeight(s));
            previous = current;
        }
        return out.build();
    }
    /**
     * @param a operand
     * @return graph with exactly the useful states, preserving all accepting paths
     */
    public static WeightedAutomaton trim(WeightedAutomaton a) {
        AutomatonGraph g = a.graph(); boolean[] useful = g.usefulStates();
        WeightedAutomaton.Builder out = WeightedAutomaton.builder(); int[] map = new int[useful.length]; Arrays.fill(map, -1);
        for (int s = 0; s < map.length; s++) if (useful[s]) { map[s] = out.addState(); if (g.accept[s]) out.setAccept(map[s], a.finalWeight(s)); }
        if (g.initial >= 0 && useful[g.initial]) out.setInitial(map[g.initial], a.initialWeight());
        for (int e = 0; e < g.source.length; e++) if (useful[g.source[e]] && useful[g.target[e]]) {
            if (g.epsilon[e]) out.addEpsilonTransition(map[g.source[e]], map[g.target[e]], a.weight(e));
            else out.addTransition(map[g.source[e]], map[g.target[e]], g.labels(e), a.weight(e));
        }
        return out.build();
    }
    private record Merge(int from, int to, boolean epsilon, long weight) { }
    /**
     * Trims and merges labels with identical endpoints/mode/exact weight.
     * Preserves word costs, not path multiplicity or old edge IDs.
     * @param a operand
     * @return simplified cost function
     */
    public static WeightedAutomaton simplify(WeightedAutomaton a) {
        a = trim(a); AutomatonGraph g = a.graph();
        Map<Merge, IntervalSet> grouped = new LinkedHashMap<>();
        for (int e = 0; e < g.source.length; e++) {
            Merge key = new Merge(g.source[e], g.target[e], g.epsilon[e], Double.doubleToLongBits(a.weight(e)));
            IntervalSet set = grouped.computeIfAbsent(key, k -> new IntervalSet()); if (!g.epsilon[e]) set.add(g.labels(e));
        }
        WeightedAutomaton.Builder out = WeightedAutomaton.builder();
        for (int s = 0; s < g.getNbStates(); s++) { out.addState(); if (g.accept[s]) out.setAccept(s, a.finalWeight(s)); }
        if (g.initial >= 0) out.setInitial(g.initial, a.initialWeight());
        for (Map.Entry<Merge, IntervalSet> entry : grouped.entrySet()) {
            Merge k = entry.getKey(); double w = Double.longBitsToDouble(k.weight);
            if (k.epsilon) out.addEpsilonTransition(k.from, k.to, w); else out.addTransition(k.from, k.to, entry.getValue(), w);
        }
        return out.build();
    }
}
