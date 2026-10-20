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
 * Cost-preserving transformations with explicit family restrictions.
 * Includes fast bounded-integer DAG transforms and exact transforms on cyclic
 * graphs. General residual determinization has an explicit state budget, since
 * a finite deterministic equivalent need not exist.
 */
public final class WeightedTransformations {
    private WeightedTransformations() { }

    /**
     * Eliminates epsilon by shortest epsilon-prefix costs and new terminal costs.
     * Preserves minimum word costs, not all paths or vector relations. A reachable
     * negative epsilon cycle is rejected, since finite edge costs cannot encode it.
     * Worst case O(V^2 Eepsilon + VE) time, O(V+E) scratch plus possibly quadratic output.
     * @param input operand
     * @return epsilon-free weighting
     */
    public static WeightedAutomaton removeEpsilon(WeightedAutomaton input) {
        if (!input.hasEpsilon) return input;
        WeightedAutomaton a = input.trim(); AutomatonGraph g = a.graph(); int n = g.getNbStates();
        if (g.initial < 0) return a;
        WeightedAutomaton.Builder eps = WeightedAutomaton.builder();
        for (int s = 0; s < n; s++) eps.addState();
        for (int e = 0; e < g.source.length; e++) if (g.epsilon[e]) eps.addEpsilonTransition(g.source[e], g.target[e], a.weight(e));
        WeightedAutomaton epsilon = eps.build(); WeightedAutomaton.Builder out = WeightedAutomaton.builder();
        for (int s = 0; s < n; s++) out.addState(); out.setInitial(g.initial, a.initialWeight());
        for (int s = 0; s < n; s++) {
            double[] distances = WeightedPaths.shortestPaths(epsilon, s).distances(); double last = Double.POSITIVE_INFINITY;
            for (int t = 0; t < n; t++) {
                if (distances[t] == Double.NEGATIVE_INFINITY) throw new IllegalArgumentException("Negative epsilon cycle cannot be eliminated into finite costs");
                if (g.accept[t] && Double.isFinite(distances[t])) last = Math.min(last, WeightMath.add(distances[t], a.finalWeight(t)));
            }
            if (Double.isFinite(last)) out.setAccept(s, last);
            for (int e = 0; e < g.source.length; e++) if (!g.epsilon[e] && Double.isFinite(distances[g.source[e]]))
                out.addTransition(s, g.target[e], g.labels(e), WeightMath.add(distances[g.source[e]], a.weight(e)));
        }
        return out.build();
    }

