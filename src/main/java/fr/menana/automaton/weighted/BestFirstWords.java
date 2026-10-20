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
import java.util.*;

/** On-demand weighted subset search: one prefix per concrete word, never per path. */
final class BestFirstWords implements Iterator<WeightedWord> {
    private final AutomatonGraph graph;
    private final ExactCosts costs;
    private final boolean[] useful;
    private final BigDecimal[] suffix;
    private final int[] steps;
    private final Alphabet alphabet;
    private final PriorityQueue<Entry> queue = new PriorityQueue<>();
    private long serial;
    private WeightedWord ready;

    private record StateCosts(BigDecimal[] values, BigDecimal bound, BigDecimal terminal, int remaining) { }
    private static final class Prefix {
        final Prefix parent;
        final int symbol, symbolClass, length;
        final StateCosts costs;
        Prefix(Prefix parent, int symbol, int symbolClass, StateCosts costs) {
            this.parent = parent; this.symbol = symbol; this.symbolClass = symbolClass; this.costs = costs;
            length = parent == null ? 0 : Math.addExact(parent.length, 1);
        }
    }
    private record Entry(Prefix prefix, BigDecimal priority, long estimatedLength, boolean terminal, long serial) implements Comparable<Entry> {
        public int compareTo(Entry other) {
            int c = priority.compareTo(other.priority);
            if (c == 0) c = Long.compare(estimatedLength, other.estimatedLength);
            if (c == 0) c = Integer.compare(other.prefix.length, prefix.length);
            if (c == 0) c = Boolean.compare(other.terminal, terminal);
            return c == 0 ? Long.compare(serial, other.serial) : c;
        }
    }
    BestFirstWords(WeightedAutomaton a) {
        graph = a.graph(); costs = new ExactCosts(a); useful = graph.usefulStates();
        suffix = costs.suffixes(useful); steps = costs.symbolSteps(suffix);
        List<IntervalSet> labels = new ArrayList<>();
        for (int e = 0; e < graph.source.length; e++) if (!graph.epsilon[e] && useful[graph.source[e]] && useful[graph.target[e]]) labels.add(graph.labels(e));
        alphabet = Alphabet.of(labels);
        if (graph.initial >= 0 && useful[graph.initial]) {
            BigDecimal[] initial = new BigDecimal[graph.accept.length]; initial[graph.initial] = costs.initial;
            costs.closeEpsilon(initial, useful); enqueue(new Prefix(null, 0, -1, stateCosts(initial)));
        }
    }
    private StateCosts stateCosts(BigDecimal[] values) {
        BigDecimal bound = null, terminal = null; int remaining = Integer.MAX_VALUE;
        for (int s = 0; s < values.length; s++) if (values[s] != null && suffix[s] != null) {
            BigDecimal value = values[s].add(suffix[s]);
            int c = bound == null ? -1 : value.compareTo(bound);
            if (c < 0) { bound = value; remaining = steps[s]; }
            else if (c == 0) remaining = Math.min(remaining, steps[s]);
            if (costs.finals[s] != null) {
                BigDecimal last = values[s].add(costs.finals[s]);
                if (ExactCosts.improves(last, terminal)) terminal = last;
            }
        }
        return bound == null ? null : new StateCosts(values, bound, terminal, remaining);
    }
    private long nextSerial() { long result = serial; serial = Math.incrementExact(serial); return result; }
    private void enqueue(Prefix prefix) {
        queue.add(new Entry(prefix, prefix.costs.bound, (long) prefix.length + prefix.costs.remaining, false, nextSerial()));
    }
    @Override public boolean hasNext() {
        if (ready != null) return true;
        while (!queue.isEmpty()) {
            Entry entry = queue.remove(); Prefix p = entry.prefix;
            if (entry.terminal) {
                int[] word = new int[p.length]; Prefix at = p;
                for (int i = word.length - 1; i >= 0; i--, at = at.parent) word[i] = at.symbol;
                ready = new WeightedWord(word, ExactCosts.rounded(entry.priority)); return true;
            }
            if (p.parent != null && p.symbol < alphabet.max(p.symbolClass))
                enqueue(new Prefix(p.parent, p.symbol + 1, p.symbolClass, p.costs));
            if (p.costs.terminal != null)
                queue.add(new Entry(p, p.costs.terminal, p.length, true, nextSerial()));
            for (int c = 0; c < alphabet.size(); c++) {
                BigDecimal[] next = new BigDecimal[graph.accept.length]; int symbol = alphabet.min(c);
                for (int e = 0; e < graph.source.length; e++) if (p.costs.values[graph.source[e]] != null && useful[graph.target[e]] && graph.accepts(e, symbol)) {
                    int t = graph.target[e]; BigDecimal value = p.costs.values[graph.source[e]].add(costs.edges[e]);
                    if (ExactCosts.improves(value, next[t])) next[t] = value;
                }
                costs.closeEpsilon(next, useful); StateCosts values = stateCosts(next);
                if (values != null) enqueue(new Prefix(p, symbol, c, values));
            }
        }
        return false;
    }
    @Override public WeightedWord next() {
        if (!hasNext()) throw new NoSuchElementException();
        WeightedWord result = ready; ready = null; return result;
    }
}
