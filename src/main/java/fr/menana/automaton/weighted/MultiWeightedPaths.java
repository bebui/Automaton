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

import java.util.*;
import java.math.BigDecimal;

/**
 * DAG lexicographic and exact Pareto label propagation. All dimensions are minimized.
 */
public final class MultiWeightedPaths {
    private MultiWeightedPaths() { }
    private static final class Label {
        final double[] costs; final BigDecimal[] exact; final Label parent; final int edge, state;
        Label(double[] costs, BigDecimal[] exact, Label parent, int edge, int state) { this.costs = costs; this.exact = exact; this.parent = parent; this.edge = edge; this.state = state; }
    }
    private static boolean exactDoubleSums(MultiWeightedAutomaton a) {
        WeightMath.Exactness certificate = new WeightMath.Exactness();
        for (int d = 0; d < a.dimensions(); d++) {
            certificate.include(a.initialWeight(d));
            for (int s = 0; s < a.graph().getNbStates(); s++) if (a.graph().accept[s]) certificate.include(a.finalWeight(s, d));
            for (int e = 0; e < a.graph().source.length; e++) certificate.include(a.weight(e, d));
        }
        return certificate.safe(a.graph().getNbStates() + 1.0);
    }
    private static Label root(MultiWeightedAutomaton a) {
        double[] costs = a.initialVector(); BigDecimal[] exact = exactDoubleSums(a) ? null : new BigDecimal[costs.length];
        if (exact != null) for (int d = 0; d < costs.length; d++) exact[d] = new BigDecimal(costs[d]);
        return new Label(costs, exact, null, -1, a.graph().initial);
    }
    private static Label extend(MultiWeightedAutomaton a, Label prefix, int edge) {
        double[] result = new double[prefix.costs.length]; BigDecimal[] exact = prefix.exact == null ? null : new BigDecimal[result.length];
        for (int d = 0; d < result.length; d++) {
            if (exact == null) result[d] = WeightMath.add(prefix.costs[d], a.weight(edge, d));
            else { exact[d] = prefix.exact[d].add(new BigDecimal(a.weight(edge, d))); result[d] = rounded(exact[d]); }
        }
        return new Label(result, exact, prefix, edge, a.graph().target[edge]);
    }
    private static Label terminal(MultiWeightedAutomaton a, Label label) {
        double[] v = new double[label.costs.length]; BigDecimal[] exact = label.exact == null ? null : new BigDecimal[v.length];
        for (int d = 0; d < v.length; d++) {
            if (exact == null) v[d] = WeightMath.add(label.costs[d], a.finalWeight(label.state, d));
            else { exact[d] = label.exact[d].add(new BigDecimal(a.finalWeight(label.state, d))); v[d] = rounded(exact[d]); }
        }
        return new Label(v, exact, label.parent, label.edge, label.state);
    }
    private static double rounded(BigDecimal value) {
        double result = value.doubleValue(); if (!Double.isFinite(result)) throw new ArithmeticException("Cost overflow"); return result == 0 ? 0 : result;
    }
    private static MultiWeightedPath path(MultiWeightedAutomaton a, Label label, double[] total) {
        int size = 0, length = 0; AutomatonGraph g = a.graph();
        for (Label p = label; p.parent != null; p = p.parent) { size++; if (!g.epsilon[p.edge]) length++; }
        int[] edges = new int[size], word = new int[length];
        for (Label p = label; p.parent != null; p = p.parent) { edges[--size] = p.edge; if (!g.epsilon[p.edge]) word[--length] = g.representative(p.edge); }
        return new MultiWeightedPath(new WeightedPath(g, g.initial, edges, word, 0), total, label.exact);
    }
    /**
     * O(d(V+E)) arithmetic operations with one best label per state.
     * @param a vector weighting
     * @param priority permutation, or empty for natural order
     * @return accepting witness, or null; cyclic graphs are rejected
     */
    public static MultiWeightedPath lexicographic(MultiWeightedAutomaton a, int... priority) {
        AutomatonGraph g = a.graph(); g.requireDag(); int d = a.dimensions();
        int[] order = priority.length == 0 ? new int[d] : priority.clone();
        if (priority.length == 0) for (int i = 0; i < d; i++) order[i] = i;
        if (order.length != d) throw new IllegalArgumentException("A permutation of every dimension is required");
        boolean[] seen = new boolean[d]; for (int i : order) { if (i < 0 || i >= d || seen[i]) throw new IllegalArgumentException("Invalid dimension permutation"); seen[i] = true; }
        if (g.initial < 0) return null;
        Label[] best = new Label[g.getNbStates()]; best[g.initial] = root(a);
        for (int s : g.order) if (best[s] != null) for (int i = g.outStart[s]; i < g.outStart[s + 1]; i++) {
            int e = g.outEdges[i], t = g.target[e]; Label v = extend(a, best[s], e);
            if (best[t] == null || compare(v, best[t], order) < 0) best[t] = v;
        }
        Label selected = null;
        for (int s = 0; s < best.length; s++) if (g.accept[s] && best[s] != null) {
            Label v = terminal(a, best[s]); if (selected == null || compare(v, selected, order) < 0) selected = v;
        }
        return selected == null ? null : path(a, selected, selected.costs);
    }
    private static int compare(Label x, Label y, int[] order) { for (int d : order) { int c = x.exact == null ? Double.compare(x.costs[d], y.costs[d]) : x.exact[d].compareTo(y.exact[d]); if (c != 0) return c; } return 0; }
    /**
     * @param x vector
     * @param y vector
     * @return strict componentwise minimization dominance
     */
    public static boolean dominates(double[] x, double[] y) {
        if (x.length != y.length || x.length == 0) throw new IllegalArgumentException("Equal positive dimensions required");
        for (int d = 0; d < x.length; d++) { WeightMath.finite(x[d]); WeightMath.finite(y[d]); }
        boolean strict = false;
        for (int d = 0; d < x.length; d++) { if (x[d] > y[d]) return false; strict |= x[d] < y[d]; }
        return strict;
    }
    private static boolean insert(List<Label> front, Label value, boolean allPaths) {
        for (Label old : front) { int r = relation(old, value); if (r == -1 || (!allPaths && r == 0)) return false; }
        front.removeIf(old -> relation(value, old) == -1); front.add(value); return true;
    }
    private static int relation(Label x, Label y) {
        boolean less = false, more = false;
        for (int d = 0; d < x.costs.length; d++) {
            int c = x.exact == null ? Double.compare(x.costs[d], y.costs[d]) : x.exact[d].compareTo(y.exact[d]);
            less |= c < 0; more |= c > 0; if (less && more) return 2;
        }
        return less ? -1 : more ? 1 : 0;
    }
    /**
     * Exact topological label propagation. Worst-case label count and output are exponential.
     * Lists give O(d*A*M) dominance work for A generated candidates and maximum front M.
     * @param a weighting
     * @param allPaths explicitly retain all equal-vector paths
     * @return complete front; all cycles, including epsilon cycles, are rejected
     */
    public static ParetoResult pareto(MultiWeightedAutomaton a, boolean allPaths) {
        AutomatonGraph g = a.graph(); g.requireDag();
        if (g.initial < 0) return new ParetoResult(List.of(), allPaths);
        List<List<Label>> fronts = new ArrayList<>(); for (int s = 0; s < g.getNbStates(); s++) fronts.add(new ArrayList<>());
        fronts.get(g.initial).add(root(a));
        for (int s : g.order) for (Label label : fronts.get(s)) for (int i = g.outStart[s]; i < g.outStart[s + 1]; i++) {
            int e = g.outEdges[i]; insert(fronts.get(g.target[e]), extend(a, label, e), allPaths);
        }
        List<Label> accepted = new ArrayList<>();
        for (int s = 0; s < g.getNbStates(); s++) if (g.accept[s]) for (Label label : fronts.get(s)) {
            insert(accepted, terminal(a, label), allPaths);
        }
        List<MultiWeightedPath> result = new ArrayList<>(); for (Label label : accepted) result.add(path(a, label, label.costs));
        return new ParetoResult(result, allPaths);
    }
}
