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

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.PriorityQueue;

/** Exact binary-input sums for transformations and cost-ordered enumeration. */
final class ExactCosts {
    final AutomatonGraph graph;
    final BigDecimal initial;
    final BigDecimal[] edges, finals;

    ExactCosts(WeightedAutomaton a) {
        graph = a.graph(); initial = new BigDecimal(a.initialWeight());
        edges = new BigDecimal[graph.source.length];
        finals = new BigDecimal[graph.accept.length];
        for (int e = 0; e < edges.length; e++) edges[e] = new BigDecimal(a.weight(e));
        for (int s = 0; s < finals.length; s++) if (graph.accept[s]) finals[s] = new BigDecimal(a.finalWeight(s));
    }
    static boolean improves(BigDecimal value, BigDecimal previous) {
        return previous == null || value.compareTo(previous) < 0;
    }
    static BigDecimal canonical(BigDecimal value) {
        return value.signum() == 0 ? BigDecimal.ZERO : value.stripTrailingZeros();
    }
    static double rounded(BigDecimal value) {
        double result = value.doubleValue();
        if (!Double.isFinite(result)) throw new ArithmeticException("Cost overflow");
        return result == 0 ? 0 : result;
    }
    static double representable(BigDecimal value) {
        double result = rounded(value);
        if (new BigDecimal(result).compareTo(value) != 0)
            throw new ArithmeticException("Exact transformed weight is not representable as a double");
        return result;
    }
    private record Distance(int state, BigDecimal cost) { }

    // null denotes unreachable. Only useful states participate; irrelevant cycles
    // cannot cause failure. Every retained edge has w+h(target)-h(source) >= 0.
    BigDecimal[] suffixes(boolean[] useful) {
        AutomatonGraph g = graph; int n = g.accept.length;
        BigDecimal[] distances = new BigDecimal[n]; boolean nonnegative = true;
        for (int e = 0; e < edges.length; e++) if (useful[g.source[e]] && useful[g.target[e]] && edges[e].signum() < 0) nonnegative = false;
        for (int s = 0; s < n; s++) if (useful[s]) distances[s] = finals[s];
        if (g.order != null) {
            for (int i = n - 1; i >= 0; i--) {
                int s = g.order[i]; if (!useful[s]) continue;
                for (int j = g.outStart[s]; j < g.outStart[s + 1]; j++) {
                    int e = g.outEdges[j], t = g.target[e];
                    if (distances[t] != null) {
                        BigDecimal value = edges[e].add(distances[t]);
                        if (improves(value, distances[s])) distances[s] = value;
                    }
                }
            }
        } else if (nonnegative) {
            PriorityQueue<Distance> queue = new PriorityQueue<>(Comparator.comparing(Distance::cost));
            for (int s = 0; s < n; s++) if (distances[s] != null) queue.add(new Distance(s, distances[s]));
            while (!queue.isEmpty()) {
                Distance current = queue.remove(); int t = current.state;
                if (current.cost.compareTo(distances[t]) != 0) continue;
                for (int i = g.inStart[t]; i < g.inStart[t + 1]; i++) {
                    int e = g.inEdges[i], s = g.source[e]; if (!useful[s]) continue;
                    BigDecimal value = edges[e].add(current.cost);
                    if (improves(value, distances[s])) { distances[s] = value; queue.add(new Distance(s, value)); }
                }
            }
        } else {
            for (int pass = 0; pass < n; pass++) {
                boolean changed = false;
                for (int e = 0; e < edges.length; e++) {
                    int s = g.source[e], t = g.target[e];
                    if (!useful[s] || distances[t] == null) continue;
                    BigDecimal value = edges[e].add(distances[t]);
                    if (improves(value, distances[s])) { distances[s] = value; changed = true; }
                }
                if (!changed) break;
                if (pass == n - 1) throw new IllegalArgumentException("Cost-ordered enumeration requires no useful negative cycle");
            }
        }
        return distances;
    }

    // Minimum consumed length among exact minimum-cost suffixes. Zero-one BFS
    // keeps epsilon edges at the same length without forming a cyclic witness.
    int[] symbolSteps(BigDecimal[] suffix) {
        AutomatonGraph g = graph; int[] steps = new int[g.accept.length];
        java.util.Arrays.fill(steps, Integer.MAX_VALUE);
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
        for (int s = 0; s < steps.length; s++) if (suffix[s] != null && finals[s] != null && suffix[s].compareTo(finals[s]) == 0) {
            steps[s] = 0; queue.add(s);
        }
        while (!queue.isEmpty()) {
            int t = queue.removeFirst();
            for (int i = g.inStart[t]; i < g.inStart[t + 1]; i++) {
                int e = g.inEdges[i], s = g.source[e]; int value = steps[t] + (g.epsilon[e] ? 0 : 1);
                if (value < steps[s] && suffix[s] != null && edges[e].add(suffix[t]).compareTo(suffix[s]) == 0) {
                    steps[s] = value;
                    if (g.epsilon[e]) queue.addFirst(s); else queue.addLast(s);
                }
            }
        }
        return steps;
    }

    // Multi-source shortest epsilon closure. Throws only if a negative epsilon
    // cycle is reachable from a finite seed. Callers trim before using it.
    void closeEpsilon(BigDecimal[] distances) { closeEpsilon(distances, null); }

    void closeEpsilon(BigDecimal[] distances, boolean[] allowed) {
        AutomatonGraph g = graph; int n = distances.length;
        for (int pass = 0; pass < n; pass++) {
            boolean changed = false;
            for (int e = 0; e < edges.length; e++) if (g.epsilon[e] && distances[g.source[e]] != null && (allowed == null || allowed[g.target[e]])) {
                BigDecimal value = distances[g.source[e]].add(edges[e]); int t = g.target[e];
                if (improves(value, distances[t])) { distances[t] = value; changed = true; }
            }
            if (!changed) return;
            if (pass == n - 1) throw new IllegalArgumentException("Negative epsilon cycle cannot have finite transformed weights");
        }
    }
}
