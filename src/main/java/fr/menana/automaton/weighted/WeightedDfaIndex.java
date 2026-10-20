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

import java.util.Arrays;

/**
 * Immutable interval-to-logical-edge index, independent of scalar/vector weights.
 * Its primitive arrays are private; any number of weightings of the identical
 * immutable graph can reuse the index without copying it or retaining old costs.
 */
public final class WeightedDfaIndex {
    private final AutomatonGraph graph;
    private final int[] start, min, max, edge;
    private WeightedDfaIndex(AutomatonGraph graph, int[] start, int[] min, int[] max, int[] edge) {
        this.graph = graph; this.start = start; this.min = min; this.max = max; this.edge = edge;
    }
    /**
     * @param graph deterministic epsilon-free immutable graph
     * @return compiled read-only index, rejecting nondeterminism
     */
    public static WeightedDfaIndex of(AutomatonGraph graph) {
        AutomatonGraph g = graph; if (!g.isDeterministic()) throw new IllegalArgumentException("Deterministic epsilon-free graph required");
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
        start[g.getNbStates()] = at; return new WeightedDfaIndex(g, start, min, max, edge);
    }
    /** @return indexed immutable graph */
    public AutomatonGraph graph() { return graph; }
    /**
     * @param state current state or -1
     * @param symbol consumed symbol
     * @return stable logical edge ID, or -1 if no transition exists
     */
    public int transition(int state, int symbol) {
        if (state == -1) return -1;
        int lo = start[state], hi = start[state + 1] - 1;
        while (lo <= hi) { int mid = (lo + hi) >>> 1; if (max[mid] < symbol) lo = mid + 1; else if (min[mid] > symbol) hi = mid - 1; else return edge[mid]; }
        return -1;
    }
}
