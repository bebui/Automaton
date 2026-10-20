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

import java.util.*;

/**
 * This class represents an ordered set of integer values as a set of {@link fr.menana.automaton.Interval} <br>
 * The intervals are stored in a sorted int array <code>[min0, max0, min1, max1, ...]</code>.
 * Stored intervals are disjoint and never adjacent, so that every set has a unique canonical representation.
 * Unions, intersections and complements are computed by merging in linear time, membership tests by binary search
 *
 * @author Julien Menana
 */
public class IntervalSet implements Comparable<IntervalSet>,Cloneable{

    /**
     * The bounds of the intervals of this set : interval i is [bounds[2i], bounds[2i+1]]
     */
    private int[] bounds;

    /**
     * The number of intervals in this set
     */
    private int count;

    /**
     * <code>true</code> if this set cannot be modified
     */
    private boolean frozen;

    /**
     * An unmodifiable interval set containing all values between Integer.MIN_VALUE and Integer.MAX_VALUE.
     * Use {@link #clone()} to get a modifiable copy
     */
    public static final IntervalSet ALL = new IntervalSet(new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}, 1).freeze();

    /**
     * An unmodifiable empty interval set.
     * Use {@link #clone()} to get a modifiable copy
     */
    @SuppressWarnings("unused")
    public static final IntervalSet EMPTY = new IntervalSet().freeze();


    /**
     * Constructs an empty interval set
     */
    public IntervalSet() {
        this.bounds = new int[4];
        this.count = 0;
    }

    private IntervalSet(int[] bounds, int count) {
        this.bounds = bounds;
        this.count = count;
    }

    private IntervalSet freeze() {
        this.frozen = true;
        return this;
    }

    private void checkModifiable() {
        if (frozen)
            throw new UnsupportedOperationException("This interval set cannot be modified, use a clone");
    }

    /**
     * Returns the number of disjoint intervals in this set
     * @return the number of intervals
     */
    public int intervalCount() {
        return count;
    }

    /**
     * Returns the lower bound of the i-th interval of this set
     * @param i the interval position
     * @return its lower bound
     */
    int minAt(int i) {
        return bounds[2 * i];
    }

    /**
     * Returns the upper bound of the i-th interval of this set
     * @param i the interval position
     * @return its upper bound
     */
    int maxAt(int i) {
        return bounds[2 * i + 1];
    }

    /**
     * Reads an interval lower bound without allocating an Interval or collection.
     * @param index interval index in sorted canonical order
     * @return inclusive lower bound
     * @throws IndexOutOfBoundsException if index is outside intervalCount()
     */
    public int getIntervalMin(int index) { return bounds[2 * Objects.checkIndex(index, count)]; }

    /**
     * Reads an interval upper bound without allocating an Interval or collection.
     * @param index interval index in sorted canonical order
     * @return inclusive upper bound
     * @throws IndexOutOfBoundsException if index is outside intervalCount()
     */
    public int getIntervalMax(int index) { return bounds[2 * Objects.checkIndex(index, count) + 1]; }

    /**
     * Copies a slice of sorted disjoint inclusive intervals from primitive arrays.
     * Adjacent intervals are merged. Neither input array is retained or modified.
     * @param minima lower bounds
     * @param maxima upper bounds, same length as minima
     * @param from inclusive slice start
     * @param to exclusive slice end
     * @return independent canonical interval set
     * @throws IllegalArgumentException for unequal arrays, inverted or overlapping/unsorted intervals
     * @throws IndexOutOfBoundsException for an invalid slice
     */
    public static IntervalSet fromSortedIntervals(int[] minima, int[] maxima, int from, int to) {
        if (minima.length != maxima.length) throw new IllegalArgumentException("Equal interval-array lengths required");
        Objects.checkFromToIndex(from, to, minima.length);
        IntervalSet result = new IntervalSet();
        for (int i = from; i < to; i++) {
            if (minima[i] > maxima[i] || (i > from && minima[i] <= maxima[i - 1]))
                throw new IllegalArgumentException("Sorted disjoint intervals required");
            result.append(minima[i], maxima[i]);
        }
        return result;
    }

    private void ensureCapacity(int intervals) {
        if (bounds.length < 2 * intervals)
            bounds = Arrays.copyOf(bounds, Math.max(2 * intervals, 2 * bounds.length));
    }

    /**
     * Appends an interval located strictly after every interval of this set.
     * Merges it with the last interval if they are contiguous.
     * @param min the lower bound
     * @param max the upper bound
     */
    void append(int min, int max) {
        checkModifiable();
        if (count > 0 && (long) bounds[2 * count - 1] + 1 >= min) {
            bounds[2 * count - 1] = Math.max(bounds[2 * count - 1], max);
            return;
        }
        ensureCapacity(count + 1);
        bounds[2 * count] = min;
        bounds[2 * count + 1] = max;
        count++;
    }

    /**
     * Adds a set of integer values given as an array to this interval set
     * @param values an int array
     * @return <code>true</code> if and only if the values were added
     */
    @SuppressWarnings("unused")
    public boolean add(int... values) {
        return this.add(IntervalSet.fromIntArray(values));
    }

    /**
     * Adds an integer value to this interval set
     * @param value an int
     * @return <code>true</code> if and only if the value was added
     */
    @SuppressWarnings("unused")
    public boolean add(int value) {
        return this.add(value, value);
    }

    /**
     * Adds a set of integer values given as another interval set to this interval set
     * @param intervalSet a set of integer values
     * @return <code>true</code> if and only if the values were added
     */
    public boolean add(IntervalSet intervalSet) {
        checkModifiable();
        if (intervalSet == null || intervalSet.count == 0)
            return false;
        if (intervalSet.count == 1)
            return this.add(intervalSet.bounds[0], intervalSet.bounds[1]);
        IntervalSet merged = merge(this, intervalSet);
        boolean changed = !merged.equals(this);
        this.bounds = merged.bounds;
        this.count = merged.count;
        return changed;
    }

    /**
     * Adds a set of integer values given as a {@link fr.menana.automaton.Interval} to this interval set
     * @param interval a set of integer values
     * @return <code>true</code> if and only if the values were added
     */
    public boolean add(Interval interval) {
        return this.add(interval.min, interval.max);
    }

    /**
     * Adds the interval [min, max] to this set, merging it with the overlapping or contiguous intervals
     * @param min the lower bound
     * @param max the upper bound
     * @return <code>true</code> if and only if values were added
     */
    private boolean add(int min, int max) {
        checkModifiable();
        // first interval whose max is >= min - 1
        int lo = 0, hi = count;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if ((long) bounds[2 * mid + 1] + 1 < min) lo = mid + 1; else hi = mid;
        }
        int first = lo;
        // first interval whose min is > max + 1
        hi = count;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (bounds[2 * mid] <= (long) max + 1) lo = mid + 1; else hi = mid;
        }
        int last = lo - 1;
        if (first > last) {
            ensureCapacity(count + 1);
            System.arraycopy(bounds, 2 * first, bounds, 2 * first + 2, 2 * (count - first));
            bounds[2 * first] = min;
            bounds[2 * first + 1] = max;
            count++;
            return true;
        }
        int newMin = Math.min(min, bounds[2 * first]);
        int newMax = Math.max(max, bounds[2 * last + 1]);
        boolean changed = first != last || newMin != bounds[2 * first] || newMax != bounds[2 * first + 1];
        bounds[2 * first] = newMin;
        bounds[2 * first + 1] = newMax;
        if (last > first) {
            System.arraycopy(bounds, 2 * last + 2, bounds, 2 * first + 2, 2 * (count - last - 1));
            count -= last - first;
        }
        return changed;
    }


    /**
     * Removes a given interval set to this  interval set
     * @param intervalSet the values as an interval set to remove
     */
    @SuppressWarnings("unused")
    public void remove(IntervalSet intervalSet) {
        checkModifiable();
        IntervalSet res = this.intersection(intervalSet.complement());
        this.bounds = res.bounds;
        this.count = res.count;
    }

    /**
     * Checks if this  interval set contains a given integer value
     * @param value the integer value to be checked
     * @return <code>true</code>  if and only if this  interval set contains the value
     */
    public boolean contains(int value)
    {
        int lo = 0, hi = count - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (bounds[2 * mid + 1] < value) lo = mid + 1;
            else if (bounds[2 * mid] > value) hi = mid - 1;
            else return true;
        }
        return false;
    }

    /**
     * Checks if this  interval set is empty
     * @return <code>true</code> if and only if this  interval set has no value
     */
    public boolean isEmpty()
    {
        return this.count == 0;
    }

    /**
     * Checks if a given  interval set intersects with this  interval set
     * @param intervalSet the other  interval set
     * @return  <code>true</code> if and only if the given  interval set and this  interval set have common values
     */
    public boolean intersects(IntervalSet intervalSet) {
        if (intervalSet == null)
            return false;
        int i = 0, j = 0;
        int[] a = this.bounds, b = intervalSet.bounds;
        while (i < this.count && j < intervalSet.count) {
            if (a[2 * i + 1] < b[2 * j]) i++;
            else if (b[2 * j + 1] < a[2 * i]) j++;
            else return true;
        }
        return false;
    }

    /**
     * Checks if a given  {@link fr.menana.automaton.Interval} intersects with this  interval set
     * @param interval the  {@link fr.menana.automaton.Interval}
     * @return  <code>true</code> if and only if the given  {@link fr.menana.automaton.Interval} and this  interval set have common values
     */
    public boolean intersects(Interval interval)
    {
        if (interval == null)
            return false;
        // first interval whose max is >= interval.min
        int lo = 0, hi = count;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (bounds[2 * mid + 1] < interval.min) lo = mid + 1; else hi = mid;
        }
        return lo < count && bounds[2 * lo] <= interval.max;
    }


    /**
     * Returns a new  interval set resulting of the intersection of this  interval set and a given  interval set
     * @param intervalSet the  interval set to intersect
     * @return a new  interval set
     */
    public IntervalSet intersection(IntervalSet intervalSet)
    {
        IntervalSet out = new IntervalSet();
        if (intervalSet == null)
            return out;
        int i = 0, j = 0;
        int[] a = this.bounds, b = intervalSet.bounds;
        while (i < this.count && j < intervalSet.count) {
            int min = Math.max(a[2 * i], b[2 * j]);
            int max = Math.min(a[2 * i + 1], b[2 * j + 1]);
            if (min <= max)
                out.append(min, max);
            if (a[2 * i + 1] < b[2 * j + 1]) i++; else j++;
        }
        return out;
    }


    /**
     * Returns a new  interval set representing the complement in [|Integer.MIN_VALUE,Integer.MAX_VALUE|] of this  interval set
     * @return a new  interval set
     */
    public IntervalSet complement()  {
        IntervalSet ret = new IntervalSet();
        long next = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            if (bounds[2 * i] > next)
                ret.append((int) next, bounds[2 * i] - 1);
            next = (long) bounds[2 * i + 1] + 1;
        }
        if (next <= Integer.MAX_VALUE)
            ret.append((int) next, Integer.MAX_VALUE);
        return ret;
    }

    /**
     * Returns a new  interval set equal to this  interval set minus the  interval set given
     * @param others the  interval set to remove to this  interval set
     * @return a new  interval set
     */
    @SuppressWarnings("unused")
    public IntervalSet minus(IntervalSet... others) {
        IntervalSet ret = this;
        for (IntervalSet other : others) {
            ret = ret.intersection(other.complement());
        }
        return ret;
    }

    /**
     * Returns the union of this  interval set with a list of  interval set
     * @param others the  interval set to add to this  interval set
     * @return a new  interval set
     */
    @SuppressWarnings("unused")
    public IntervalSet union(IntervalSet... others) {
        IntervalSet out = this.clone();
        for (IntervalSet other : others)
            out.add(other);
        return out;
    }

    /**
     * Merges two interval sets in linear time
     * @param left the first set
     * @param right the second set
     * @return a new interval set containing the values of both sets
     */
    private static IntervalSet merge(IntervalSet left, IntervalSet right) {
        IntervalSet out = new IntervalSet(new int[2 * (left.count + right.count)], 0);
        int i = 0, j = 0;
        int[] a = left.bounds, b = right.bounds;
        while (i < left.count || j < right.count) {
            if (j == right.count || (i < left.count && a[2 * i] <= b[2 * j])) {
                out.append(a[2 * i], a[2 * i + 1]);
                i++;
            } else {
                out.append(b[2 * j], b[2 * j + 1]);
                j++;
            }
        }
        return out;
    }

    /**
     * Returns the minimum value of this set of intervals
     * @return the minimum value of this set
     * @throws NoSuchElementException if this set is empty
     */
    public int getMin() {
        if (count == 0)
            throw new NoSuchElementException();
        return bounds[0];
    }


    /**
     * Returns the maximum value of this set of intervals
     * @return the maximum value of this set
     * @throws NoSuchElementException if this set is empty
     */
    public int getMax() {
        if (count == 0)
            throw new NoSuchElementException();
        return bounds[2 * count - 1];
    }

    /**
     * Returns the cardinality of this set, up to 2^32 for {@link #ALL}
     * @return the cardinality of this set
     */
    public long size() {
        long size = 0;
        for (int i = 0; i < count; i++)
            size += (long) bounds[2 * i + 1] - bounds[2 * i] + 1;
        return size;
    }




    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0)
                b.append(", ");
            b.append(new Interval(bounds[2 * i], bounds[2 * i + 1]));
        }
        return b.append("]").toString();
    }

    @Override
    public IntervalSet clone() {
        IntervalSet clone = null;
        try {
            clone = (IntervalSet) super.clone();
            clone.bounds = Arrays.copyOf(this.bounds, Math.max(2 * this.count, 2));
            clone.frozen = false;
        } catch (CloneNotSupportedException ignored) {

        }
        return clone;
    }

    /**
     * Returns the union the given interval set
     * @param intervals the  interval set to add to each other
     * @return a new  interval set
     */
    public static IntervalSet union(Collection<IntervalSet> intervals) {
        IntervalSet out = new IntervalSet();
        intervals.forEach(out::add);
        return out;
    }

    /**
     * Returns a sorted set containing a copy of the {@link fr.menana.automaton.Interval} of this set.
     * Modifying the returned set does not modify this interval set.
     * @return a sorted set of {@link fr.menana.automaton.Interval}
     */
    public TreeSet<Interval> getIntervals() {
        TreeSet<Interval> out = new TreeSet<>();
        for (int i = 0; i < count; i++)
            out.add(new Interval(bounds[2 * i], bounds[2 * i + 1]));
        return out;
    }


    /**
     * Returns a new  interval set resulting of the intersection of the given  interval set
     * @param intervalSets the  interval set to intersect
     * @return a new  interval set
     */
    @SuppressWarnings("unused")
    public static IntervalSet intersection(Collection<IntervalSet> intervalSets) {
        IntervalSet out = new IntervalSet();
        if (intervalSets.isEmpty())
            return out;

        out = out.complement();
        for (IntervalSet is : intervalSets)
        {
            out = out.intersection(is);
        }

        return out;
    }

    /**
     * Return a new interval set from an integer array
     * @param values the int array used to construct the interval set
     * @return a new interval set containing all values in the given int array
     */
    public static IntervalSet fromIntArray(int... values)
    {
        int[] sorted = values.clone();
        Arrays.sort(sorted);
        IntervalSet set = new IntervalSet();
        for (int value : sorted)
            set.append(value, value);
        return set;
    }


    public boolean equals(Object other) {
        if (other == this)
            return true;
        else if (other instanceof IntervalSet) {
            IntervalSet inter = (IntervalSet) other;
            if (inter.count != this.count)
                return false;
            for (int i = 0; i < 2 * count; ++i) {
                if (inter.bounds[i] != this.bounds[i])
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int h = 1;
        for (int i = 0; i < 2 * count; ++i)
            h = 31 * h + bounds[i];
        return h;
    }


    /**
     * Constructs a new interval set from a single {@link fr.menana.automaton.Interval}
     * @param values the {@link fr.menana.automaton.Interval}
     * @return a new interval set
     */
    public static IntervalSet fromInterval(Interval values) {
        IntervalSet set = new IntervalSet();
        set.add(values);
        return set;
    }

    @Override
    public int compareTo(IntervalSet other) {
        if (other == null)
            return 1;
        else if (this.getMin() <other.getMin())
            return -1;
        else if (this.getMin() == other.getMin()) {
            if (this.getMax() == other.getMax())
                return Long.compare(this.size(), other.size());
            else
                return Integer.compare(this.getMax(), other.getMax());
        }
        else
            return 1;

    }
}
