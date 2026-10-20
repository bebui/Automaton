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
import java.util.function.ToDoubleFunction;

/**
 * Immutable additive scalar weighting of a shared multigraph. For a word the
 * cost is the infimum of its accepting paths, including initial/final weights.
 * Missing words cost +infinity; relevant negative epsilon cycles can give
 * -infinity without an optimal path. Inputs must be finite; arithmetic overflow
 * is rejected. Floating-point sums may round; no approximate state merging is used.
 */
public final class WeightedAutomaton {
    interface Weights {
        double edge(int edge);
        double initial();
        double terminal(int state);
    }
    private static final class ArrayWeights implements Weights {
        private final double[] edges, finals;
        private final double initial;
        ArrayWeights(double[] edges, double initial, double[] finals) { this.edges = edges; this.initial = initial; this.finals = finals; }
        public double edge(int e) { return edges[e]; }
        public double initial() { return initial; }
        public double terminal(int s) { return finals[s]; }
    }
    private final AutomatonGraph graph;
    private final Weights weights;
    final boolean hasEpsilon;
    WeightedAutomaton(AutomatonGraph graph, Weights weights) {
        this.graph = Objects.requireNonNull(graph); this.weights = weights;
        hasEpsilon = graph.hasEpsilon;
    }
    /**
     * @param graph shared structure
     * @param edgeWeights weights in edge-ID order
     * @return zero-boundary weighting
     */
    public static WeightedAutomaton of(AutomatonGraph graph, double[] edgeWeights) {
        return of(graph, edgeWeights, 0, new double[graph.getNbStates()]);
    }
    /**
     * Copies weights; final costs of nonaccepting states are ignored.
     * @param graph shared structure
     * @param edgeWeights edge weights
     * @param initialWeight initial boundary
     * @param finalWeights one entry per state
     * @return immutable weighting
     */
    public static WeightedAutomaton of(AutomatonGraph graph, double[] edgeWeights, double initialWeight, double[] finalWeights) {
        double[] edges = WeightMath.copy(edgeWeights, graph.getNbTransitions());
        double[] finals = WeightMath.copy(finalWeights, graph.getNbStates());
        double initial = WeightMath.finite(initialWeight);
        return new WeightedAutomaton(graph, new ArrayWeights(edges, initial, finals));
    }
    /**
     * Snapshots a 3.0 automaton, preserving ambiguity. A mixed transition receives
     * the same supplied cost for both modes; use the import mapping to distinguish them.
     * @param automaton source
     * @param weight transition-cost callback
     * @return weighting
     */
    public static WeightedAutomaton from(Automaton automaton, ToDoubleFunction<Transition> weight) {
        AutomatonGraph.ImportResult imported = AutomatonGraph.importAutomaton(automaton);
        double[] costs = new double[imported.graph().getNbTransitions()];
        for (int e = 0; e < costs.length; e++) costs[e] = weight.applyAsDouble(imported.originalTransition(e));
        return of(imported.graph(), costs);
    }
    /**
     * @return builder accepting structure and costs together
     */
    public static Builder builder() { return new Builder(); }
    /**
     * @return shared immutable structure
     */
    public AutomatonGraph graph() { return graph; }
    /**
     * @param edge logical edge ID
     * @return scalar cost
     */
    public double weight(int edge) { return weights.edge(edge); }
    /**
     * @return initial boundary
     */
    public double initialWeight() { return weights.initial(); }
    /**
     * @param state state ID
     * @return terminal boundary (used only for final states)
     */
    public double finalWeight(int state) { graph.checkState(state); return weights.terminal(state); }
    /**
     * @param costs new edge costs
     * @return independent weighting sharing this exact graph and boundaries
     */
    public WeightedAutomaton withWeights(double[] costs) {
        double[] edges = WeightMath.copy(costs, graph.getNbTransitions());
        double initial = initialWeight();
        double[] finals = new double[graph.getNbStates()];
        for (int s = 0; s < finals.length; s++) finals[s] = finalWeight(s);
        return new WeightedAutomaton(graph, new ArrayWeights(edges, initial, finals));
    }
    /**
     * @param word consumed symbols
     * @return minimum accepting cost, +infinity if absent, -infinity if unbounded
     */
    public double cost(int... word) {
        Objects.requireNonNull(word);
        return graph.isDeterministic() ? deterministicCost(word) : evaluator().cost(word);
    }
    private double deterministicCost(int[] word) {
        int s = graph.initial; if (s < 0) return Double.POSITIVE_INFINITY;
        double cost = initialWeight();
        for (int symbol : word) {
            int found = -1;
            for (int i = graph.outStart[s]; i < graph.outStart[s + 1]; i++) {
                int e = graph.outEdges[i]; if (graph.accepts(e, symbol)) { found = e; break; }
            }
            if (found < 0) return Double.POSITIVE_INFINITY;
            cost = WeightMath.add(cost, weight(found)); s = graph.target[found];
        }
        return graph.accept[s] ? WeightMath.add(cost, finalWeight(s)) : Double.POSITIVE_INFINITY;
    }
    /**
     * @return thread-confined reusable cost evaluator; no path is built
     */
    public Evaluator evaluator() { return new Evaluator(this); }
    /**
     * @return optimum over all accepting paths
     */
    public double optimalCost() { return WeightedPaths.shortestPaths(this, false).optimalCost(); }
    /**
     * @return best accepting path, or null if absent
     * @throws IllegalStateException if optimum is unbounded
     */
    public WeightedPath bestPath() { return WeightedPaths.shortestPaths(this).bestPath(); }
    /**
     * Reconstructs a best path for exactly this word using its layered product.
     * Cost-only queries avoid this product and its O((L+1)V+LE) storage.
     * @param word consumed word
     * @return best witness, or null if absent
     * @throws IllegalStateException if a relevant epsilon cycle makes its cost unbounded
     */
    public WeightedPath bestPath(int... word) {
        Objects.requireNonNull(word);
        if (graph.initial < 0) return null;
        return LayeredAutomaton.forWord(graph, word).bestPath(this);
    }
    /**
     * @param other operand
     * @return minimum-cost union
     */
    public WeightedAutomaton union(WeightedAutomaton other) { return WeightedOperations.union(this, other); }
    /**
     * @param other operand
     * @return minimum over all word splits
     */
    public WeightedAutomaton concatenate(WeightedAutomaton other) { return WeightedOperations.concatenate(this, other); }
    /**
     * @param other operand
     * @return synchronized product with summed costs
     */
    public WeightedAutomaton intersection(WeightedAutomaton other) { return WeightedOperations.intersection(this, other); }
    /**
     * @return reversed cost function, subject to floating-point summation order
     */
    public WeightedAutomaton revert() { return WeightedOperations.revert(this); }
    /**
     * @return weighted Kleene closure, possibly with improving cycles
     */
    public WeightedAutomaton star() { return WeightedOperations.star(this); }
    /**
     * @return weighting with inaccessible/noncoaccessible states removed
     */
    public WeightedAutomaton trim() { return WeightedOperations.trim(this); }
    /**
     * @param length consumed symbol count
     * @return best accepting cost of exactly this length
     */
    public double costOfLength(int length) { return WeightedPaths.fixedLengthCost(this, length, false); }
    /**
     * @param length maximum consumed symbol count
     * @return best accepting cost up to that length
     */
    public double costOfLengthAtMost(int length) { return WeightedPaths.fixedLengthCost(this, length, true); }
    /**
     * @param length consumed symbol count
     * @return best witness, or null if none
     */
    public WeightedPath bestPathOfLength(int length) { return LayeredAutomaton.exactly(graph, length).bestPath(this); }
    /**
     * @param length consumed symbol count
     * @return word of a best path, or null if none
     */
    public int[] bestWordOfLength(int length) { WeightedPath path = bestPathOfLength(length); return path == null ? null : path.word(); }
    /**
     * @return cost-equivalent epsilon-free weighting; negative epsilon cycles are rejected
     */
    public WeightedAutomaton removeEpsilon() { return WeightedTransformations.removeEpsilon(this); }
    /**
     * @return certified residual determinization on bounded integer DAGs, or this if already deterministic
     */
    public WeightedAutomaton determinize() { return WeightedTransformations.determinizeDag(this); }
    /**
     * @return certified weighted minimization of a bounded integer deterministic DAG
     */
    public WeightedAutomaton minimize() { return WeightedTransformations.minimizeDag(this); }
    /**
     * @return immutable weighted DFA index; rejects nondeterminism rather than losing costs
     */
    public CompiledWeightedDfa compile() { return CompiledWeightedDfa.of(this); }
    /**
     * @param k number of paths
     * @return DAG accepting paths in increasing cost order
     */
    public List<WeightedPath> kBestPaths(int k) { return KBestPaths.accepting(this, k); }
    /**
     * @param prefix imposed prefix
     * @return optimum among accepting paths starting with these symbols
     */
    public double optimalCostWithPrefix(int... prefix) { return evaluator().optimalCostWithPrefix(prefix); }

