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

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable compiled deterministic vector evaluator. The interval index is shared
 * with scalar projections/scalarizations and can be reused for new vector costs.
 * The caller-buffer API allocates nothing after setup, including on rejection.
 * Vector sums use checked IEEE double addition, as in the compiled scalar DFA.
 */
public final class CompiledMultiWeightedDfa {
    private final MultiWeightedAutomaton automaton;
    private final CompiledWeightedDfa index;
    private CompiledMultiWeightedDfa(MultiWeightedAutomaton automaton, CompiledWeightedDfa index) {
        this.automaton = automaton; this.index = index;
    }
    /**
     * @param a deterministic epsilon-free vector weighting
     * @return compiled evaluator sharing the graph; rejects ambiguous graphs
     */
    public static CompiledMultiWeightedDfa of(MultiWeightedAutomaton a) {
        return new CompiledMultiWeightedDfa(a, CompiledWeightedDfa.of(a.project(0)));
    }
    /** @return number of cost dimensions */
    public int dimensions() { return automaton.dimensions(); }
    /** @return shared immutable structure */
    public AutomatonGraph graph() { return automaton.graph(); }
    /**
     * @param state current state or -1
     * @param symbol consumed symbol
     * @return stable logical edge ID, or -1 on missing transition
     */
    public int transition(int state, int symbol) { return index.transition(state, symbol); }
    /**
     * @param a new vector weighting of the identical graph (dimension may change)
     * @return evaluator reusing the exact interval index without retaining prior weights
     */
    public CompiledMultiWeightedDfa reweight(MultiWeightedAutomaton a) {
        return new CompiledMultiWeightedDfa(a, index.reweight(a.project(0)));
    }
    /**
     * @param dimension retained component
     * @return compiled scalar view reusing the exact interval index
     */
    public CompiledWeightedDfa project(int dimension) { return index.reweight(automaton.project(dimension)); }
    /**
     * @param coefficients finite linear coefficients
     * @return compiled scalar view sharing graph, vector costs and interval index
     */
    public CompiledWeightedDfa scalarize(double... coefficients) { return index.reweight(automaton.scalarize(coefficients)); }
    /**
     * @param word consumed symbols
     * @return cost vector of the unique accepting path, or null if the word is absent
     */
    public double[] costs(int... word) { return costs(word, 0, word.length); }
    /**
     * @param text input array
     * @param from inclusive start
     * @param to exclusive end
     * @return new vector for the slice, or null if not accepted
     */
    public double[] costs(int[] text, int from, int to) {
        double[] output = new double[dimensions()]; return costs(text, from, to, output) ? output : null;
    }
    /**
     * @param word existing word array
     * @param output caller buffer of exactly dimensions() entries
     * @return true on acceptance; false fills output with +infinity
     */
    public boolean costs(int[] word, double[] output) { return costs(word, 0, word.length, output); }
    /**
     * Evaluates without allocating a result or work buffer. Output is overwritten;
     * rejection fills all components with +infinity and returns false, rather than
     * inventing a vector for an unrecognized word. Finite arithmetic overflow
     * throws; output contents after an arithmetic exception are unspecified.
     * Time O(L(log I + d)), where I is the outgoing interval count and d dimensions.
     * @param text existing input array
     * @param from inclusive start
     * @param to exclusive end
     * @param output caller buffer of exactly dimensions() entries
     * @return whether the slice is accepted
     */
    public boolean costs(int[] text, int from, int to, double[] output) {
        Objects.checkFromToIndex(from, to, text.length);
        int d = dimensions();
        if (output.length != d) throw new IllegalArgumentException("Expected " + d + " output components");
        int state = graph().initial;
        if (state < 0) { Arrays.fill(output, Double.POSITIVE_INFINITY); return false; }
        for (int j = 0; j < d; j++) output[j] = automaton.initialWeight(j);
        for (int i = from; i < to; i++) {
            int edge = transition(state, text[i]);
            if (edge < 0) { Arrays.fill(output, Double.POSITIVE_INFINITY); return false; }
            for (int j = 0; j < d; j++) output[j] = WeightMath.add(output[j], automaton.weight(edge, j));
            state = graph().target[edge];
        }
        if (!graph().accept[state]) { Arrays.fill(output, Double.POSITIVE_INFINITY); return false; }
        for (int j = 0; j < d; j++) output[j] = WeightMath.add(output[j], automaton.finalWeight(state, j));
        return true;
    }
}
