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

import java.util.Arrays;

/**
 * Immutable shortest-distance result. Reconstruction is optional and never returned for an unbounded optimum.
 */
public final class ShortestPathResult {
    /**
     * Mathematical reachability/boundedness, distinct from rounded finite cost.
     */
    public enum Status {
        /**
         * A finite optimum exists.
         */ FINITE,
        /**
         * No suitable path exists.
         */ UNREACHABLE,
        /**
         * Infimum is negative infinity; there is no best finite path.
         */ UNBOUNDED
    }
    final WeightedAutomaton automaton;
    final double[] distance;
    final int[] parent;
    final int start;
    final double seed;
    final boolean longest;
    ShortestPathResult(WeightedAutomaton a, double[] distances, int[] parent, int start, double seed, boolean longest) {
        automaton = a; distance = distances; this.parent = parent; this.start = start; this.seed = seed; this.longest = longest;
    }
    /**
     * @param state destination
     * @return distance, +infinity if unreachable, -infinity if unbounded
     */
    public double distanceTo(int state) {
        automaton.graph().checkState(state);
        return longest && distance[state] == Double.NEGATIVE_INFINITY ? Double.POSITIVE_INFINITY : distance[state];
    }
    /**
     * @return defensive distance copy, using +infinity for unreachable states
     */
    public double[] distances() {
        double[] result = distance.clone();
        if (longest) for (int i = 0; i < result.length; i++) if (result[i] == Double.NEGATIVE_INFINITY) result[i] = Double.POSITIVE_INFINITY;
        return result;
    }
    /**
     * @param state destination
     * @return status
     */
    public Status statusTo(int state) {
        double d = distanceTo(state);
        return d == Double.POSITIVE_INFINITY ? Status.UNREACHABLE : d == Double.NEGATIVE_INFINITY ? Status.UNBOUNDED : Status.FINITE;
    }
    /**
     * Reconstructs only when predecessors were requested. Edge labels use their least symbol.
     * @param state destination
     * @return witness, or null if unreachable
     * @throws IllegalStateException if unbounded or predecessors were not retained
     */
    public WeightedPath pathTo(int state) {
        if (statusTo(state) == Status.UNREACHABLE) return null;
        if (statusTo(state) == Status.UNBOUNDED) throw new IllegalStateException("Unbounded optimum has no best path");
        if (parent == null) throw new IllegalStateException("Predecessors were not requested");
        AutomatonGraph g = automaton.graph();
        int[] reversed = new int[g.getNbStates()]; int size = 0, s = state;
        while (s != start) {
            if (size == reversed.length || parent[s] < 0) throw new IllegalStateException("Invalid predecessor chain");
            int e = parent[s]; reversed[size++] = e; s = g.source[e];
        }
        int[] edges = new int[size]; int length = 0;
        for (int i = 0; i < size; i++) { edges[i] = reversed[size - i - 1]; if (!g.epsilon[edges[i]]) length++; }
        int[] word = new int[length]; int at = 0;
        for (int e : edges) if (!g.epsilon[e]) word[at++] = g.representative(e);
        // Keep the solver's rounding contract, including exact Bellman-Ford fallback.
        return new WeightedPath(g, start, edges, word, distance[state]);
    }
    /**
     * @return best accepting cost, +infinity if none, -infinity if unbounded
     */
    public double optimalCost() {
        int best = bestFinal();
        return best < 0 ? Double.POSITIVE_INFINITY : distance[best] == Double.NEGATIVE_INFINITY && !longest
                ? Double.NEGATIVE_INFINITY : WeightMath.add(distance[best], automaton.finalWeight(best));
    }
    /**
     * @return best accepting witness, or null if none
     * @throws IllegalStateException if unbounded
     */
    public WeightedPath bestPath() {
        int best = bestFinal(); if (best < 0) return null;
        WeightedPath path = pathTo(best);
        return new WeightedPath(automaton.graph(), start, path.edges(), path.word(), WeightMath.add(path.cost(), automaton.finalWeight(best)));
    }
    private int bestFinal() {
        int best = -1; double value = longest ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (int s = 0; s < distance.length; s++) if (automaton.graph().accept[s] && statusTo(s) != Status.UNREACHABLE) {
            double candidate = distance[s] == Double.NEGATIVE_INFINITY ? Double.NEGATIVE_INFINITY : WeightMath.add(distance[s], automaton.finalWeight(s));
            if (best < 0 || (longest ? candidate > value : candidate < value)) { best = s; value = candidate; }
        }
        return best;
    }
}