    /**
     * Reusable evaluator; do not share between threads. Epsilon-free NFA evaluation reuses two primitive arrays.
     */
    public static final class Evaluator {
        private final WeightedAutomaton a;
        private final WeightedPaths.EpsilonWorkspace closure;
        private double[] current, next;
        private Evaluator(WeightedAutomaton a) {
            this.a = a; current = new double[a.graph.getNbStates()]; next = new double[current.length];
            closure = a.hasEpsilon ? new WeightedPaths.EpsilonWorkspace(a) : null;
        }
        /**
         * @param word consumed word
         * @return infimum, without witness allocation
         */
        public double cost(int... word) {
            Objects.requireNonNull(word);
            AutomatonGraph g = a.graph; if (g.initial < 0) return Double.POSITIVE_INFINITY;
            if (g.isDeterministic()) return a.deterministicCost(word);
            advance(word);
            return terminalCost(current);
        }
        private void advance(int[] word) {
            AutomatonGraph g = a.graph;
            Arrays.fill(current, Double.POSITIVE_INFINITY); current[g.initial] = a.initialWeight();
            if (closure != null) closure.close(current);
            for (int symbol : word) {
                Arrays.fill(next, Double.POSITIVE_INFINITY);
                for (int s = 0; s < current.length; s++) if (current[s] != Double.POSITIVE_INFINITY) {
                    for (int i = g.outStart[s]; i < g.outStart[s + 1]; i++) {
                        int e = g.outEdges[i]; if (!g.accepts(e, symbol)) continue;
                        double candidate = current[s] == Double.NEGATIVE_INFINITY ? current[s] : WeightMath.add(current[s], a.weight(e));
                        int t = g.target[e]; if (candidate < next[t]) next[t] = candidate;
                    }
                }
                double[] tmp = current; current = next; next = tmp;
                if (closure != null) closure.close(current);
            }
        }
        private double terminalCost(double[] distances) {
            AutomatonGraph g = a.graph;
            double best = Double.POSITIVE_INFINITY;
            for (int s = 0; s < distances.length; s++) if (g.accept[s] && distances[s] != Double.POSITIVE_INFINITY)
                best = Math.min(best, distances[s] == Double.NEGATIVE_INFINITY ? distances[s] : WeightMath.add(distances[s], a.finalWeight(s)));
            return best;
        }
        /**
         * @param prefix required consumed prefix
         * @return optimum over all accepting continuations
         */
        public double optimalCostWithPrefix(int... prefix) {
            Objects.requireNonNull(prefix); if (a.graph.initial < 0) return Double.POSITIVE_INFINITY;
            advance(prefix); return terminalCost(WeightedPaths.solve(a, current, false, false, false).dist);
        }
    }

