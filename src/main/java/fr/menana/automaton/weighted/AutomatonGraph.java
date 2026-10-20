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

/**
 * Immutable integer-labelled multigraph shared by scalar and vector weightings.
 * State and edge IDs are local to a graph; edge IDs follow insertion order.
 * Parallel edges are never merged. An edge either consumes one symbol or is epsilon.
 * Labels, incoming/outgoing CSR indexes and topological order are stored once.
 */
public final class AutomatonGraph {
    final int initial;
    final boolean[] accept;
    final int[] source, target, outStart, outEdges, inStart, inEdges;
    final int[] labelStart, mins, maxs;
    final boolean[] epsilon;
    final boolean hasEpsilon;
    final int[] order;
    private volatile Boolean deterministic;

    private AutomatonGraph(Builder b) {
        initial = b.initial;
        accept = new boolean[b.accept.size()];
        for (int i = 0; i < accept.length; i++) accept[i] = b.accept.get(i);
        int m = b.source.size();
        source = new int[m]; target = new int[m]; epsilon = new boolean[m];
        labelStart = new int[m + 1];
        boolean hasEps = false;
        for (int e = 0; e < m; e++) {
            source[e] = b.source.get(e); target[e] = b.target.get(e);
            IntervalSet label = b.labels.get(e);
            epsilon[e] = label == null;
            hasEps |= epsilon[e];
            labelStart[e + 1] = Math.addExact(labelStart[e], label == null ? 0 : label.intervalCount());
        }
        hasEpsilon = hasEps;
        mins = new int[labelStart[m]]; maxs = new int[mins.length];
        for (int e = 0; e < m; e++) {
            IntervalSet label = b.labels.get(e);
            for (int j = labelStart[e]; j < labelStart[e + 1]; j++) {
                mins[j] = label.getIntervalMin(j - labelStart[e]); maxs[j] = label.getIntervalMax(j - labelStart[e]);
            }
        }
        outStart = starts(source, accept.length); inStart = starts(target, accept.length);
        outEdges = edges(source, outStart); inEdges = edges(target, inStart);
        int[] degree = new int[accept.length], sorted = new int[accept.length];
        for (int s = 0; s < degree.length; s++) degree[s] = inStart[s + 1] - inStart[s];
        int head = 0, tail = 0;
        for (int s = 0; s < degree.length; s++) if (degree[s] == 0) sorted[tail++] = s;
        while (head < tail) {
            int s = sorted[head++];
            for (int i = outStart[s]; i < outStart[s + 1]; i++)
                if (--degree[target[outEdges[i]]] == 0) sorted[tail++] = target[outEdges[i]];
        }
        order = tail == accept.length ? sorted : null;
    }

    private static int[] starts(int[] states, int n) {
        int[] start = new int[n + 1];
        for (int s : states) start[s + 1]++;
        for (int s = 0; s < n; s++) start[s + 1] += start[s];
        return start;
    }

    private static int[] edges(int[] states, int[] start) {
        int[] fill = start.clone(), edges = new int[states.length];
        for (int e = 0; e < states.length; e++) edges[fill[states[e]]++] = e;
        return edges;
    }

