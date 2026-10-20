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

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Primitive shortest-path algorithms; no symbols are enumerated.
 * DAG distances cost O(V+E); Dijkstra O((V+E) log V); Bellman-Ford O(VE).
 * Bellman-Ford uses primitive synchronous relaxation for bounded integer data,
 * and exact binary-double values via BigDecimal otherwise, including cycle signs.
 * Exact arithmetic may have additional bit complexity and allocation costs.
 */
public final class WeightedPaths {
    private WeightedPaths() { }

    /**
     * @param a weighting
     * @return automatic DAG/Dijkstra/Bellman-Ford result, predecessors retained
     */
    public static ShortestPathResult shortestPaths(WeightedAutomaton a) { return shortestPaths(a, true); }
    /**
     * @param a weighting
     * @param reconstruct retain predecessors
     * @return distances from its weighted initial
     */
    public static ShortestPathResult shortestPaths(WeightedAutomaton a, boolean reconstruct) {
        return from(a, a.graph().initial, a.initialWeight(), reconstruct, false, false);
    }
    /**
     * @param a weighting
     * @param source source vertex
     * @return distances from zero, ignoring initial boundary
     */
    public static ShortestPathResult shortestPaths(WeightedAutomaton a, int source) {
        a.graph().checkState(source); return from(a, source, 0, true, false, false);
    }
    /**
     * @param a weighting
     * @return DAG shortest paths
     * @throws IllegalArgumentException on cycles
     */
    public static ShortestPathResult dagShortestPaths(WeightedAutomaton a) {
        a.graph().requireDag(); return from(a, a.graph().initial, a.initialWeight(), true, false, false);
    }
    /**
     * @param a weighting
     * @return DAG longest paths
     * @throws IllegalArgumentException on cycles
     */
    public static ShortestPathResult dagLongestPaths(WeightedAutomaton a) {
        a.graph().requireDag(); return from(a, a.graph().initial, a.initialWeight(), true, true, false);
    }
    /**
     * @param a weighting
     * @return Dijkstra result
     * @throws IllegalArgumentException on any negative edge
     */
    public static ShortestPathResult dijkstra(WeightedAutomaton a) {
        for (int e = 0; e < a.graph().source.length; e++) if (a.weight(e) < 0) throw new IllegalArgumentException("Dijkstra requires nonnegative edges");
        return from(a, a.graph().initial, a.initialWeight(), true, false, true);
    }
    /**
     * @param a weighting
     * @return Bellman-Ford result including all reachable negative-cycle descendants
     */
    public static ShortestPathResult bellmanFord(WeightedAutomaton a) {
        double[] seed = seeds(a.graph().getNbStates(), a.graph().initial, a.initialWeight());
        Relaxed r = bellman(a, seed, false, false, true);
        return new ShortestPathResult(a, r.dist, r.parent, a.graph().initial, a.initialWeight(), false);
    }
    private static ShortestPathResult from(WeightedAutomaton a, int start, double value, boolean track, boolean longest, boolean forceDijkstra) {
        double[] seed = seeds(a.graph().getNbStates(), start, value);
        Relaxed r = forceDijkstra ? dijkstra(a, seed, false, false, track)
                : a.graph().isAcyclic() ? dag(a, seed, false, longest, track)
                : hasNegative(a, false) ? bellman(a, seed, false, false, track) : dijkstra(a, seed, false, false, track);
        return new ShortestPathResult(a, r.dist, r.parent, start, value, longest);
    }
    private static double[] seeds(int n, int state, double weight) {
        double[] seed = new double[n]; Arrays.fill(seed, Double.POSITIVE_INFINITY);
        if (state >= 0) seed[state] = weight;
        return seed;
    }
    /**
     * @param a weighting
     * @return distances to acceptance, including final costs, excluding initial cost
     */
    public static double[] backwardDistances(WeightedAutomaton a) {
        AutomatonGraph g = a.graph(); double[] seed = new double[g.getNbStates()];
        for (int s = 0; s < seed.length; s++) seed[s] = g.accept[s] ? a.finalWeight(s) : Double.POSITIVE_INFINITY;
        return solve(a, seed, true, false, false).dist;
    }
    /**
     * @param a weighting
     * @param destination target
     * @return distances to target with zero terminal boundary
     */
    public static double[] distancesTo(WeightedAutomaton a, int destination) {
        a.graph().checkState(destination);
        return solve(a, seeds(a.graph().getNbStates(), destination, 0), true, false, false).dist;
    }
    /**
     * @param a weighting
     * @return reusable distances for O(1) state/transition conditional queries
     */
    public static Analysis analyze(WeightedAutomaton a) { return new Analysis(a); }
    /**
     * Immutable forward/backward analysis tied to one weighting.
     */
    public static final class Analysis {
        private final WeightedAutomaton a;
        private final ShortestPathResult forward;
        private final double[] backward;
        private Analysis(WeightedAutomaton a) { this.a = a; forward = shortestPaths(a); backward = backwardDistances(a); }
        /**
         * @return immutable forward result
         */
        public ShortestPathResult forward() { return forward; }
        /**
         * @return copied backward distances
         */
        public double[] backward() { return backward.clone(); }
        /**
         * @param state state ID
         * @return accepting optimum through that state
         */
        public double costThroughState(int state) { a.graph().checkState(state); return join(forward.distanceTo(state), 0, backward[state]); }
        /**
         * @param edge edge ID
         * @return accepting optimum through that logical edge
         */
        public double costThroughTransition(int edge) { return join(forward.distanceTo(a.graph().source[edge]), a.weight(edge), backward[a.graph().target[edge]]); }
    }
    /**
     * @param a weighting
     * @param state vertex
     * @return minimum accepting cost via the vertex
     */
    public static double costThroughState(WeightedAutomaton a, int state) {
        a.graph().checkState(state); return join(shortestPaths(a, false).distanceTo(state), 0, backwardDistances(a)[state]);
    }
    /**
     * @param a weighting
     * @param edge edge
     * @return minimum accepting cost via that edge
     */
    public static double costThroughTransition(WeightedAutomaton a, int edge) {
        AutomatonGraph g = a.graph(); return join(shortestPaths(a, false).distanceTo(g.source[edge]), a.weight(edge), backwardDistances(a)[g.target[edge]]);
    }
    static double join(double left, double weight, double right) {
        if (left == Double.POSITIVE_INFINITY || right == Double.POSITIVE_INFINITY) return Double.POSITIVE_INFINITY;
        if (left == Double.NEGATIVE_INFINITY || right == Double.NEGATIVE_INFINITY) return Double.NEGATIVE_INFINITY;
        return WeightMath.add(WeightMath.add(left, weight), right);
    }
    /**
     * Two-layer dynamic programming; epsilon closures do not consume positions.
     * O(T(V+E)) time and O(V) space without epsilon; closures add their shortest-path costs.
     * @param a weighting
     * @param length exact/maximum length
     * @param atMost accept earlier layers
     * @return optimal accepting cost
     */
    public static double fixedLengthCost(WeightedAutomaton a, int length, boolean atMost) {
        if (length < 0) throw new IllegalArgumentException("Negative length");
        AutomatonGraph g = a.graph(); if (g.initial < 0) return Double.POSITIVE_INFINITY;
        double[] current = seeds(g.getNbStates(), g.initial, a.initialWeight()), next = new double[current.length];
        EpsilonWorkspace closure = a.hasEpsilon ? new EpsilonWorkspace(a) : null;
        double best = Double.POSITIVE_INFINITY;
        for (int pos = 0; ; pos++) {
            if (closure != null) closure.close(current);
            if (atMost || pos == length) for (int s = 0; s < current.length; s++) if (g.accept[s])
                best = Math.min(best, join(current[s], 0, a.finalWeight(s)));
            if (pos == length) return best;
            Arrays.fill(next, Double.POSITIVE_INFINITY);
            for (int e = 0; e < g.source.length; e++) if (!g.epsilon[e] && current[g.source[e]] != Double.POSITIVE_INFINITY) {
                double candidate = join(current[g.source[e]], a.weight(e), 0);
                if (candidate < next[g.target[e]]) next[g.target[e]] = candidate;
            }
            double[] tmp = current; current = next; next = tmp;
        }
    }