    /**
     * Convenience builder; IDs and parallel edges follow {@link AutomatonGraph.Builder}.
     */
    public static final class Builder {
        final AutomatonGraph.Builder graph = AutomatonGraph.builder();
        final List<Double> edges = new ArrayList<>(), finals = new ArrayList<>();
        double initial;
        private Builder() { }
        /**
         * @return new state ID
         */
        public int addState() { finals.add(0.0); return graph.addState(); }
        /**
         * @param state initial state
         */
        public void setInitial(int state) { setInitial(state, 0); }
        /**
         * @param state initial state
         * @param cost initial cost
         */
        public void setInitial(int state, double cost) { double v = WeightMath.finite(cost); graph.setInitial(state); initial = v; }
        /**
         * @param state final state
         */
        public void setAccept(int state) { setAccept(state, 0); }
        /**
         * @param state final state
         * @param cost terminal cost
         */
        public void setAccept(int state, double cost) { double v = WeightMath.finite(cost); graph.setAccept(state); finals.set(state, v); }
        /**
         * @param from origin
         * @param to destination
         * @param label symbols
         * @param cost additive cost
         * @return edge ID
         */
        public int addTransition(int from, int to, IntervalSet label, double cost) {
            double v = WeightMath.finite(cost); int e = graph.addTransition(from, to, label); edges.add(v); return e;
        }
        /**
         * @param from origin
         * @param to destination
         * @param label interval
         * @param cost additive cost
         * @return edge ID
         */
        public int addTransition(int from, int to, Interval label, double cost) { return addTransition(from, to, IntervalSet.fromInterval(label), cost); }
        /**
         * @param from origin
         * @param to destination
         * @param symbol symbol
         * @param cost additive cost
         * @return edge ID
         */
        public int addTransition(int from, int to, int symbol, double cost) { return addTransition(from, to, IntervalSet.fromIntArray(symbol), cost); }
        /**
         * @param from origin
         * @param to destination
         * @param cost additive cost
         * @return epsilon edge ID
         */
        public int addEpsilonTransition(int from, int to, double cost) {
            double v = WeightMath.finite(cost); int e = graph.addEpsilonTransition(from, to); edges.add(v); return e;
        }
        /**
         * @return immutable independent weighting snapshot
         */
        public WeightedAutomaton build() {
            double[] w = new double[edges.size()], f = new double[finals.size()];
            for (int i = 0; i < w.length; i++) w[i] = edges.get(i);
            for (int i = 0; i < f.length; i++) f[i] = finals.get(i);
            return of(graph.build(), w, initial, f);
        }
    }
}