    /**
     * @return a mutable builder; building snapshots it without invalidating it
     */
    public static Builder builder() { return new Builder(); }
    /**
     * @return number of states
     */
    public int getNbStates() { return accept.length; }
    /**
     * @return number of logical edges, including parallel and epsilon edges
     */
    public int getNbTransitions() { return source.length; }
    /**
     * @return initial state, or -1 if absent
     */
    public int getInitialState() { return initial; }
    /**
     * @param state state ID
     * @return whether it accepts
     */
    public boolean isAccept(int state) { checkState(state); return accept[state]; }
    /**
     * @param edge edge ID
     * @return origin
     */
    public int source(int edge) { return source[edge]; }
    /**
     * @param edge edge ID
     * @return destination
     */
    public int target(int edge) { return target[edge]; }
    /**
     * @param edge edge ID
     * @return whether it consumes no symbol
     */
    public boolean isEpsilon(int edge) { return epsilon[edge]; }
    /**
     * @param edge edge ID
     * @return a defensive label copy, or null for epsilon
     */
    public IntervalSet labels(int edge) {
        if (epsilon[edge]) return null;
        return IntervalSet.fromSortedIntervals(mins, maxs, labelStart[edge], labelStart[edge + 1]);
    }
    /**
     * @param edge edge ID
     * @param symbol symbol
     * @return whether this edge consumes it
     */
    public boolean accepts(int edge, int symbol) {
        int lo = labelStart[edge], hi = labelStart[edge + 1] - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (maxs[mid] < symbol) lo = mid + 1;
            else if (mins[mid] > symbol) hi = mid - 1;
            else return true;
        }
        return false;
    }
    /**
     * @param state state ID
     * @return a copy of its outgoing edge IDs
     */
    public int[] outgoingEdges(int state) {
        checkState(state); return Arrays.copyOfRange(outEdges, outStart[state], outStart[state + 1]);
    }
    /**
     * @return whether all edges, including epsilon edges, form a DAG
     */
    public boolean isAcyclic() { return order != null; }
    /**
     * @return a topological order
     * @throws IllegalArgumentException on any cycle
     */
    public int[] topologicalOrder() { requireDag(); return order.clone(); }
    void requireDag() { if (order == null) throw new IllegalArgumentException("Acyclic graph required"); }
    void checkState(int s) { if (s < 0 || s >= accept.length) throw new IndexOutOfBoundsException("State " + s); }
    int representative(int e) { return mins[labelStart[e]]; }

    /**
     * @return true exactly when there is no epsilon edge or overlapping outgoing label
     */
    public boolean isDeterministic() {
        Boolean cached = deterministic;
        if (cached != null) return cached;
        for (boolean eps : epsilon) if (eps) { deterministic = false; return false; }
        for (int s = 0; s < accept.length; s++) {
            int n = 0;
            for (int i = outStart[s]; i < outStart[s + 1]; i++) {
                int e = outEdges[i]; n = Math.addExact(n, labelStart[e + 1] - labelStart[e]);
            }
            long[] sorted = new long[n];
            int at = 0;
            for (int i = outStart[s]; i < outStart[s + 1]; i++) {
                int e = outEdges[i];
                for (int j = labelStart[e]; j < labelStart[e + 1]; j++)
                    sorted[at++] = ((long) mins[j] << 32) | (j & 0xffffffffL);
            }
            Arrays.sort(sorted);
            for (int j = 1; j < n; j++) if (mins[(int) sorted[j]] <= maxs[(int) sorted[j - 1]]) {
                deterministic = false; return false;
            }
        }
        deterministic = true; return true;
    }

    /**
     * @return states both reachable from the initial state and able to reach a final
     */
    public boolean[] usefulStates() {
        boolean[] reached = new boolean[accept.length], useful = new boolean[accept.length];
        int[] queue = new int[accept.length]; int head = 0, tail = 0;
        if (initial >= 0) { reached[initial] = true; queue[tail++] = initial; }
        while (head < tail) {
            int s = queue[head++];
            for (int i = outStart[s]; i < outStart[s + 1]; i++) {
                int t = target[outEdges[i]];
                if (!reached[t]) { reached[t] = true; queue[tail++] = t; }
            }
        }
        head = tail = 0;
        for (int s = 0; s < accept.length; s++) if (accept[s] && reached[s]) { useful[s] = true; queue[tail++] = s; }
        while (head < tail) {
            int s = queue[head++];
            for (int i = inStart[s]; i < inStart[s + 1]; i++) {
                int p = source[inEdges[i]];
                if (reached[p] && !useful[p]) { useful[p] = true; queue[tail++] = p; }
            }
        }
        return useful;
    }

    /**
     * Imports a snapshot without determinizing or changing the source.
     * Mixed 3.0 transitions are split into consuming and epsilon edges.
     * @param automaton source
     * @return immutable graph
     */
    public static AutomatonGraph from(Automaton automaton) { return importAutomaton(automaton).graph(); }

    /**
     * @param automaton source
     * @return graph and explicit mapping of imported edge modes
     */
    public static ImportResult importAutomaton(Automaton automaton) {
        Objects.requireNonNull(automaton);
        Builder b = builder(); IdentityHashMap<State, Integer> ids = new IdentityHashMap<>();
        List<State> states = automaton.getStates();
        for (State s : states) {
            if (s == null || ids.containsKey(s)) throw new IllegalArgumentException("Invalid state list");
            int id = b.addState(); ids.put(s, id); b.setAccept(id, s.isAccept());
        }
        State init = automaton.getInitial();
        if (init != null) {
            if (!ids.containsKey(init)) throw new IllegalArgumentException("Foreign initial state");
            b.setInitial(ids.get(init));
        }
        List<Transition> originals = new ArrayList<>();
        for (State s : states) for (Map.Entry<State, Transition> entry : s.getTransitions().entrySet()) {
            Transition t = entry.getValue();
            if (t == null || t.getOrig() != s || t.getDest() != entry.getKey() || !ids.containsKey(t.getDest()))
                throw new IllegalArgumentException("Invalid transition map");
            if (t.hasValues()) { b.addTransition(ids.get(s), ids.get(t.getDest()), t.getValues()); originals.add(t); }
            if (t.hasEpsilon()) { b.addEpsilonTransition(ids.get(s), ids.get(t.getDest())); originals.add(t); }
        }
        return new ImportResult(b.build(), originals.toArray(Transition[]::new));
    }

    /**
     * Snapshot with a mapping to source transitions; source references are metadata only.
     */
    public static final class ImportResult {
        private final AutomatonGraph graph;
        private final Transition[] originals;
        private ImportResult(AutomatonGraph graph, Transition[] originals) { this.graph = graph; this.originals = originals; }
        /**
         * @return imported graph
         */
        public AutomatonGraph graph() { return graph; }
        /**
         * @param edge imported ID
         * @return source transition, possibly shared by two modes
         */
        public Transition originalTransition(int edge) { return originals[edge]; }
    }

    /**
     * Mutable builder. Each addition creates a distinct logical edge.
     */
    public static final class Builder {
        private int initial = -1;
        private final List<Boolean> accept = new ArrayList<>();
        private final List<Integer> source = new ArrayList<>(), target = new ArrayList<>();
        private final List<IntervalSet> labels = new ArrayList<>();
        private Builder() { }
        /**
         * @return new state ID
         */
        public int addState() { accept.add(false); return accept.size() - 1; }
        /**
         * @param state new unique initial
         */
        public void setInitial(int state) { check(state); initial = state; }
        /**
         * @param state accepting state
         */
        public void setAccept(int state) { setAccept(state, true); }
        /**
         * @param state state
         * @param value acceptance
         */
        public void setAccept(int state, boolean value) { check(state); accept.set(state, value); }
        /**
         * @param from origin
         * @param to destination
         * @param label nonempty label
         * @return edge ID
         */
        public int addTransition(int from, int to, IntervalSet label) {
            Objects.requireNonNull(label); if (label.isEmpty()) throw new IllegalArgumentException("Empty label");
            return add(from, to, label.clone());
        }
        /**
         * @param from origin
         * @param to destination
         * @param label interval
         * @return edge ID
         */
        public int addTransition(int from, int to, Interval label) { return addTransition(from, to, IntervalSet.fromInterval(label)); }
        /**
         * @param from origin
         * @param to destination
         * @param symbol consumed symbol
         * @return edge ID
         */
        public int addTransition(int from, int to, int symbol) { return addTransition(from, to, IntervalSet.fromIntArray(symbol)); }
        /**
         * @param from origin
         * @param to destination
         * @return epsilon edge ID
         */
        public int addEpsilonTransition(int from, int to) { return add(from, to, null); }
        private int add(int from, int to, IntervalSet label) {
            check(from); check(to); int id = source.size(); source.add(from); target.add(to); labels.add(label); return id;
        }
        private void check(int state) { if (state < 0 || state >= accept.size()) throw new IndexOutOfBoundsException("State " + state); }
        /**
         * @return independent immutable snapshot
         */
        public AutomatonGraph build() { return new AutomatonGraph(this); }
    }
}
