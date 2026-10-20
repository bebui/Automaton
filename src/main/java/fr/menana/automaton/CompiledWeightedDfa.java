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
 * Immutable weighted DFA index. No implicit determinization; recognition allocates nothing for an existing input array.
 */
public final class CompiledWeightedDfa {
    private final WeightedAutomaton automaton;
    private final int[] start, min, max, edge;
    private CompiledWeightedDfa(WeightedAutomaton a, int[] start, int[] min, int[] max, int[] edge) {
        automaton = a; this.start = start; this.min = min; this.max = max; this.edge = edge;
    }
    /**
     * @param a deterministic epsilon-free weighting
     * @return compiled interval index sharing its immutable graph
     */
    public static CompiledWeightedDfa of(WeightedAutomaton a) {
        AutomatonGraph g = a.graph(); if (!g.isDeterministic()) throw new IllegalArgumentException("Deterministic epsilon-free graph required");
        int[] start = new int[g.getNbStates() + 1], min = new int[g.mins.length], max = new int[min.length], edge = new int[min.length];
        int at = 0;
        for (int s = 0; s < g.getNbStates(); s++) {
            start[s] = at; int count = 0;
            for (int i = g.outStart[s]; i < g.outStart[s + 1]; i++) { int e = g.outEdges[i]; count += g.labelStart[e + 1] - g.labelStart[e]; }
            long[] keys = new long[count]; int[] owner = new int[count], interval = new int[count]; int k = 0;
            for (int i = g.outStart[s]; i < g.outStart[s + 1]; i++) {
                int e = g.outEdges[i];
                for (int j = g.labelStart[e]; j < g.labelStart[e + 1]; j++) { keys[k] = ((long) g.mins[j] << 32) | (k & 0xffffffffL); owner[k] = e; interval[k++] = j; }
            }
            Arrays.sort(keys);
            for (long key : keys) {
                int e = owner[(int) key], low = (int) (key >> 32), high = g.maxs[interval[(int) key]];
                min[at] = low; max[at] = high; edge[at++] = e;
            }
        }
        start[g.getNbStates()] = at; return new CompiledWeightedDfa(a, start, min, max, edge);
    }
    /**
     * @param a new weighting of the identical graph
     * @return view reusing the exact compiled interval index
     */
    public CompiledWeightedDfa reweight(WeightedAutomaton a) {
        if (a.graph() != automaton.graph()) throw new IllegalArgumentException("Same graph required");
        return new CompiledWeightedDfa(a, start, min, max, edge);
    }
    /**
     * @param state current vertex, or -1
     * @param symbol next symbol
     * @return logical edge ID, or -1
     */
    public int transition(int state, int symbol) {
        if (state == -1) return -1;
        int lo = start[state], hi = start[state + 1] - 1;
        while (lo <= hi) { int mid = (lo + hi) >>> 1; if (max[mid] < symbol) lo = mid + 1; else if (min[mid] > symbol) hi = mid - 1; else return edge[mid]; }
        return -1;
    }
    /**
     * @param word consumed symbols
     * @return minimum cost of the unique path, or +infinity
     */
    public double cost(int... word) { return cost(word, 0, word.length); }
    /**
     * @param text input array
     * @param from inclusive start
     * @param to exclusive end
     * @return cost of the slice
     */
    public double cost(int[] text, int from, int to) {
        Objects.checkFromToIndex(from, to, text.length);
        int s = automaton.graph().initial; if (s < 0) return Double.POSITIVE_INFINITY;
        double result = automaton.initialWeight();
        for (int i = from; i < to; i++) { int e = transition(s, text[i]); if (e < 0) return Double.POSITIVE_INFINITY; result = WeightMath.add(result, automaton.weight(e)); s = automaton.graph().target[e]; }
        return automaton.graph().accept[s] ? WeightMath.add(result, automaton.finalWeight(s)) : Double.POSITIVE_INFINITY;
    }
}
