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
import java.math.BigDecimal;

/**
 * K best logical-edge accepting walks. Repeated states and equal-cost paths remain distinct.
 * Interval labels provide a representative symbol; distinct words have a separate API.
 */
public final class KBestPaths {
    private KBestPaths() { }
    private record Suffix(double cost, BigDecimal exact, int edge, Suffix next) { }
    /**
     * On DAGs keeps K suffixes per vertex, sorting at most K*outdegree+1 candidates.
     * On cyclic graphs collects the lazy iterator, rejecting useful negative cycles.
     * The number of paths can be exponential in graph size; storage depends on K.
     * No silent budget truncation is used.
     * A nonsingleton label contributes its smallest symbol as the concrete witness.
     * @param a scalar weighting
     * @param k requested number of paths
     * @return up to K paths, increasing cost
     */
    public static List<WeightedPath> accepting(WeightedAutomaton a, int k) {
        if (k < 0) throw new IllegalArgumentException("Negative K");
        if (k == 0) return List.of();
        if (a.graph().isAcyclic()) return acceptingDag(a, k);
        Iterator<WeightedPath> paths = iterator(a); List<WeightedPath> result = new ArrayList<>();
        while (result.size() < k && paths.hasNext()) result.add(paths.next());
        return List.copyOf(result);
    }
    /**
     * Lazily enumerates accepting walks, including repeated vertices and epsilon
     * edges. Exact suffix potentials handle signed weights; useful negative cycles
     * are rejected before enumeration. Zero-cost cycles yield infinitely many
     * equal-cost walks, returned fairly in increasing edge count. There is no
     * promise to reach a higher cost after infinitely many lower-cost results.
     * Preprocessing: DAG O(V+E), nonnegative edges O((V+E) log(V+E)), otherwise O(VE).
     * Queue work is output/search dependent, O(log Q) per queued prefix, plus exact
     * arithmetic. Persistent prefixes share storage; no paths are eagerly listed.
     * The iterator is confined to one thread and can be abandoned at any time.
     * @param a scalar weighting
     * @return independent lazy iterator in increasing exact cost
     */
    public static Iterator<WeightedPath> iterator(WeightedAutomaton a) { return new BestFirstPaths(a, false); }

    private static List<WeightedPath> acceptingDag(WeightedAutomaton a, int k) {
        if (k < 0) throw new IllegalArgumentException("Negative K");
        AutomatonGraph g = a.graph(); g.requireDag(); if (k == 0 || g.initial < 0) return List.of();
        WeightMath.Exactness certificate = new WeightMath.Exactness(); certificate.include(a.initialWeight());
        for (int s = 0; s < g.getNbStates(); s++) if (g.accept[s]) certificate.include(a.finalWeight(s));
        for (int e = 0; e < g.source.length; e++) certificate.include(a.weight(e));
        boolean exact = !certificate.safe(g.getNbStates() + 1.0);
        List<List<Suffix>> best = new ArrayList<>(); for (int s = 0; s < g.getNbStates(); s++) best.add(List.of());
        for (int p = g.order.length - 1; p >= 0; p--) {
            int s = g.order[p]; List<Suffix> candidates = new ArrayList<>();
            if (g.accept[s]) candidates.add(new Suffix(a.finalWeight(s), exact ? new BigDecimal(a.finalWeight(s)) : null, -1, null));
            for (int i = g.outStart[s]; i < g.outStart[s + 1]; i++) {
                int e = g.outEdges[i]; for (Suffix suffix : best.get(g.target[e])) {
                    BigDecimal value = exact ? new BigDecimal(a.weight(e)).add(suffix.exact) : null;
                    candidates.add(new Suffix(exact ? rounded(value) : WeightMath.add(a.weight(e), suffix.cost), value, e, suffix));
                }
            }
            candidates.sort(exact ? (x, y) -> x.exact.compareTo(y.exact) : Comparator.comparingDouble(Suffix::cost));
            best.set(s, new ArrayList<>(candidates.subList(0, Math.min(k, candidates.size()))));
        }
        List<WeightedPath> result = new ArrayList<>();
        for (Suffix suffix : best.get(g.initial)) {
            int size = 0, length = 0;
            for (Suffix p = suffix; p.edge >= 0; p = p.next) { size++; if (!g.epsilon[p.edge]) length++; }
            int[] edges = new int[size], word = new int[length]; int i = 0, at = 0;
            double cost = a.initialWeight(); int s = g.initial;
            for (Suffix p = suffix; p.edge >= 0; p = p.next) {
                edges[i++] = p.edge; if (!g.epsilon[p.edge]) word[at++] = g.representative(p.edge);
                if (!exact) cost = WeightMath.add(cost, a.weight(p.edge)); s = g.target[p.edge];
            }
            cost = exact ? rounded(suffix.exact.add(new BigDecimal(a.initialWeight()))) : WeightMath.add(cost, a.finalWeight(s));
            result.add(new WeightedPath(g, g.initial, edges, word, cost));
        }
        return List.copyOf(result);
    }
    private static double rounded(BigDecimal value) { double d = value.doubleValue(); if (!Double.isFinite(d)) throw new ArithmeticException("Cost overflow"); return d; }
    /**
     * @param a weighting
     * @param length exact consumed length
     * @param k number of logical paths
     * @return best walks; rejects useful negative epsilon cycles at this horizon
     */
    public static List<WeightedPath> ofLength(WeightedAutomaton a, int length, int k) {
        if (k < 0) throw new IllegalArgumentException("Negative K");
        LayeredAutomaton layers = LayeredAutomaton.exactly(a.graph(), length); List<WeightedPath> result = new ArrayList<>();
        for (WeightedPath p : accepting(layers.evaluate(a), k)) result.add(layers.map(p)); return List.copyOf(result);
    }
}
