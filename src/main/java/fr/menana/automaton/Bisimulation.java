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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Computes the coarsest bisimulation of a non-deterministic automaton with the algorithm of Paige and Tarjan,
 * in O(m log n). <br>
 * Besides the partition of the states into blocks, it keeps a coarser partition into compound blocks such that every
 * block is stable with respect to every compound block : two states of a block have transitions with the same labels
 * into the same compound blocks. A compound block made of several blocks is split by one of its blocks B holding at most
 * half of its states, and the transitions into B are counted per state and label, so that the states whose transitions
 * into the compound block all go to B can be separated without looking at the rest of the compound block
 */
final class Bisimulation {

    private Bisimulation() {
        // Hiding the implicit public one
    }

    /**
     * Returns the coarsest bisimulation refining the partition into accepting and non accepting states
     * @param n the number of states
     * @param src the source of each transition
     * @param lab the label of each transition, between 0 and nbLabels - 1
     * @param dst the destination of each transition
     * @param nbLabels the number of labels
     * @param states the states, to know which ones are accepting
     * @return the block of each state, blocks being numbered from 0
     */
    static int[] coarsest(int n, int[] src, int[] lab, int[] dst, int nbLabels, List<State> states) {
        int m = src.length;
        Blocks blocks = new Blocks(n);
        for (State s : states)
            if (s.accept)
                blocks.mark(s.index);
        blocks.split();

        // Counters (source, label, compound block) : count[counterOf[t]] transitions of the source of t, with the label of t,
        // go into the compound block containing the destination of t. Initially a single compound block
        int[] counterOf = new int[m];
        int[] count = new int[m + 1];
        int nbCounters = 0;
        Integer[] bySourceLabel = new Integer[m];
        for (int t = 0; t < m; t++)
            bySourceLabel[t] = t;
        Arrays.sort(bySourceLabel, (x, y) -> src[x] != src[y] ? Integer.compare(src[x], src[y]) : Integer.compare(lab[x], lab[y]));
        for (int i = 0; i < m; i++) {
            int t = bySourceLabel[i];
            if (i == 0 || src[t] != src[bySourceLabel[i - 1]] || lab[t] != lab[bySourceLabel[i - 1]])
                nbCounters++;
            counterOf[t] = nbCounters - 1;
            count[nbCounters - 1]++;
        }
        count = Arrays.copyOf(count, Math.max(2 * m + 1, 1));

        // Incoming transitions of each state, sorted by label
        Integer[] byDestLabel = new Integer[m];
        for (int i = 0; i < m; i++)
            byDestLabel[i] = i;
        Arrays.sort(byDestLabel, (x, y) -> dst[x] != dst[y] ? Integer.compare(dst[x], dst[y]) : Integer.compare(lab[x], lab[y]));
        int[] inStart = new int[n + 1];
        for (int t = 0; t < m; t++)
            inStart[dst[t] + 1]++;
        for (int q = 0; q < n; q++)
            inStart[q + 1] += inStart[q];
        int[] into = new int[m];
        for (int i = 0; i < m; i++)
            into[i] = byDestLabel[i];

        // The initial blocks must be stable with respect to the single compound block : split by "has a transition labelled a"
        int[] stamp = new int[n];
        int generation = 0;
        List<List<Integer>> sourcesByLabel = new ArrayList<>();
        for (int a = 0; a < nbLabels; a++)
            sourcesByLabel.add(new ArrayList<>());
        for (int t = 0; t < m; t++)
            sourcesByLabel.get(lab[t]).add(src[t]);
        for (int a = 0; a < nbLabels; a++) {
            generation++;
            for (int x : sourcesByLabel.get(a)) {
                if (stamp[x] != generation) {
                    stamp[x] = generation;
                    blocks.mark(x);
                }
            }
            blocks.split();
        }

        Compounds compounds = new Compounds(n);
        for (int b = 0; b < blocks.z; b++)
            compounds.addBlock(0, b, blocks);

        int[] newCount = new int[n];
        int[] newCounter = new int[n];
        int[] group = new int[m];
        while (compounds.hasNonTrivial()) {
            int compound = compounds.nextNonTrivial();
            int b = compounds.extractSmallBlock(compound, blocks);
            // Snapshot of the transitions into B, grouped by label
            int nbGroup = 0;
            for (int i = blocks.first[b]; i < blocks.past[b]; i++) {
                int y = blocks.elems[i];
                for (int j = inStart[y]; j < inStart[y + 1]; j++)
                    group[nbGroup++] = into[j];
            }
            sortByLabel(group, nbGroup, lab, nbLabels);
            int from = 0;
            while (from < nbGroup) {
                int a = lab[group[from]];
                int to = from;
                while (to < nbGroup && lab[group[to]] == a)
                    to++;
                // Split by the predecessors of B with label a, counting their transitions into B
                generation++;
                for (int i = from; i < to; i++) {
                    int x = src[group[i]];
                    if (stamp[x] != generation) {
                        stamp[x] = generation;
                        newCount[x] = 0;
                        blocks.mark(x);
                    }
                    newCount[x]++;
                }
                splitAndRecord(blocks, compounds);
                // Split by the predecessors whose a-transitions into the compound block all go into B
                generation++;
                for (int i = from; i < to; i++) {
                    int x = src[group[i]];
                    if (stamp[x] != generation) {
                        stamp[x] = generation;
                        if (newCount[x] == count[counterOf[group[i]]])
                            blocks.mark(x);
                    }
                }
                splitAndRecord(blocks, compounds);
                // The old counters now count the transitions into the rest of the compound block
                generation++;
                for (int i = from; i < to; i++) {
                    int t = group[i];
                    int x = src[t];
                    count[counterOf[t]]--;
                    if (stamp[x] != generation) {
                        stamp[x] = generation;
                        if (nbCounters == count.length)
                            count = Arrays.copyOf(count, 2 * count.length);
                        newCounter[x] = nbCounters;
                        count[nbCounters++] = newCount[x];
                    }
                    counterOf[t] = newCounter[x];
                }
                from = to;
            }
        }
        return Arrays.copyOf(blocks.setOf, n);
    }

