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
 * Immutable fixed-dimension additive vector weighting of a shared graph.
 * An ambiguous word denotes a set of vectors, not one componentwise minimum.
 * Scalarizations and dimension projections share both structure and underlying
 * vector storage. Scalarization creates only an O(d) coefficient snapshot;
 * each requested weight costs O(d), without an O(E) cache or graph copy.
 */
public final class MultiWeightedAutomaton {
    interface Vectors {
        double edge(int edge, int dimension);
        double initial(int dimension);
        double terminal(int state, int dimension);
    }
    private final AutomatonGraph graph;
    private final int dimensions;
    private final Vectors vectors;
    MultiWeightedAutomaton(AutomatonGraph graph, int dimensions, Vectors vectors) {
        if (dimensions <= 0) throw new IllegalArgumentException("Positive dimension required");
        this.graph = Objects.requireNonNull(graph); this.dimensions = dimensions; this.vectors = vectors;
    }
    /**
     * @param graph shared structure
     * @param dimensions positive dimension
     * @param weights flattened edge-major vectors
     * @return zero-boundary weighting
     */
    public static MultiWeightedAutomaton of(AutomatonGraph graph, int dimensions, double[] weights) {
        return of(graph, dimensions, weights, new double[positive(dimensions)], new double[Math.multiplyExact(graph.getNbStates(), dimensions)]);
    }
    private static int positive(int d) { if (d <= 0) throw new IllegalArgumentException("Positive dimension required"); return d; }
    /**
     * @param graph shared structure
     * @param dimensions dimension
     * @param weights flattened edge-major costs
     * @param initial initial vector
     * @param finals flattened state-major terminal vectors
     * @return immutable weighting with defensive copies
     */
    public static MultiWeightedAutomaton of(AutomatonGraph graph, int dimensions, double[] weights, double[] initial, double[] finals) {
        positive(dimensions);
        double[] w = WeightMath.copy(weights, Math.multiplyExact(graph.getNbTransitions(), dimensions));
        double[] i = WeightMath.copy(initial, dimensions), f = WeightMath.copy(finals, Math.multiplyExact(graph.getNbStates(), dimensions));
        return new MultiWeightedAutomaton(graph, dimensions, new Vectors() {
            public double edge(int e, int d) { return w[e * dimensions + d]; }
            public double initial(int d) { return i[d]; }
            public double terminal(int s, int d) { return f[s * dimensions + d]; }
        });
    }
    /**
     * @param dimensions positive number of dimensions
     * @return convenience builder
     */
    public static Builder builder(int dimensions) { return new Builder(dimensions); }
    /**
     * @return shared immutable structure
     */
    public AutomatonGraph graph() { return graph; }
    /**
     * @return fixed dimension
     */
    public int dimensions() { return dimensions; }
    private void check(int d) { if (d < 0 || d >= dimensions) throw new IndexOutOfBoundsException("Dimension " + d); }
    /**
     * @param edge logical edge
     * @param dimension component
     * @return cost
     */
    public double weight(int edge, int dimension) { check(dimension); return vectors.edge(edge, dimension); }
    /**
     * @param dimension component
     * @return initial cost
     */
    public double initialWeight(int dimension) { check(dimension); return vectors.initial(dimension); }
    /**
     * @param state state
     * @param dimension component
     * @return terminal cost
     */
    public double finalWeight(int state, int dimension) { graph.checkState(state); check(dimension); return vectors.terminal(state, dimension); }
    /**
     * @param edge logical edge
     * @return copied edge vector
     */
    public double[] weights(int edge) { double[] v = new double[dimensions]; for (int d = 0; d < dimensions; d++) v[d] = weight(edge, d); return v; }
    double[] initialVector() { double[] v = new double[dimensions]; for (int d = 0; d < dimensions; d++) v[d] = initialWeight(d); return v; }
    double[] finalVector(int s) { double[] v = new double[dimensions]; for (int d = 0; d < dimensions; d++) v[d] = finalWeight(s, d); return v; }
    /**
     * @param dimension retained component
     * @return O(1)-access scalar view, without copying weights or graph
     */
    public WeightedAutomaton project(int dimension) {
        check(dimension);
        return new WeightedAutomaton(graph, new WeightedAutomaton.Weights() {
            public double edge(int e) { return weight(e, dimension); }
            public double initial() { return initialWeight(dimension); }
            public double terminal(int s) { return finalWeight(s, dimension); }
        });
    }
    /**
     * @param selected retained/reordered dimensions, at least one
     * @return lightweight dimension-selection view
     */
    public MultiWeightedAutomaton selectDimensions(int... selected) {
        positive(selected.length); int[] map = selected.clone(); for (int d : map) check(d);
        return new MultiWeightedAutomaton(graph, map.length, new Vectors() {
            public double edge(int e, int d) { return weight(e, map[d]); }
            public double initial(int d) { return initialWeight(map[d]); }
            public double terminal(int s, int d) { return finalWeight(s, map[d]); }
        });
    }
    /**
     * @param coefficients finite linear coefficients; negative coefficients may require Bellman-Ford
     * @return lightweight scalar view; no paths are pruned
     */
    public WeightedAutomaton scalarize(double... coefficients) {
        double[] c = WeightMath.copy(coefficients, dimensions);
        return new WeightedAutomaton(graph, new WeightedAutomaton.Weights() {
            public double edge(int e) { double sum = 0; for (int d = 0; d < dimensions; d++) sum = WeightMath.add(sum, WeightMath.multiply(c[d], weight(e, d))); return sum; }
            public double initial() { double sum = 0; for (int d = 0; d < dimensions; d++) sum = WeightMath.add(sum, WeightMath.multiply(c[d], initialWeight(d))); return sum; }
            public double terminal(int s) { double sum = 0; for (int d = 0; d < dimensions; d++) sum = WeightMath.add(sum, WeightMath.multiply(c[d], finalWeight(s, d))); return sum; }
        });
    }
    /**
     * @param coefficients linear coefficients
     * @return materialized scalar weights, still sharing the graph
     */
    public WeightedAutomaton scalarizeMaterialized(double... coefficients) {
        WeightedAutomaton view = scalarize(coefficients); double[] w = new double[graph.getNbTransitions()], f = new double[graph.getNbStates()];
        for (int e = 0; e < w.length; e++) w[e] = view.weight(e);
        for (int s = 0; s < f.length; s++) f[s] = view.finalWeight(s);
        return WeightedAutomaton.of(graph, w, view.initialWeight(), f);
    }
    /**
     * @param path accepting witness on this graph
     * @return additive vector for that very path
     */
    public double[] pathCosts(WeightedPath path) {
        if (path.graph() != graph || path.start() != graph.initial || !graph.accept[path.end()]) throw new IllegalArgumentException("Accepting path on this graph required");
        int[] edges = path.edges(); WeightMath.Exactness certificate = new WeightMath.Exactness();
        for (int d = 0; d < dimensions; d++) {
            certificate.include(initialWeight(d)); certificate.include(finalWeight(path.end(), d));
            for (int e : edges) certificate.include(weight(e, d));
        }
        if (!certificate.safe(edges.length + 2.0)) {
            double[] result = new double[dimensions];
            for (int d = 0; d < dimensions; d++) {
                BigDecimal value = new BigDecimal(initialWeight(d));
                for (int e : edges) value = value.add(new BigDecimal(weight(e, d)));
                value = value.add(new BigDecimal(finalWeight(path.end(), d))); result[d] = value.doubleValue();
                if (!Double.isFinite(result[d])) throw new ArithmeticException("Cost overflow");
            }
            return result;
        }
        double[] v = initialVector();
        for (int e : edges) for (int d = 0; d < dimensions; d++) v[d] = WeightMath.add(v[d], weight(e, d));
        for (int d = 0; d < dimensions; d++) v[d] = WeightMath.add(v[d], finalWeight(path.end(), d));
        return v;
    }
    /**
     * @param priority permutation of all dimensions, or empty for natural order
     * @return DAG lexicographic optimum
     */
    public MultiWeightedPath lexicographicBestPath(int... priority) { return MultiWeightedPaths.lexicographic(this, priority); }
    /**
     * @return exact DAG front with one representative per nondominated vector
     */
    public ParetoResult paretoFront() { return MultiWeightedPaths.pareto(this, false); }
    /**
     * @param allPaths retain all paths with equal nondominated vectors; can be exponentially many
     * @return exact DAG result
     */
    public ParetoResult paretoFront(boolean allPaths) { return MultiWeightedPaths.pareto(this, allPaths); }
    /**
     * @param word fixed word
     * @return exact front for that word; epsilon cycles are rejected
     */
    public ParetoResult paretoForWord(int... word) {
        LayeredAutomaton layers = LayeredAutomaton.forWord(graph, word);
        ParetoResult result = layers.evaluate(this).paretoFront();
        List<MultiWeightedPath> paths = new ArrayList<>(); for (MultiWeightedPath p : result.paths()) paths.add(layers.map(p));
        return new ParetoResult(paths, false);
    }
    /**
     * @param other same-dimension operand
     * @return full union of word/vector relations
     */
    public MultiWeightedAutomaton union(MultiWeightedAutomaton other) { return MultiWeightedOperations.union(this, other); }
    /**
     * @param other same-dimension operand
     * @return concatenation with componentwise sums
     */
    public MultiWeightedAutomaton concatenate(MultiWeightedAutomaton other) { return MultiWeightedOperations.concatenate(this, other); }
    /**
     * @param other same-dimension operand
     * @return synchronized product with componentwise sums
     */
    public MultiWeightedAutomaton intersection(MultiWeightedAutomaton other) { return MultiWeightedOperations.intersection(this, other); }
    /**
     * @return reversed relation, subject to floating summation rounding
     */
    public MultiWeightedAutomaton revert() { return MultiWeightedOperations.revert(this); }
    /**
     * @return full vector relation with useless states and equal-vector redundant labels removed
     */
    public MultiWeightedAutomaton simplify() { return MultiWeightedOperations.simplify(this); }

