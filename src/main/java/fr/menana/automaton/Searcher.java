/**
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

import java.util.ArrayList;
import java.util.List;

/**
 * Searches the factors of a sequence of integers that belong to the language of an automaton. <br>
 * Matches are leftmost-longest : the match starting first is chosen, and among those the longest one.
 * Finding the leftmost start takes a single backward pass, with the automaton recognizing the mirror of
 * <code>L.*</code>, so a search is linear in the length of the text, plus the length of the match. <br>
 * Instances are immutable and can be shared between threads. <p>
 * <pre>
 * Searcher searcher = Automaton.dfaFromString("12+").searcher();
 * searcher.find(3, 1, 2, 2, 4);   // Match[start=1, end=4]
 * </pre>
 */
public final class Searcher {

    /**
     * A match of a search : the symbols text[start .. end-1]
     * @param start the index of the first symbol of the match
     * @param end the index following the last symbol of the match
     */
    public record Match(int start, int end) {
        /**
         * Returns the number of symbols of the match
         * @return end - start
         */
        public int length() {
            return end - start;
        }
    }

    /**
     * Recognizes the language L
     */
    private final CompiledDfa forward;

    /**
     * Recognizes the mirror of L.* : reading text[i ..] backwards ends in an accepting state iff a match starts at i
     */
    private final CompiledDfa startFinder;

    /**
     * Prepares the search of the words of the language of an automaton
     * @param automaton the automaton
     */
    public Searcher(Automaton automaton) {
        this.forward = CompiledDfa.of(automaton.minimize());
        Automaton anything = Automaton.dfaFromString(".*");
        this.startFinder = CompiledDfa.of(Operation.concatenate(anything, automaton.revert(), false).minimize());
    }

    /**
     * Returns the leftmost-longest match in a text
     * @param text the text
     * @return the match, or <code>null</code> if no factor of the text belongs to the language
     */
    public Match find(int... text) {
        return find(text, 0);
    }

    /**
     * Returns the leftmost-longest match in a text starting at or after a given index
     * @param text the text
     * @param from the index from which matches may start
     * @return the match, or <code>null</code> if there is none
     */
    public Match find(int[] text, int from) {
        int start = leftmostStart(text, from);
        return start < 0 ? null : new Match(start, longestEnd(text, start));
    }

    /**
     * Checks if a factor of the text belongs to the language
     * @param text the text
     * @return <code>true</code> if and only if a match exists
     */
    public boolean contains(int... text) {
        return leftmostStart(text, 0) >= 0;
    }

    /**
     * Returns the successive non overlapping leftmost-longest matches of a text. <br>
     * After an empty match, the search resumes one symbol further
     * @param text the text
     * @return the matches in increasing order
     */
    public List<Match> findAll(int... text) {
        List<Match> matches = new ArrayList<>();
        // Every possible start, computed in a single backward pass
        boolean[] starts = new boolean[text.length + 1];
        int state = startFinder.getInitialState();
        starts[text.length] = startFinder.isAccept(state);
        for (int i = text.length - 1; i >= 0 && state != CompiledDfa.DEAD; i--) {
            state = startFinder.step(state, text[i]);
            starts[i] = startFinder.isAccept(state);
        }
        int i = 0;
        while (i <= text.length) {
            if (!starts[i]) {
                i++;
                continue;
            }
            int end = longestEnd(text, i);
            matches.add(new Match(i, end));
            i = end > i ? end : i + 1;
        }
        return matches;
    }

    /**
     * Returns the smallest index at or after <code>from</code> where a match starts, or -1
     */
    private int leftmostStart(int[] text, int from) {
        int state = startFinder.getInitialState();
        int start = startFinder.isAccept(state) ? text.length : -1;
        for (int i = text.length - 1; i >= from && state != CompiledDfa.DEAD; i--) {
            state = startFinder.step(state, text[i]);
            if (startFinder.isAccept(state))
                start = i;
        }
        return start;
    }

    /**
     * Returns the end of the longest match starting at a given index, a match being known to start there
     */
    private int longestEnd(int[] text, int start) {
        int state = forward.getInitialState();
        int end = start;
        for (int i = start; i < text.length && state != CompiledDfa.DEAD; i++) {
            state = forward.step(state, text[i]);
            if (forward.isAccept(state))
                end = i + 1;
        }
        return end;
    }
}