    private static void splitAndRecord(Blocks blocks, Compounds compounds) {
        int before = blocks.z;
        blocks.split();
        for (int b = before; b < blocks.z; b++)
            compounds.addBlock(compounds.compoundOf[blocks.parent[b]], b, blocks);
    }

    /**
     * Sorts transitions by label with a counting sort
     */
    private static void sortByLabel(int[] group, int size, int[] lab, int nbLabels) {
        if (size < 2)
            return;
        int[] counts = new int[nbLabels + 1];
        for (int i = 0; i < size; i++)
            counts[lab[group[i]] + 1]++;
        for (int a = 0; a < nbLabels; a++)
            counts[a + 1] += counts[a];
        int[] sorted = new int[size];
        for (int i = 0; i < size; i++)
            sorted[counts[lab[group[i]]]++] = group[i];
        System.arraycopy(sorted, 0, group, 0, size);
    }

    /**
     * Refinable partition of the states, as in Valmari and Lehtinen's algorithm, remembering the set each new set comes from
     */
    private static final class Blocks {
        int z;
        final int[] elems, loc, setOf, first, past, parent;
        private final int[] marked, touched;
        private int nbTouched;

        Blocks(int n) {
            elems = new int[n];
            loc = new int[n];
            setOf = new int[n];
            first = new int[n + 1];
            past = new int[n + 1];
            parent = new int[n + 1];
            marked = new int[n + 1];
            touched = new int[n + 1];
            for (int i = 0; i < n; i++)
                elems[i] = loc[i] = i;
            z = n > 0 ? 1 : 0;
            past[0] = n;
        }

        void mark(int e) {
            int s = setOf[e], i = loc[e], j = first[s] + marked[s];
            if (i < j)
                return;
            elems[i] = elems[j];
            loc[elems[i]] = i;
            elems[j] = e;
            loc[e] = j;
            if (marked[s]++ == 0)
                touched[nbTouched++] = s;
        }

        void split() {
            while (nbTouched > 0) {
                int s = touched[--nbTouched], j = first[s] + marked[s];
                if (j == past[s]) {
                    marked[s] = 0;
                    continue;
                }
                if (marked[s] <= past[s] - j) {
                    first[z] = first[s];
                    past[z] = first[s] = j;
                } else {
                    past[z] = past[s];
                    first[z] = past[s] = j;
                }
                for (int i = first[z]; i < past[z]; i++)
                    setOf[elems[i]] = z;
                parent[z] = s;
                marked[s] = marked[z++] = 0;
            }
        }

        int size(int b) {
            return past[b] - first[b];
        }
    }

    /**
     * Partition of the blocks into compound blocks, with the list of compound blocks made of several blocks
     */
    private static final class Compounds {
        final int[] compoundOf;
        private final int[] position;
        private final List<List<Integer>> members = new ArrayList<>();
        private final Deque<Integer> nonTrivial = new ArrayDeque<>();
        private final List<Boolean> queued = new ArrayList<>();

        Compounds(int n) {
            compoundOf = new int[n + 1];
            position = new int[n + 1];
            members.add(new ArrayList<>());
            queued.add(false);
        }

        void addBlock(int compound, int b, Blocks blocks) {
            compoundOf[b] = compound;
            position[b] = members.get(compound).size();
            members.get(compound).add(b);
            if (members.get(compound).size() >= 2 && !queued.get(compound)) {
                queued.set(compound, true);
                nonTrivial.push(compound);
            }
        }

        boolean hasNonTrivial() {
            return !nonTrivial.isEmpty();
        }

        int nextNonTrivial() {
            return nonTrivial.peek();
        }

        /**
         * Moves a block holding at most half of the states of a compound block into a new compound block of its own
         * @return the extracted block
         */
        int extractSmallBlock(int compound, Blocks blocks) {
            List<Integer> list = members.get(compound);
            int b0 = list.get(0), b1 = list.get(1);
            int b = blocks.size(b0) <= blocks.size(b1) ? b0 : b1;
            int last = list.remove(list.size() - 1);
            if (last != b) {
                list.set(position[b], last);
                position[last] = position[b];
            }
            if (list.size() < 2) {
                queued.set(compound, false);
                nonTrivial.remove(compound);
            }
            members.add(new ArrayList<>());
            queued.add(false);
            int own = members.size() - 1;
            compoundOf[b] = own;
            position[b] = 0;
            members.get(own).add(b);
            return b;
        }
    }
}
