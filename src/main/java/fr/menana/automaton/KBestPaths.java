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
 * K best logical-edge paths on DAGs. Equal-cost paths remain distinct; distinct words are not enumerated.
 */
public final class KBestPaths {
    private KBestPaths() { }
    private record Suffix(double cost, BigDecimal exact, int edge, Suffix next) { }
    /**
     * Keeps K suffixes per vertex, sorting at most K*outdegree+1 candidates.
     * Output/reconstruction can be exponential in requested K; no silent budget truncation.
     * A nonsingleton label contributes its smallest symbol as the concrete witness.
     * @param a DAG weighting
     * @param k requested number of paths
     * @return up to K paths, increasing cost
     */
    public static List<WeightedPath> accepting(WeightedAutomaton a, int k) {
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
     * @return best paths, rejecting epsilon cycles
     */
    public static List<WeightedPath> ofLength(WeightedAutomaton a, int length, int k) {
        LayeredAutomaton layers = LayeredAutomaton.exactly(a.graph(), length); List<WeightedPath> result = new ArrayList<>();
        for (WeightedPath p : accepting(layers.evaluate(a), k)) result.add(layers.map(p)); return List.copyOf(result);
    }
}
