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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Distinct concrete words ranked by their minimum accepting cost. */
public final class KBestWords {
    private KBestWords() { }
    /**
     * Enumerates word prefixes on demand using exact per-state costs and epsilon
     * closure. It does not construct a complete DFA, so no finite determinization
     * assumption or determinization budget is required. Useful negative cycles
     * are rejected before enumeration. NFA ambiguity and zero-cost epsilon cycles
     * cannot duplicate words. Interval classes use lazy symbol cursors, including
     * the full int range. Equal-cost words are ordered by consumed length, without
     * a lexicographic promise. An infinite tie can prevent access to higher costs.
     * Deterministic inputs use a direct path/cursor fast path without per-state vectors.
     * Preprocessing uses exact suffix potentials and symbolic alphabet partitioning.
     * Search/frontier size can be exponential; each expanded prefix needs O(V)
     * state costs and O(C*(E+VEepsilon)) worst-case transition/closure work for C
     * interval classes. Exact arithmetic costs and output sizes are additional.
     * @param a scalar weighting
     * @return independent single-thread lazy iterator
     */
    public static Iterator<WeightedWord> iterator(WeightedAutomaton a) {
        if (!a.graph().isDeterministic()) return new BestFirstWords(a);
        Iterator<WeightedPath> paths = new BestFirstPaths(a, true);
        return new Iterator<>() {
            public boolean hasNext() { return paths.hasNext(); }
            public WeightedWord next() { WeightedPath path = paths.next(); return new WeightedWord(path.word(), path.cost()); }
        };
    }
    /**
     * @param a scalar weighting
     * @param k maximum number of distinct words
     * @return up to K distinct words, increasing exact minimum cost
     */
    public static List<WeightedWord> accepting(WeightedAutomaton a, int k) {
        if (k < 0) throw new IllegalArgumentException("Negative K");
        if (k == 0) return List.of();
        Iterator<WeightedWord> words = iterator(a); List<WeightedWord> result = new ArrayList<>();
        while (result.size() < k && words.hasNext()) result.add(words.next());
        return List.copyOf(result);
    }
    /**
     * @param a scalar weighting
     * @param length exact number of consumed symbols
     * @param k maximum number of distinct words
     * @return best distinct words at this horizon; epsilon cycles use weighted closure
     */
    public static List<WeightedWord> ofLength(WeightedAutomaton a, int length, int k) {
        if (k < 0) throw new IllegalArgumentException("Negative K");
        LayeredAutomaton layers = LayeredAutomaton.exactly(a.graph(), length);
        return accepting(layers.evaluate(a), k);
    }
}
