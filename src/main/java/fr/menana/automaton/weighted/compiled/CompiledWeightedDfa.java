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
package fr.menana.automaton.weighted.compiled;

import fr.menana.automaton.weighted.AutomatonGraph;
import fr.menana.automaton.weighted.WeightedAutomaton;
import fr.menana.automaton.weighted.WeightedDfaIndex;
import fr.menana.automaton.weighted.internal.WeightMath;
import java.util.Objects;

/** Immutable scalar DFA evaluator backed by a reusable, weight-independent index. */
public final class CompiledWeightedDfa {
    private final WeightedAutomaton automaton;
    private final WeightedDfaIndex index;
    private CompiledWeightedDfa(WeightedAutomaton automaton, WeightedDfaIndex index) {
        this.automaton = automaton; this.index = index;
    }
    /**
     * @param a deterministic epsilon-free weighting
     * @return compiled evaluator sharing the immutable graph
     */
    public static CompiledWeightedDfa of(WeightedAutomaton a) {
        return of(a, WeightedDfaIndex.of(a.graph()));
    }
    /**
     * @param a scalar weighting
     * @param index precompiled index of the identical graph
     * @return evaluator reusing the index without copying weights or lookup arrays
     */
    public static CompiledWeightedDfa of(WeightedAutomaton a, WeightedDfaIndex index) {
        Objects.requireNonNull(a); Objects.requireNonNull(index);
        if (a.graph() != index.graph()) throw new IllegalArgumentException("Same graph required");
        return new CompiledWeightedDfa(a, index);
    }
    /** @return shared immutable transition index */
    public WeightedDfaIndex index() { return index; }
    /**
     * @param a new weighting of the identical graph
     * @return view reusing the exact interval index
     */
    public CompiledWeightedDfa reweight(WeightedAutomaton a) { return of(a, index); }
    /**
     * @param state current vertex, or -1
     * @param symbol next symbol
     * @return logical edge ID, or -1
     */
    public int transition(int state, int symbol) { return index.transition(state, symbol); }
    /**
     * @param word consumed symbols
     * @return unique accepting path cost, or +infinity
     */
    public double cost(int... word) { return cost(word, 0, word.length); }
    /**
     * @param text input array
     * @param from inclusive slice start
     * @param to exclusive slice end
     * @return cost including initial/final boundaries; +infinity on rejection
     */
    public double cost(int[] text, int from, int to) {
        Objects.checkFromToIndex(from, to, text.length);
        AutomatonGraph graph = automaton.graph(); int state = graph.getInitialState();
        if (state < 0) return Double.POSITIVE_INFINITY;
        double result = automaton.initialWeight();
        for (int i = from; i < to; i++) {
            int edge = transition(state, text[i]);
            if (edge < 0) return Double.POSITIVE_INFINITY;
            result = WeightMath.add(result, automaton.weight(edge)); state = graph.target(edge);
        }
        return graph.isAccept(state) ? WeightMath.add(result, automaton.finalWeight(state)) : Double.POSITIVE_INFINITY;
    }
}