    // A deliberately conservative sufficient bound: all normalizations, path sums
    // and potential differences in these finite-depth constructions stay exact.
    private static void certifiedIntegerDag(WeightedAutomaton a) {
        a.graph().requireDag();
        double n = a.graph().getNbStates() + 1.0, limit = 0x1.0p48 / (n * n);
        requireInteger(a.initialWeight(), limit);
        for (int s = 0; s < a.graph().getNbStates(); s++) requireInteger(a.finalWeight(s), limit);
        for (int e = 0; e < a.graph().getNbTransitions(); e++) requireInteger(a.weight(e), limit);
    }
    private static void requireInteger(double value, double bound) {
        if (value != Math.rint(value) || Math.abs(value) > bound)
            throw new IllegalArgumentException("This transformation requires bounded integer costs for exact normalization");
    }
    private static final class Residual {
        final double[] values; final int hash;
        Residual(double[] values) { this.values = values; hash = Arrays.hashCode(values); }
        public int hashCode() { return hash; }
        public boolean equals(Object other) { return other instanceof Residual r && Arrays.equals(values, r.values); }
    }
    /**
     * @param a operand
     * @return deterministic equivalent in the supported certified family
     */
    public static WeightedAutomaton determinizeDag(WeightedAutomaton a) { return determinizeDag(a, Integer.MAX_VALUE); }
    /**
     * Residual subset construction on integer-weighted DAGs; output may be exponential.
     * Epsilon removal is performed after certification. An already deterministic
     * input is returned unchanged, including when cyclic or nonintegral.
     * @param input operand
     * @param maxStates explicit output-state budget
     * @return deterministic cost equivalent
     * @throws IllegalStateException on budget exhaustion (not a proof of nondeterminizability)
     */
    public static WeightedAutomaton determinizeDag(WeightedAutomaton input, int maxStates) {
        if (maxStates <= 0) throw new IllegalArgumentException("Positive state budget required");
        if (input.graph().isDeterministic()) return input;
        certifiedIntegerDag(input);
        WeightedAutomaton a = removeEpsilon(input); AutomatonGraph g = a.graph();
        WeightedAutomaton.Builder out = WeightedAutomaton.builder(); if (g.initial < 0) return out.build();
        List<IntervalSet> sets = new ArrayList<>(); for (int e = 0; e < g.source.length; e++) sets.add(g.labels(e));
        Alphabet alphabet = Alphabet.of(sets); int n = g.getNbStates();
        double[] initial = new double[n]; Arrays.fill(initial, Double.POSITIVE_INFINITY); initial[g.initial] = 0;
        List<Residual> queue = new ArrayList<>(); Map<Residual, Integer> ids = new HashMap<>();
        Residual root = new Residual(initial); queue.add(root); ids.put(root, out.addState()); out.setInitial(0, a.initialWeight());
        for (int at = 0; at < queue.size(); at++) {
            Residual current = queue.get(at); double terminal = Double.POSITIVE_INFINITY;
            for (int s = 0; s < n; s++) if (Double.isFinite(current.values[s]) && g.accept[s]) terminal = Math.min(terminal, WeightMath.add(current.values[s], a.finalWeight(s)));
            if (Double.isFinite(terminal)) out.setAccept(at, terminal);
            for (int c = 0; c < alphabet.size(); c++) {
                double[] next = new double[n]; Arrays.fill(next, Double.POSITIVE_INFINITY);
                for (int e = 0; e < g.source.length; e++) if (Double.isFinite(current.values[g.source[e]]) && g.accepts(e, alphabet.min(c)))
                    next[g.target[e]] = Math.min(next[g.target[e]], WeightMath.add(current.values[g.source[e]], a.weight(e)));
                double common = Double.POSITIVE_INFINITY; for (double value : next) common = Math.min(common, value);
                if (!Double.isFinite(common)) continue;
                for (int s = 0; s < n; s++) if (Double.isFinite(next[s])) next[s] = WeightMath.add(next[s], -common);
                Residual target = new Residual(next); Integer id = ids.get(target);
                if (id == null) {
                    if (queue.size() == maxStates) throw new IllegalStateException("Determinization state budget exhausted; no complete result");
                    id = out.addState(); ids.put(target, id); queue.add(target);
                }
                out.addTransition(at, id, new Interval(alphabet.min(c), alphabet.max(c)), common);
            }
        }
        return WeightedOperations.simplify(out.build());
    }
    private record Segment(int min, int max, int target, double weight) { }
    private record Signature(boolean accept, double terminal, List<Segment> edges) { }
    /**
     * Pushes exact integer suffix potentials then identifies equal normalized
     * right-cost functions bottom-up. On deterministic DAGs this gives a minimal
     * weighted deterministic automaton; it never applies boolean equivalence.
     * Sorting dominates O(I log I), with O(V+E+I) structural storage.
     * @param input deterministic integer-weighted DAG
     * @return minimal weighted DAG
     */
    public static WeightedAutomaton minimizeDag(WeightedAutomaton input) {
        if (!input.graph().isDeterministic()) throw new IllegalArgumentException("Deterministic weighting required");
        certifiedIntegerDag(input);
        WeightedAutomaton a = input.trim(); AutomatonGraph g = a.graph(); if (g.initial < 0) return a;
        double[] potential = WeightedPaths.backwardDistances(a); int[] group = new int[g.getNbStates()];
        WeightedAutomaton.Builder out = WeightedAutomaton.builder(); Map<Signature, Integer> groups = new HashMap<>();
        for (int k = g.order.length - 1; k >= 0; k--) {
            int s = g.order[k]; List<Segment> segments = new ArrayList<>();
            for (int i = g.outStart[s]; i < g.outStart[s + 1]; i++) {
                int e = g.outEdges[i]; double w = WeightMath.add(WeightMath.add(a.weight(e), potential[g.target[e]]), -potential[s]);
                for (int j = g.labelStart[e]; j < g.labelStart[e + 1]; j++) segments.add(new Segment(g.mins[j], g.maxs[j], group[g.target[e]], w));
            }
            segments.sort(Comparator.comparingInt(Segment::min)); List<Segment> merged = new ArrayList<>();
            for (Segment edge : segments) {
                if (!merged.isEmpty()) {
                    Segment last = merged.get(merged.size() - 1);
                    if ((long) last.max + 1 == edge.min && last.target == edge.target && last.weight == edge.weight) {
                        merged.set(merged.size() - 1, new Segment(last.min, edge.max, edge.target, edge.weight)); continue;
                    }
                }
                merged.add(edge);
            }
            double terminal = g.accept[s] ? WeightMath.add(a.finalWeight(s), -potential[s]) : 0;
            Signature signature = new Signature(g.accept[s], terminal, List.copyOf(merged)); Integer id = groups.get(signature);
            if (id == null) {
                id = out.addState(); groups.put(signature, id); if (g.accept[s]) out.setAccept(id, terminal);
                for (Segment e : merged) out.addTransition(id, e.target, new Interval(e.min, e.max), e.weight);
            }
            group[s] = id;
        }
        out.setInitial(group[g.initial], WeightMath.add(a.initialWeight(), potential[g.initial]));
        return out.build();
    }

