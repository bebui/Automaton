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
 * Reusable structural unfolding. Only vertices reachable from the initial pair
 * and coaccessible to a permitted final pair are built. Epsilon edges stay in
 * the same layer: an unfolding is not necessarily a DAG. Construction/storage
 * is O((T+1)V+TE) before trimming; weight changes never rebuild the structure.
 */
public final class LayeredAutomaton {
    /**
     * Position-aware cost, constant across all symbols of the logical edge.
     */
    @FunctionalInterface public interface PositionWeights {
        /**
         * @param position consumed prefix length, including T for epsilon edges
         * @param edge original edge ID
         * @return finite cost
         */
        double weight(int position, int edge);
    }
    private final AutomatonGraph base, graph;
    private final int horizon;
    private final int[] originalState, nodePosition, originalEdge, edgePosition;

    private LayeredAutomaton(AutomatonGraph base, int horizon, boolean atMost, int[] word) {
        if (horizon < 0) throw new IllegalArgumentException("Negative horizon");
        this.base = Objects.requireNonNull(base); this.horizon = horizon;
        int n = base.getNbStates(), count = Math.multiplyExact(Math.addExact(horizon, 1), n);
        boolean[] forward = new boolean[count], backward = new boolean[count];
        int[] queue = new int[count]; int head = 0, tail = 0;
        if (base.initial >= 0) { forward[base.initial] = true; queue[tail++] = base.initial; }
        while (head < tail) {
            int node = queue[head++], pos = node / n, s = node % n;
            for (int i = base.outStart[s]; i < base.outStart[s + 1]; i++) {
                int e = base.outEdges[i]; if (!base.epsilon[e] && (pos == horizon || (word != null && !base.accepts(e, word[pos])))) continue;
                int t = (pos + (base.epsilon[e] ? 0 : 1)) * n + base.target[e];
                if (!forward[t]) { forward[t] = true; queue[tail++] = t; }
            }
        }
        head = tail = 0;
        for (int pos = atMost ? 0 : horizon; pos <= horizon; pos++) for (int s = 0; s < n; s++) if (base.accept[s] && forward[pos * n + s]) {
            int node = pos * n + s; backward[node] = true; queue[tail++] = node;
        }
        while (head < tail) {
            int node = queue[head++], pos = node / n, s = node % n;
            for (int i = base.inStart[s]; i < base.inStart[s + 1]; i++) {
                int e = base.inEdges[i]; if (!base.epsilon[e] && (pos == 0 || (word != null && !base.accepts(e, word[pos - 1])))) continue;
                int p = (pos - (base.epsilon[e] ? 0 : 1)) * n + base.source[e];
                if (forward[p] && !backward[p]) { backward[p] = true; queue[tail++] = p; }
            }
        }
        AutomatonGraph.Builder b = AutomatonGraph.builder(); int[] ids = new int[count]; Arrays.fill(ids, -1);
        int kept = 0; for (boolean v : backward) if (v) kept++;
        originalState = new int[kept]; nodePosition = new int[kept];
        for (int node = 0; node < count; node++) if (backward[node]) {
            int id = b.addState(); ids[node] = id; originalState[id] = node % n; nodePosition[id] = node / n;
            if (base.accept[node % n] && (atMost || node / n == horizon)) b.setAccept(id);
        }
        if (base.initial >= 0 && ids[base.initial] >= 0) b.setInitial(ids[base.initial]);
        List<Integer> edges = new ArrayList<>(), positions = new ArrayList<>();
        for (int node = 0; node < count; node++) if (ids[node] >= 0) {
            int pos = node / n, s = node % n;
            for (int i = base.outStart[s]; i < base.outStart[s + 1]; i++) {
                int e = base.outEdges[i]; if (!base.epsilon[e] && (pos == horizon || (word != null && !base.accepts(e, word[pos])))) continue;
                int next = (pos + (base.epsilon[e] ? 0 : 1)) * n + base.target[e]; if (ids[next] < 0) continue;
                if (base.epsilon[e]) b.addEpsilonTransition(ids[node], ids[next]);
                else if (word != null) b.addTransition(ids[node], ids[next], word[pos]);
                else b.addTransition(ids[node], ids[next], base.labels(e));
                edges.add(e); positions.add(pos);
            }
        }
        graph = b.build(); originalEdge = new int[edges.size()]; edgePosition = new int[edges.size()];
        for (int e = 0; e < edges.size(); e++) { originalEdge[e] = edges.get(e); edgePosition[e] = positions.get(e); }
    }
    /**
     * @param graph base structure
     * @param horizon exact symbol length
     * @return structural unfolding
     */
    public static LayeredAutomaton exactly(AutomatonGraph graph, int horizon) { return new LayeredAutomaton(graph, horizon, false, null); }
    /**
     * @param graph base structure
     * @param horizon maximum symbol length
     * @return unfolding accepting at every layer
     */
    public static LayeredAutomaton atMost(AutomatonGraph graph, int horizon) { return new LayeredAutomaton(graph, horizon, true, null); }
    /**
     * @param graph base structure
     * @param word imposed word
     * @return unfolding restricted to these exact symbols
     */
    public static LayeredAutomaton forWord(AutomatonGraph graph, int... word) { return new LayeredAutomaton(graph, word.length, false, word); }
    /**
     * @return base graph
     */
    public AutomatonGraph baseGraph() { return base; }
    /**
     * @return unfolded shared graph
     */
    public AutomatonGraph graph() { return graph; }
    /**
     * @return horizon
     */
    public int horizon() { return horizon; }
    /**
     * @param state unfolded state
     * @return original state
     */
    public int originalState(int state) { return originalState[state]; }
    /**
     * @param state unfolded state
     * @return consumed-symbol position
     */
    public int position(int state) { return nodePosition[state]; }
    /**
     * @param edge unfolded edge
     * @return original logical edge
     */
    public int originalEdge(int edge) { return originalEdge[edge]; }
    /**
     * @param a weighting of the exact base graph
     * @return lightweight weighting sharing the unfolded graph
     */
    public WeightedAutomaton evaluate(WeightedAutomaton a) {
        check(a);
        return new WeightedAutomaton(graph, new WeightedAutomaton.Weights() {
            public double edge(int e) { return a.weight(originalEdge[e]); }
            public double initial() { return a.initialWeight(); }
            public double terminal(int s) { return a.finalWeight(originalState[s]); }
        });
    }
    /**
     * Snapshots position-dependent weights once; subsequent evaluations can reuse this structure.
     * @param a supplies boundaries
     * @param weights position/edge costs
     * @return immutable weighting
     */
    public WeightedAutomaton evaluate(WeightedAutomaton a, PositionWeights weights) {
        check(a); double[] w = new double[originalEdge.length], f = new double[originalState.length];
        for (int e = 0; e < w.length; e++) w[e] = weights.weight(edgePosition[e], originalEdge[e]);
        for (int s = 0; s < f.length; s++) f[s] = a.finalWeight(originalState[s]);
        return WeightedAutomaton.of(graph, w, a.initialWeight(), f);
    }
    /**
     * @param a vector weighting of the base graph
     * @return lightweight unfolded vector view
     */
    public MultiWeightedAutomaton evaluate(MultiWeightedAutomaton a) {
        if (a.graph() != base) throw new IllegalArgumentException("Weighting must share the base graph");
        return new MultiWeightedAutomaton(graph, a.dimensions(), new MultiWeightedAutomaton.Vectors() {
            public double edge(int e, int d) { return a.weight(originalEdge[e], d); }
            public double initial(int d) { return a.initialWeight(d); }
            public double terminal(int s, int d) { return a.finalWeight(originalState[s], d); }
        });
    }
    private void check(WeightedAutomaton a) { if (a.graph() != base) throw new IllegalArgumentException("Weighting must share the base graph"); }
    /**
     * @param a weighting
     * @return original-graph witness at this horizon
     */
    public WeightedPath bestPath(WeightedAutomaton a) { return map(evaluate(a).bestPath()); }
    /**
     * @param path path on this unfolded graph
     * @return corresponding path on the base graph
     */
    public WeightedPath map(WeightedPath path) {
        if (path == null) return null;
        if (path.graph() != graph) throw new IllegalArgumentException("Foreign unfolded path");
        int[] edges = path.edges(); for (int i = 0; i < edges.length; i++) edges[i] = originalEdge[edges[i]];
        return new WeightedPath(base, originalState[path.start()], edges, path.word(), path.cost());
    }
    /**
     * @param path vector witness on this unfolding
     * @return original-graph vector witness
     */
    public MultiWeightedPath map(MultiWeightedPath path) {
        if (path == null) return null;
        if (path.graph() != graph) throw new IllegalArgumentException("Foreign unfolded path");
        int[] edges = path.edges(); for (int i = 0; i < edges.length; i++) edges[i] = originalEdge[edges[i]];
        return new MultiWeightedPath(new WeightedPath(base, originalState[path.start()], edges, path.word(), 0), path.costs(), path.exactVector());
    }
    /**
     * @param a base weighting
     * @return reusable forward/backward distances for that weighting
     */
    public Analysis analyze(WeightedAutomaton a) { return analyzeEvaluated(evaluate(a)); }
    /**
     * @param a supplies boundaries
     * @param weights position costs
     * @return analysis of a weight snapshot
     */
    public Analysis analyze(WeightedAutomaton a, PositionWeights weights) { return analyzeEvaluated(evaluate(a, weights)); }
    private Analysis analyzeEvaluated(WeightedAutomaton a) { return new Analysis(a); }
    /**
     * Distances belonging to one immutable weighting, not reused across different weights.
     */
    public final class Analysis {
        private final WeightedAutomaton a;
        private final ShortestPathResult forward;
        private final double[] backward;
        private Analysis(WeightedAutomaton a) { this.a = a; forward = WeightedPaths.shortestPaths(a); backward = WeightedPaths.backwardDistances(a); }
        /**
         * @return forward distance copy
         */
        public double[] forwardDistances() { return forward.distances(); }
        /**
         * @return backward distance copy including final boundaries
         */
        public double[] backwardDistances() { return backward.clone(); }
        /**
         * @return optimum at the configured horizon
         */
        public double optimalCost() { return forward.optimalCost(); }
        /**
         * @return original-graph witness
         * @throws IllegalStateException if unbounded
         */
        public WeightedPath bestPath() { return map(forward.bestPath()); }
        /**
         * @param position consumed-symbol index
         * @param symbol required symbol
         * @return conditional optimum
         */
        public double costAt(int position, int symbol) {
            if (position < 0 || position >= horizon) throw new IndexOutOfBoundsException("Position " + position);
            double best = Double.POSITIVE_INFINITY;
            for (int e = 0; e < originalEdge.length; e++) if (edgePosition[e] == position && !graph.epsilon[e] && graph.accepts(e, symbol))
                best = Math.min(best, WeightedPaths.join(forward.distanceTo(graph.source[e]), a.weight(e), backward[graph.target[e]]));
            return best;
        }
    }
}
