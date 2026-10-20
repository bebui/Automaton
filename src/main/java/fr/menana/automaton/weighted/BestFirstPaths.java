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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

/** Lazy A* enumeration with exact suffix potentials and persistent prefixes. */
final class BestFirstPaths implements Iterator<WeightedPath> {
    private final AutomatonGraph graph;
    private final ExactCosts costs;
    private final BigDecimal[] suffix;
    private final int[] suffixSteps;
    private final boolean symbols;
    private final PriorityQueue<Entry> queue = new PriorityQueue<>();
    private long serial;
    private WeightedPath ready;

    private static final class Prefix {
        final Prefix parent;
        final int edge, state, symbol, interval, depth;
        final BigDecimal cost;
        Prefix(Prefix parent, int edge, int state, int symbol, int interval, BigDecimal cost) {
            this.parent = parent; this.edge = edge; this.state = state; this.symbol = symbol;
            this.interval = interval; this.cost = cost;
            depth = parent == null ? 0 : Math.addExact(parent.depth, 1);
        }
    }
    private record Entry(Prefix prefix, BigDecimal priority, long estimatedDepth, boolean terminal, long serial) implements Comparable<Entry> {
        public int compareTo(Entry other) {
            int c = priority.compareTo(other.priority);
            if (c == 0) c = Long.compare(estimatedDepth, other.estimatedDepth);
            if (c == 0) c = Integer.compare(other.prefix.depth, prefix.depth);
            if (c == 0) c = Boolean.compare(other.terminal, terminal);
            return c == 0 ? Long.compare(serial, other.serial) : c;
        }
    }

    BestFirstPaths(WeightedAutomaton a, boolean symbols) {
        graph = a.graph(); costs = new ExactCosts(a); this.symbols = symbols;
        suffix = costs.suffixes(graph.usefulStates());
        suffixSteps = optimalSuffixSteps();
        if (graph.initial >= 0 && suffix[graph.initial] != null)
            enqueue(new Prefix(null, -1, graph.initial, 0, -1, costs.initial));
    }
    // Among minimum-cost suffixes use the fewest edges. This avoids both
    // starvation on zero cycles and breadth-first explosion before a witness.
    private int[] optimalSuffixSteps() {
        int n = graph.accept.length; int[] steps = new int[n], work = new int[n];
        java.util.Arrays.fill(steps, -1); int head = 0, tail = 0;
        for (int s = 0; s < n; s++) if (suffix[s] != null && costs.finals[s] != null && suffix[s].compareTo(costs.finals[s]) == 0) {
            steps[s] = 0; work[tail++] = s;
        }
        while (head < tail) {
            int t = work[head++];
            for (int i = graph.inStart[t]; i < graph.inStart[t + 1]; i++) {
                int e = graph.inEdges[i], s = graph.source[e];
                if (steps[s] < 0 && suffix[s] != null && costs.edges[e].add(suffix[t]).compareTo(suffix[s]) == 0) {
                    steps[s] = steps[t] + 1; work[tail++] = s;
                }
            }
        }
        return steps;
    }
    private long nextSerial() { long result = serial; serial = Math.incrementExact(serial); return result; }
    private void enqueue(Prefix prefix) {
        queue.add(new Entry(prefix, prefix.cost.add(suffix[prefix.state]), (long) prefix.depth + suffixSteps[prefix.state], false, nextSerial()));
    }
    private void sibling(Prefix p) {
        if (!symbols || p.parent == null || graph.epsilon[p.edge]) return;
        int interval = p.interval; int symbol;
        if (p.symbol < graph.maxs[interval]) symbol = p.symbol + 1;
        else {
            interval++;
            if (interval == graph.labelStart[p.edge + 1]) return;
            symbol = graph.mins[interval];
        }
        enqueue(new Prefix(p.parent, p.edge, p.state, symbol, interval, p.cost));
    }
    @Override public boolean hasNext() {
        if (ready != null) return true;
        while (!queue.isEmpty()) {
            Entry entry = queue.remove(); Prefix p = entry.prefix;
            if (entry.terminal) { ready = witness(p, entry.priority); return true; }
            sibling(p);
            if (graph.accept[p.state])
                queue.add(new Entry(p, p.cost.add(costs.finals[p.state]), p.depth, true, nextSerial()));
            for (int i = graph.outStart[p.state]; i < graph.outStart[p.state + 1]; i++) {
                int e = graph.outEdges[i], t = graph.target[e]; if (suffix[t] == null) continue;
                int interval = graph.labelStart[e];
                int symbol = graph.epsilon[e] ? 0 : graph.mins[interval];
                enqueue(new Prefix(p, e, t, symbol, interval, p.cost.add(costs.edges[e])));
            }
        }
        return false;
    }
    private WeightedPath witness(Prefix p, BigDecimal total) {
        int length = 0;
        for (Prefix at = p; at.parent != null; at = at.parent) if (!graph.epsilon[at.edge]) length++;
        int[] edges = new int[p.depth], word = new int[length]; int i = edges.length, j = length;
        for (Prefix at = p; at.parent != null; at = at.parent) {
            edges[--i] = at.edge; if (!graph.epsilon[at.edge]) word[--j] = at.symbol;
        }
        return new WeightedPath(graph, graph.initial, edges, word, ExactCosts.rounded(total));
    }
    @Override public WeightedPath next() {
        if (!hasNext()) throw new NoSuchElementException();
        WeightedPath result = ready; ready = null; return result;
    }
}
