/*
 * Automaton
 * Copyright (c) 2025, Julien Menana, All rights reserved.
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
import java.util.List;

/**
 * An immutable deterministic automaton compiled into arrays, to read many words quickly. <br>
 * States are numbered from 0 to {@link #getNbStates()} - 1. The outgoing transitions of each state are stored as
 * sorted disjoint intervals, so that reading a symbol costs a binary search among the transitions of the current state.
 * Instances can be shared between threads.
 * <pre>
 * CompiledDfa dfa = Automaton.dfaFromString("(1|2)*12").compile();
 * dfa.run(2, 1, 2);   // true
 * </pre>
 * @since 2.0.0
 */
public final class CompiledDfa {

    /**
     * Value returned by {@link #step(int, int)} when no transition can be taken
     */
    public static final int DEAD = -1;

    private final int initial;
    private final boolean[] accept;

    /**
     * The transitions of state s are mins[start[s] .. start[s+1]-1], sorted by lower bound
     */
    private final int[] start;
    private final int[] mins;
    private final int[] maxs;
    private final int[] dests;

    private CompiledDfa(int initial, boolean[] accept, int[] start, int[] mins, int[] maxs, int[] dests) {
        this.initial = initial;
        this.accept = accept;
        this.start = start;
        this.mins = mins;
        this.maxs = maxs;
        this.dests = dests;
    }

    /**
     * Compiles an automaton, determinizing it first if needed. <br>
     * The compiled automaton is not minimized : call {@link Automaton#minimize()} first to get the smallest one
     * @param automaton the automaton to compile
     * @return the compiled automaton
     */
    public static CompiledDfa of(Automaton automaton) {
        Automaton dfa = automaton.isDeterministic() ? automaton : automaton.determinize();
        List<State> states = dfa.getStates();
        int n = states.size();
        if (dfa.getInitial() == null)
            return new CompiledDfa(DEAD, new boolean[0], new int[1], new int[0], new int[0], new int[0]);
        boolean[] accept = new boolean[n];
        int[] start = new int[n + 1];
        for (State s : states) {
            accept[s.index] = s.accept;
            int nb = 0;
            for (Transition tr : s.transitions.values())
                if (tr.values != null)
                    nb += tr.values.intervalCount();
            start[s.index + 1] = nb;
        }
        for (int i = 0; i < n; i++)
            start[i + 1] += start[i];
        int[] mins = new int[start[n]];
        int[] maxs = new int[start[n]];
        int[] dests = new int[start[n]];
        long[] keys = new long[0];
        for (State s : states) {
            int nb = start[s.index + 1] - start[s.index];
            if (keys.length < nb)
                keys = new long[nb];
            // Sorts the intervals of the state by lower bound, keeping the position of their destination
            int k = 0;
            int[] ownerMin = new int[nb], ownerMax = new int[nb], ownerDest = new int[nb];
            for (Transition tr : s.transitions.values()) {
                if (tr.values == null)
                    continue;
                for (int i = 0; i < tr.values.intervalCount(); i++) {
                    ownerMin[k] = tr.values.minAt(i);
                    ownerMax[k] = tr.values.maxAt(i);
                    ownerDest[k] = tr.dest.index;
                    keys[k] = ((long) tr.values.minAt(i) << 32) | k;
                    k++;
                }
            }
            Arrays.sort(keys, 0, nb);
            for (int i = 0; i < nb; i++) {
                int from = (int) keys[i];
                mins[start[s.index] + i] = ownerMin[from];
                maxs[start[s.index] + i] = ownerMax[from];
                dests[start[s.index] + i] = ownerDest[from];
            }
        }
        return new CompiledDfa(dfa.getInitial().index, accept, start, mins, maxs, dests);
    }

    /**
     * Returns the initial state
     * @return the initial state, or {@link #DEAD} if the automaton has no initial state
     */
    public int getInitialState() {
        return initial;
    }

    /**
     * Returns the number of states
     * @return the number of states
     */
    public int getNbStates() {
        return accept.length;
    }

    /**
     * Checks if a state is accepting
     * @param state a state, or {@link #DEAD}
     * @return <code>true</code> if and only if the state is accepting
     */
    public boolean isAccept(int state) {
        return state != DEAD && accept[state];
    }

    /**
     * Returns the state reached from a state by reading a symbol
     * @param state the current state, or {@link #DEAD}
     * @param symbol the symbol to read
     * @return the reached state, or {@link #DEAD} if there is no transition
     */
    public int step(int state, int symbol) {
        if (state == DEAD)
            return DEAD;
        int lo = start[state], hi = start[state + 1] - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (maxs[mid] < symbol) lo = mid + 1;
            else if (mins[mid] > symbol) hi = mid - 1;
            else return dests[mid];
        }
        return DEAD;
    }

    /**
     * Checks if a word is accepted
     * @param word the word
     * @return <code>true</code> if and only if the word belongs to the language
     */
    public boolean run(int... word) {
        return run(word, 0, word.length);
    }

    /**
     * Checks if a part of an array is an accepted word
     * @param text the array
     * @param from the index of the first symbol of the word
     * @param to the index following the last symbol of the word
     * @return <code>true</code> if and only if text[from .. to-1] belongs to the language
     */
    public boolean run(int[] text, int from, int to) {
        int state = initial;
        for (int i = from; i < to && state != DEAD; i++)
            state = step(state, text[i]);
        return isAccept(state);
    }

    /**
     * Returns the first transition of a state, its transitions being numbered consecutively
     * @param state the state
     * @return the number of its first transition
     */
    int firstTransition(int state) {
        return state == DEAD ? 0 : start[state];
    }

    /**
     * Returns the number following the last transition of a state
     * @param state the state
     * @return the number following its last transition
     */
    int endTransition(int state) {
        return state == DEAD ? 0 : start[state + 1];
    }

    int min(int transition) {
        return mins[transition];
    }

    int max(int transition) {
        return maxs[transition];
    }

    int dest(int transition) {
        return dests[transition];
    }
}