    /**
     * Mutable vector builder; vectors are copied on entry and snapshots are independent.
     */
    public static final class Builder {
        final int dimensions; final AutomatonGraph.Builder graph = AutomatonGraph.builder();
        final List<double[]> edges = new ArrayList<>(), finals = new ArrayList<>(); double[] initial;
        private Builder(int d) { dimensions = positive(d); initial = new double[d]; }
        /**
         * @return new state ID
         */
        public int addState() { finals.add(new double[dimensions]); return graph.addState(); }
        /**
         * @param state initial state
         * @param costs initial vector, or omitted for zero
         */
        public void setInitial(int state, double... costs) { double[] v = costs.length == 0 ? new double[dimensions] : WeightMath.copy(costs, dimensions); graph.setInitial(state); initial = v; }
        /**
         * @param state final state
         * @param costs terminal vector, or omitted for zero
         */
        public void setAccept(int state, double... costs) { double[] v = costs.length == 0 ? new double[dimensions] : WeightMath.copy(costs, dimensions); graph.setAccept(state); finals.set(state, v); }
        /**
         * @param from origin
         * @param to destination
         * @param label symbols
         * @param costs edge vector
         * @return edge ID
         */
        public int addTransition(int from, int to, IntervalSet label, double... costs) {
            double[] v = WeightMath.copy(costs, dimensions); int e = graph.addTransition(from, to, label); edges.add(v); return e;
        }
        /**
         * @param from origin
         * @param to destination
         * @param label interval
         * @param costs vector
         * @return edge ID
         */
        public int addTransition(int from, int to, Interval label, double... costs) { return addTransition(from, to, IntervalSet.fromInterval(label), costs); }
        /**
         * @param from origin
         * @param to destination
         * @param symbol symbol
         * @param costs vector
         * @return edge ID
         */
        public int addTransition(int from, int to, int symbol, double... costs) { return addTransition(from, to, IntervalSet.fromIntArray(symbol), costs); }
        /**
         * @param from origin
         * @param to destination
         * @param costs vector
         * @return epsilon edge ID
         */
        public int addEpsilonTransition(int from, int to, double... costs) {
            double[] v = WeightMath.copy(costs, dimensions); int e = graph.addEpsilonTransition(from, to); edges.add(v); return e;
        }
        /**
         * @return independent immutable weighting
         */
        public MultiWeightedAutomaton build() {
            double[] w = new double[Math.multiplyExact(edges.size(), dimensions)], f = new double[Math.multiplyExact(finals.size(), dimensions)];
            for (int e = 0; e < edges.size(); e++) System.arraycopy(edges.get(e), 0, w, e * dimensions, dimensions);
            for (int s = 0; s < finals.size(); s++) System.arraycopy(finals.get(s), 0, f, s * dimensions, dimensions);
            return of(graph.build(), dimensions, w, initial, f);
        }
    }
}