    static final class Relaxed {
        final double[] dist; final int[] parent;
        Relaxed(double[] dist, int[] parent) { this.dist = dist; this.parent = parent; }
    }
    static Relaxed solve(WeightedAutomaton a, double[] seed, boolean reverse, boolean epsOnly, boolean track) {
        if (a.graph().isAcyclic()) return dag(a, seed, reverse, false, track);
        return hasNegative(a, epsOnly) ? bellman(a, seed, reverse, epsOnly, track) : dijkstra(a, seed, reverse, epsOnly, track);
    }
    static void closeEpsilon(WeightedAutomaton a, double[] distances) {
        // No epsilon DAG special case here: DAG solve must also filter symbol edges.
        Relaxed r = hasNegative(a, true) ? bellman(a, distances, false, true, false) : dijkstra(a, distances, false, true, false);
        System.arraycopy(r.dist, 0, distances, 0, distances.length);
    }
    /** Reuses Dijkstra buffers for nonnegative epsilon closures; signed closures retain the exact fallback. */
    static final class EpsilonWorkspace {
        final WeightedAutomaton a;
        final boolean signed;
        final double[] work;
        final Heap heap;
        EpsilonWorkspace(WeightedAutomaton a) {
            this.a = a; signed = hasNegative(a, true); work = new double[a.graph().getNbStates()]; heap = new Heap(work);
        }
        void close(double[] distances) {
            boolean fallback = signed;
            for (double d : distances) fallback |= d == Double.NEGATIVE_INFINITY;
            if (fallback) { closeEpsilon(a, distances); return; }
            AutomatonGraph g = a.graph(); System.arraycopy(distances, 0, work, 0, work.length);
            heap.size = 0; Arrays.fill(heap.position, -1);
            for (int s = 0; s < work.length; s++) if (Double.isFinite(work[s])) heap.offer(s);
            while (heap.size > 0) {
                int s = heap.poll();
                for (int i = g.outStart[s]; i < g.outStart[s + 1]; i++) {
                    int e = g.outEdges[i]; if (!g.epsilon[e]) continue;
                    double candidate = WeightMath.add(work[s], a.weight(e)); int t = g.target[e];
                    if (candidate < work[t]) { work[t] = candidate; heap.offer(t); }
                }
            }
            System.arraycopy(work, 0, distances, 0, work.length);
        }
    }
    private static boolean hasNegative(WeightedAutomaton a, boolean epsOnly) {
        for (int e = 0; e < a.graph().source.length; e++) if ((!epsOnly || a.graph().epsilon[e]) && a.weight(e) < 0) return true;
        return false;
    }
    private static int[] parents(int n, boolean track) { if (!track) return null; int[] p = new int[n]; Arrays.fill(p, -1); return p; }
    private static Relaxed dag(WeightedAutomaton a, double[] seed, boolean reverse, boolean longest, boolean track) {
        AutomatonGraph g = a.graph(); double[] d = seed.clone(); int[] p = parents(d.length, track);
        if (longest) for (int i = 0; i < d.length; i++) if (d[i] == Double.POSITIVE_INFINITY) d[i] = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < g.order.length; k++) {
            int s = g.order[reverse ? g.order.length - 1 - k : k];
            if (d[s] == (longest ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY)) continue;
            int[] start = reverse ? g.inStart : g.outStart, edges = reverse ? g.inEdges : g.outEdges;
            for (int i = start[s]; i < start[s + 1]; i++) {
                int e = edges[i], t = reverse ? g.source[e] : g.target[e];
                double candidate = d[s] == Double.NEGATIVE_INFINITY ? d[s] : WeightMath.add(d[s], a.weight(e));
                if (longest ? candidate > d[t] : candidate < d[t]) { d[t] = candidate; if (p != null) p[t] = e; }
            }
        }
        return new Relaxed(d, p);
    }
    private static Relaxed dijkstra(WeightedAutomaton a, double[] seed, boolean reverse, boolean epsOnly, boolean track) {
        AutomatonGraph g = a.graph(); double[] d = seed.clone(); int[] p = parents(d.length, track);
        boolean[] negative = propagate(g, seed, reverse, epsOnly);
        Heap heap = new Heap(d);
        for (int s = 0; s < d.length; s++) if (!negative[s] && Double.isFinite(d[s])) heap.offer(s);
        int[] start = reverse ? g.inStart : g.outStart, edges = reverse ? g.inEdges : g.outEdges;
        while (heap.size > 0) {
            int s = heap.poll();
            for (int i = start[s]; i < start[s + 1]; i++) {
                int e = edges[i]; if (epsOnly && !g.epsilon[e]) continue;
                int t = reverse ? g.source[e] : g.target[e]; if (negative[t]) continue;
                double candidate = WeightMath.add(d[s], a.weight(e));
                if (candidate < d[t]) { d[t] = candidate; if (p != null) p[t] = e; heap.offer(t); }
            }
        }
        for (int s = 0; s < d.length; s++) if (negative[s]) d[s] = Double.NEGATIVE_INFINITY;
        return new Relaxed(d, p);
    }

    private static Relaxed bellman(WeightedAutomaton a, double[] seed, boolean reverse, boolean epsOnly, boolean track) {
        AutomatonGraph g = a.graph(); int n = seed.length;
        boolean[] negative = propagate(g, seed, reverse, epsOnly);
        // Synchronous rounds bound every finite candidate to at most V edges.
        double bound = 0x1.0p53 / Math.max(2.0, n + 1.0); boolean integral = true;
        for (double v : seed) if (Double.isFinite(v) && (v != Math.rint(v) || Math.abs(v) > bound)) integral = false;
        for (int e = 0; e < g.source.length; e++) if (!epsOnly || g.epsilon[e]) {
            double v = a.weight(e); if (v != Math.rint(v) || Math.abs(v) > bound) integral = false;
        }
        int[] p = parents(n, track);
        if (integral) {
            double[] d = seed.clone(), next = new double[n];
            for (int s = 0; s < n; s++) if (negative[s]) d[s] = Double.POSITIVE_INFINITY;
            for (int pass = 0; pass < n; pass++) {
                System.arraycopy(d, 0, next, 0, n); boolean changed = false;
                for (int e = 0; e < g.source.length; e++) {
                    if (epsOnly && !g.epsilon[e]) continue;
                    int s = reverse ? g.target[e] : g.source[e], t = reverse ? g.source[e] : g.target[e];
                    if (d[s] == Double.POSITIVE_INFINITY || negative[t]) continue;
                    double candidate = WeightMath.add(d[s], a.weight(e));
                    if (candidate < next[t]) {
                        changed = true; next[t] = candidate; if (p != null) p[t] = e;
                        if (pass == n - 1) negative[t] = true;
                    }
                }
                double[] tmp = d; d = next; next = tmp; if (!changed) break;
            }
            markNegative(g, d, negative, reverse, epsOnly);
            return new Relaxed(d, p);
        }
        BigDecimal[] d = new BigDecimal[n], weights = new BigDecimal[g.source.length];
        for (int s = 0; s < n; s++) if (!negative[s] && Double.isFinite(seed[s])) d[s] = new BigDecimal(seed[s]);
        for (int e = 0; e < weights.length; e++) if (!epsOnly || g.epsilon[e]) weights[e] = new BigDecimal(a.weight(e));
        for (int pass = 0; pass < n; pass++) {
            BigDecimal[] next = d.clone(); boolean changed = false;
            for (int e = 0; e < weights.length; e++) {
                if (weights[e] == null) continue;
                int s = reverse ? g.target[e] : g.source[e], t = reverse ? g.source[e] : g.target[e];
                if (d[s] == null || negative[t]) continue;
                BigDecimal candidate = d[s].add(weights[e]);
                if (next[t] == null || candidate.compareTo(next[t]) < 0) {
                    changed = true; next[t] = candidate; if (p != null) p[t] = e;
                    if (pass == n - 1) negative[t] = true;
                }
            }
            d = next; if (!changed) break;
        }
        double[] result = new double[n]; Arrays.fill(result, Double.POSITIVE_INFINITY);
        markNegative(g, result, negative, reverse, epsOnly);
        for (int s = 0; s < n; s++) if (!negative[s] && d[s] != null) {
            result[s] = d[s].doubleValue(); if (!Double.isFinite(result[s])) throw new ArithmeticException("Cost overflow");
        }
        return new Relaxed(result, p);
    }
    private static boolean[] propagate(AutomatonGraph g, double[] seed, boolean reverse, boolean epsOnly) {
        boolean[] neg = new boolean[seed.length];
        for (int s = 0; s < seed.length; s++) neg[s] = seed[s] == Double.NEGATIVE_INFINITY;
        markNegative(g, null, neg, reverse, epsOnly); return neg;
    }
    private static void markNegative(AutomatonGraph g, double[] d, boolean[] negative, boolean reverse, boolean epsOnly) {
        int[] queue = new int[negative.length]; int head = 0, tail = 0;
        for (int s = 0; s < negative.length; s++) if (negative[s]) queue[tail++] = s;
        int[] start = reverse ? g.inStart : g.outStart, edges = reverse ? g.inEdges : g.outEdges;
        while (head < tail) {
            int s = queue[head++]; if (d != null) d[s] = Double.NEGATIVE_INFINITY;
            for (int i = start[s]; i < start[s + 1]; i++) {
                int e = edges[i]; if (epsOnly && !g.epsilon[e]) continue;
                int t = reverse ? g.source[e] : g.target[e];
                if (!negative[t]) { negative[t] = true; queue[tail++] = t; }
            }
        }
    }

    /**
     * Indexed primitive binary heap; no boxed nodes or decrease-key duplicates.
     */
    private static final class Heap {
        final double[] d; final int[] heap, position; int size;
        Heap(double[] d) { this.d = d; heap = new int[d.length]; position = new int[d.length]; Arrays.fill(position, -1); }
        void offer(int s) {
            int i = position[s]; if (i < 0) { i = size++; heap[i] = s; position[s] = i; }
            while (i > 0) {
                int p = (i - 1) >>> 1; if (d[heap[p]] <= d[s]) break;
                heap[i] = heap[p]; position[heap[i]] = i; i = p;
            }
            heap[i] = s; position[s] = i;
        }
        int poll() {
            int result = heap[0], last = heap[--size]; position[result] = -1;
            if (size == 0) return result;
            int i = 0;
            while (2 * i + 1 < size) {
                int child = 2 * i + 1;
                if (child + 1 < size && d[heap[child + 1]] < d[heap[child]]) child++;
                if (d[last] <= d[heap[child]]) break;
                heap[i] = heap[child]; position[heap[i]] = i; i = child;
            }
            heap[i] = last; position[last] = i; return result;
        }
    }
}