    /** Default output-state limit for residual determinization. */
    public static final int DEFAULT_MAX_STATES = 10_000;

    /**
     * Exact residual construction, with a finite resource budget. Cyclic inputs
     * are supported when the construction finishes within that budget. Exhaustion
     * is not a proof of nondeterminizability. All arithmetic uses the exact binary
     * values of input doubles; every emitted weight must itself be exactly
     * representable as a finite double. Epsilon cycles of negative cost are rejected.
     * @param input scalar weighting
     * @param maxStates positive output-state budget
     * @return complete deterministic equivalent; never a partial result
     * @throws IllegalStateException if the state budget is exhausted
     * @throws ArithmeticException if a transformed weight cannot be represented exactly
     */
    public static WeightedAutomaton determinize(WeightedAutomaton input, int maxStates) {
        if (maxStates <= 0) throw new IllegalArgumentException("Positive state budget required");
        if (input.graph().isDeterministic()) return input;
        // Retain the existing primitive implementation in its certified domain.
        if (input.graph().isAcyclic()) {
            boolean certified = true;
            try { certifiedIntegerDag(input); } catch (IllegalArgumentException ex) { certified = false; }
            if (certified) return determinizeDag(input, maxStates);
        }
        WeightedAutomaton a = input.trim(); AutomatonGraph g = a.graph();
        WeightedAutomaton.Builder out = WeightedAutomaton.builder();
        if (g.initial < 0) return out.build();
        ExactCosts costs = new ExactCosts(a); int n = g.accept.length;
        List<IntervalSet> labels = new ArrayList<>();
        for (int e = 0; e < g.source.length; e++) if (!g.epsilon[e]) labels.add(g.labels(e));
        Alphabet alphabet = Alphabet.of(labels);
        BigDecimal[] initial = new BigDecimal[n]; initial[g.initial] = BigDecimal.ZERO;
        costs.closeEpsilon(initial); BigDecimal common = normalize(initial);
        List<ExactResidual> queue = new ArrayList<>(); Map<ExactResidual, Integer> ids = new HashMap<>();
        ExactResidual root = new ExactResidual(initial); queue.add(root); ids.put(root, out.addState());
        out.setInitial(0, ExactCosts.representable(costs.initial.add(common)));
        for (int at = 0; at < queue.size(); at++) {
            BigDecimal[] current = queue.get(at).values; BigDecimal terminal = null;
            for (int s = 0; s < n; s++) if (current[s] != null && costs.finals[s] != null) {
                BigDecimal value = current[s].add(costs.finals[s]);
                if (ExactCosts.improves(value, terminal)) terminal = value;
            }
            if (terminal != null) out.setAccept(at, ExactCosts.representable(terminal));
            for (int c = 0; c < alphabet.size(); c++) {
                BigDecimal[] next = new BigDecimal[n];
                for (int e = 0; e < g.source.length; e++) if (current[g.source[e]] != null && g.accepts(e, alphabet.min(c))) {
                    BigDecimal value = current[g.source[e]].add(costs.edges[e]); int t = g.target[e];
                    if (ExactCosts.improves(value, next[t])) next[t] = value;
                }
                costs.closeEpsilon(next); common = normalize(next); if (common == null) continue;
                ExactResidual residual = new ExactResidual(next); Integer id = ids.get(residual);
                if (id == null) {
                    if (queue.size() == maxStates) throw new IllegalStateException("Determinization state budget exhausted; no complete result");
                    id = out.addState(); ids.put(residual, id); queue.add(residual);
                }
                out.addTransition(at, id, new Interval(alphabet.min(c), alphabet.max(c)), ExactCosts.representable(common));
            }
        }
        return WeightedOperations.simplify(out.build());
    }
    /**
     * @param input scalar weighting
     * @return exact complete determinization within the default output-state budget
     */
    public static WeightedAutomaton determinize(WeightedAutomaton input) {
        return determinize(input, DEFAULT_MAX_STATES);
    }
    private static BigDecimal normalize(BigDecimal[] values) {
        BigDecimal common = null;
        for (BigDecimal value : values) if (value != null && ExactCosts.improves(value, common)) common = value;
        if (common != null) for (int s = 0; s < values.length; s++)
            if (values[s] != null) values[s] = ExactCosts.canonical(values[s].subtract(common));
        return common;
    }
    private static final class ExactResidual {
        final BigDecimal[] values; final int hash;
        ExactResidual(BigDecimal[] values) { this.values = values; hash = Arrays.hashCode(values); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) { return other instanceof ExactResidual r && Arrays.equals(values, r.values); }
    }
    private record ExactSegment(int min, int max, int target, BigDecimal weight) { }
    private record ExactSignature(BigDecimal terminal, List<ExactSegment> edges) { }

