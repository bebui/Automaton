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

import java.util.Arrays;
import java.util.Collection;

/**
 * Partition of the integer values used by a collection of {@link fr.menana.automaton.IntervalSet} into symbol classes. <br>
 * Two values belong to the same class if and only if every given set contains both or none of them.
 * Classes are disjoint intervals numbered in increasing order, so that algorithms can work on small integer symbols.
 * Values used by none of the sets belong to no class <p>
 */
final class Alphabet {

    /**
     * Lower bounds of the classes, sorted
     */
    private final int[] mins;

    /**
     * Upper bounds of the classes
     */
    private final int[] maxs;

    private Alphabet(int[] mins, int[] maxs) {
        this.mins = mins;
        this.maxs = maxs;
    }

    /**
     * Computes the partition of the values used by the given sets in O(m log m), where m is the total number of intervals
     * @param sets the interval sets, <code>null</code> elements are ignored
     * @return the alphabet partition
     */
    static Alphabet of(Collection<IntervalSet> sets) {
        int nb = 0;
        for (IntervalSet set : sets)
            if (set != null)
                nb += set.intervalCount();
        // Every interval [min, max] starts a segment at min and ends it before max + 1
        long[] cuts = new long[2 * nb];
        int k = 0;
        for (IntervalSet set : sets) {
            if (set == null)
                continue;
            for (int i = 0; i < set.intervalCount(); i++) {
                cuts[k++] = set.minAt(i);
                cuts[k++] = (long) set.maxAt(i) + 1;
            }
        }
        Arrays.sort(cuts);
        int nbCuts = 0;
        for (int i = 0; i < cuts.length; i++)
            if (nbCuts == 0 || cuts[i] != cuts[nbCuts - 1])
                cuts[nbCuts++] = cuts[i];
        // Number of intervals covering each segment [cuts[i], cuts[i+1])
        int[] cover = new int[Math.max(nbCuts, 1)];
        for (IntervalSet set : sets) {
            if (set == null)
                continue;
            for (int i = 0; i < set.intervalCount(); i++) {
                cover[Arrays.binarySearch(cuts, 0, nbCuts, set.minAt(i))]++;
                cover[Arrays.binarySearch(cuts, 0, nbCuts, (long) set.maxAt(i) + 1)]--;
            }
        }
        int[] mins = new int[Math.max(nbCuts - 1, 0)];
        int[] maxs = new int[mins.length];
        int size = 0;
        int depth = 0;
        for (int i = 0; i + 1 < nbCuts; i++) {
            depth += cover[i];
            if (depth > 0) {
                mins[size] = (int) cuts[i];
                maxs[size] = (int) (cuts[i + 1] - 1);
                size++;
            }
        }
        return new Alphabet(Arrays.copyOf(mins, size), Arrays.copyOf(maxs, size));
    }

    /**
     * Returns the number of classes
     * @return the number of classes
     */
    int size() {
        return mins.length;
    }

    /**
     * Returns the lower bound of a class
     * @param c the class
     * @return its lower bound
     */
    int min(int c) {
        return mins[c];
    }

    /**
     * Returns the upper bound of a class
     * @param c the class
     * @return its upper bound
     */
    int max(int c) {
        return maxs[c];
    }

    /**
     * Returns the classes covering a set that was used to build this alphabet
     * @param set the interval set
     * @return the classes in increasing order
     */
    int[] classesOf(IntervalSet set) {
        int nb = 0;
        int[] firsts = new int[set.intervalCount()];
        int[] lasts = new int[set.intervalCount()];
        for (int i = 0; i < set.intervalCount(); i++) {
            firsts[i] = firstClassFrom(set.minAt(i));
            lasts[i] = firstClassFrom((long) set.maxAt(i) + 1);
            nb += lasts[i] - firsts[i];
        }
        int[] out = new int[nb];
        int k = 0;
        for (int i = 0; i < firsts.length; i++)
            for (int c = firsts[i]; c < lasts[i]; c++)
                out[k++] = c;
        return out;
    }

    /**
     * Returns the first class whose lower bound is greater or equal to the given value
     */
    private int firstClassFrom(long value) {
        int lo = 0, hi = mins.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (mins[mid] < value) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    /**
     * Appends the values of a class to an interval set, classes being appended in increasing order
     * @param set the interval set
     * @param c the class
     */
    void appendTo(IntervalSet set, int c) {
        set.append(mins[c], maxs[c]);
    }
}