    /**
     * Minimizes a deterministic epsilon-free weighting, including cyclic inputs
     * with negative cycles. Potentials are costs of canonical shortest-length,
     * then lexicographically smallest accepted suffixes, not minimum-cost walks.
     * Exact normalized signatures are refined to a fixed point. No approximate
     * equivalence is used. Emitted double weights must be exactly representable.
     * Worst-case O(V * (V + E + I log I)) signature work, O(V+E+I) live storage,
     * where I counts label intervals; exact arithmetic cost is additional.
     * @param input deterministic weighting
     * @return minimal deterministic weighting preserving exact word-cost sums
     * @throws ArithmeticException if an output weight is not exactly representable
     */
    public static WeightedAutomaton minimize(WeightedAutomaton input) {
        if (!input.graph().isDeterministic()) throw new IllegalArgumentException("Deterministic weighting required");
        if (input.graph().isAcyclic()) {
            boolean certified = true;
            try { certifiedIntegerDag(input); } catch (IllegalArgumentException ex) { certified = false; }
            if (certified) return minimizeDag(input);
        }
        WeightedAutomaton a = input.trim(); AutomatonGraph g = a.graph();
        if (g.initial < 0) return a;
        ExactCosts costs = new ExactCosts(a); int n = g.accept.length;
        int[] depth = new int[n], queue = new int[n]; Arrays.fill(depth, -1);
        int head = 0, tail = 0;
        for (int s = 0; s < n; s++) if (g.accept[s]) { depth[s] = 0; queue[tail++] = s; }
        while (head < tail) {
            int t = queue[head++];
            for (int i = g.inStart[t]; i < g.inStart[t + 1]; i++) {
                int s = g.source[g.inEdges[i]];
                if (depth[s] < 0) { depth[s] = depth[t] + 1; queue[tail++] = s; }
            }
        }
        BigDecimal[] potential = new BigDecimal[n];
        for (int i = 0; i < tail; i++) {
            int s = queue[i];
            if (g.accept[s]) { potential[s] = costs.finals[s]; continue; }
            int chosen = -1;
            for (int j = g.outStart[s]; j < g.outStart[s + 1]; j++) {
                int e = g.outEdges[j];
                if (depth[g.target[e]] == depth[s] - 1 && (chosen < 0 || g.representative(e) < g.representative(chosen))) chosen = e;
            }
            potential[s] = costs.edges[chosen].add(potential[g.target[chosen]]);
        }
        BigDecimal[] normalized = new BigDecimal[costs.edges.length];
        BigDecimal[] terminal = new BigDecimal[n];
        for (int e = 0; e < normalized.length; e++)
            normalized[e] = ExactCosts.canonical(costs.edges[e].add(potential[g.target[e]]).subtract(potential[g.source[e]]));
        for (int s = 0; s < n; s++) if (g.accept[s]) terminal[s] = ExactCosts.canonical(costs.finals[s].subtract(potential[s]));
        int[] groups = new int[n]; int count;
        while (true) {
            Map<ExactSignature, Integer> ids = new HashMap<>(); int[] next = new int[n];
            for (int s = 0; s < n; s++) {
                ExactSignature signature = exactSignature(g, s, groups, normalized, terminal[s]);
                Integer id = ids.get(signature);
                if (id == null) { id = ids.size(); ids.put(signature, id); }
                next[s] = id;
            }
            count = ids.size(); boolean stable = Arrays.equals(groups, next); groups = next;
            if (stable) break;
        }
        WeightedAutomaton.Builder out = WeightedAutomaton.builder();
        for (int i = 0; i < count; i++) out.addState();
        out.setInitial(groups[g.initial], ExactCosts.representable(costs.initial.add(potential[g.initial])));
        boolean[] emitted = new boolean[count];
        for (int s = 0; s < n; s++) if (!emitted[groups[s]]) {
            int id = groups[s]; emitted[id] = true;
            ExactSignature signature = exactSignature(g, s, groups, normalized, terminal[s]);
            if (signature.terminal != null) out.setAccept(id, ExactCosts.representable(signature.terminal));
            for (ExactSegment edge : signature.edges)
                out.addTransition(id, edge.target, new Interval(edge.min, edge.max), ExactCosts.representable(edge.weight));
        }
        return out.build();
    }
    private static ExactSignature exactSignature(AutomatonGraph g, int s, int[] groups, BigDecimal[] weights, BigDecimal terminal) {
        List<ExactSegment> segments = new ArrayList<>();
        for (int i = g.outStart[s]; i < g.outStart[s + 1]; i++) {
            int e = g.outEdges[i];
            for (int j = g.labelStart[e]; j < g.labelStart[e + 1]; j++)
                segments.add(new ExactSegment(g.mins[j], g.maxs[j], groups[g.target[e]], weights[e]));
        }
        segments.sort(Comparator.comparingInt(ExactSegment::min)); List<ExactSegment> merged = new ArrayList<>();
        for (ExactSegment edge : segments) {
            if (!merged.isEmpty()) {
                ExactSegment last = merged.get(merged.size() - 1);
                if ((long) last.max + 1 == edge.min && last.target == edge.target && last.weight.equals(edge.weight)) {
                    merged.set(merged.size() - 1, new ExactSegment(last.min, edge.max, edge.target, edge.weight)); continue;
                }
            }
            merged.add(edge);
        }
        return new ExactSignature(terminal, List.copyOf(merged));
    }
}
