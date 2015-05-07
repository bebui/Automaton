/**
 * Automaton
 * Copyright (c) 2015, Julien Menana, All rights reserved.
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
 * The {@link fr.menana.automaton.Interval} are stored using a TreeSet to preserve ordering <p>
 * Created by Julien Menana on 01/05/2015.
 */
public class IntervalSet implements Cloneable{

    /**
     * The container of this set
     */
    private TreeSet<Interval> container;

    /**
     * A static reference to an interval set containting all values between Integer.MIN_VALUE and Integer.MAX_VALUE
     */
    public static IntervalSet ALL = new IntervalSet();
    static {
        ALL.add(new Interval(Integer.MIN_VALUE,Integer.MAX_VALUE));
    }

    /**
     * A static reference to an empty interval set
     */
    @SuppressWarnings("unused")
    public static IntervalSet EMPTY = new IntervalSet();


    /**
     * Constucts an empty interval set
     */
    public IntervalSet() {
        this.container = new TreeSet<>();
    }


    private void mergeContigus(Interval a, Interval b)
    {
        a.max = b.max;
        container.remove(b);
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
        Interval tmp = new Interval(value, value);
        Interval floor = container.floor(tmp);
        Interval ceiling = container.ceiling(tmp);

        if (floor == null) {
            if (ceiling == null || value < ceiling.min - 1)
                container.add(tmp);
            else if (value == ceiling.min - 1)
                ceiling.min = value;
        }
        else if (ceiling == null) {
            if (floor.max == value - 1)
                floor.max = value;
            else if (value > floor.max)
                container.add(tmp);
        }
        else {
            if (floor.contains(value) || ceiling.contains(value))
                return false;
            else if (floor.max == value - 1 ) {
                floor.max = value;
                if (floor.max == ceiling.min - 1)
                    mergeContigus(floor, ceiling);
            }
            else if (ceiling.min == value + 1) {
                ceiling.min = value;
                if (floor.max == ceiling.min - 1)
                    mergeContigus(floor, ceiling);
            }
            else
                container.add(tmp);

        }


        return true;
    }

    /**
     * Adds a set of integer values given as another interval set to this interval set
     * @param intervalSet a set of integer values
     * @return <code>true</code> if and only if the values were added
     */
    public boolean add(IntervalSet intervalSet) {
        boolean ret = false;
        if (intervalSet == null)
            return false;
        for (Interval i : intervalSet.container) {
            //System.out.println("on a "+this);
           // System.out.println("on ajoute "+i);
            ret |= this.add(i);
           // System.out.println("on obtient "+this);
           // System.out.println();
        }
        return ret;
    }

    /**
     * Adds a set of integer values given as a {@link fr.menana.automaton.Interval} to this interval set
     * @param interval a set of integer values
     * @return <code>true</code> if and only if the values were added
     */
    public boolean add(Interval interval) {

        //System.transitions.println(this);
        Interval floor = container.floor(interval);
        Interval ceiling = container.ceiling(interval);
       // System.out.println("VAL : " + interval);//
        //System.out.println("FLOOR : " + floor);
       // System.out.println("CEILING : " + ceiling);

        if (floor == null && ceiling == null) {
            container.add(interval);
        }
        else if (floor == null) {
            if (interval.max < ((long) ceiling.min) - 1)
                container.add(interval);
            else if (interval.min < ceiling.min) {
                ceiling.min = interval.min;
                return this.add(interval);
            }
        }
        else if (ceiling == null) {
            if (interval.min <= ((long) floor.max) + 1) {
                floor.max = Math.max(interval.max,floor.max);
            }
            else
                container.add(interval);
        }
        else if (floor.contains(interval) || ceiling.contains(interval)) {
            return false;
        }
        else if (!floor.intersects(interval) && !ceiling.intersects(interval)) {
            if (floor.max == ((long) interval.min) - 1 && ceiling.min == ((long) interval.max) + 1) {
                floor.max = ceiling.max;
                container.remove(ceiling);
            }
            else if (floor.max == ((long) interval.min) - 1)
                floor.max = interval.max;
            else if (ceiling.min == ((long) interval.max) + 1)
                ceiling.min = interval.min;
            else
                container.add(interval);
        }

        else if (ceiling.max < interval.max) {
            container.remove(ceiling);
            return this.add(interval);
        }
        else if (interval.min <= ((long)floor.max) + 1) {
            floor.max = interval.max;
            if (((long)interval.max) +1 >= ceiling.min) {
                floor.max = ceiling.max;
                container.remove(ceiling);
            }
        }
        else if (interval.max <= ceiling.max) {
            ceiling.min = interval.min;
        }
        else
            container.add(interval);


        return true;

    }


    /**
     * Removes a given interval set to this  interval set
     * @param intervalSet the values as an interval set to remove
     */
    @SuppressWarnings("unused")
    public void remove(IntervalSet intervalSet) {
        this.container = this.intersection(intervalSet.complement()).container;

    }

    /**
     * Checks if this  interval set contains a given integer value
     * @param value the integer value to be checked
     * @return <code>true</code>  if and only if this  interval set contains the value
     */
    public boolean contains(int value)
    {
        Interval tmp = new Interval(value, value);
        Interval floor = container.floor(tmp);
        Interval ceiling = container.ceiling(tmp);
        if (floor == ceiling)
            return floor != null;
        else if (floor != null && floor.contains(value))
            return true;
        else if (ceiling != null && ceiling.contains(value))
            return true;
        return false;

    }

    /**
     * Checks if this  interval set is empty
     * @return <code>true</code> if and only if this  interval set has no value
     */
    public boolean isEmpty()
    {
        return this.container.isEmpty();
    }

    /**
     * Checks if a given  interval set intersects with this  interval set
     * @param intervalSet the other  interval set
     * @return  <code>true</code> if and only if the given  interval set and this  interval set have common values
     */
    public boolean intersects(IntervalSet intervalSet)
    {
        if (intervalSet == null)
            return false;
        for (Interval i : intervalSet.container)
        {
            Interval floor = container.floor(i);
            Interval ceiling = container.ceiling(i);
            if (i.intersects(floor) || i.intersects(ceiling))
                return true;
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
        Interval floor = container.floor(interval);
        Interval ceiling = container.ceiling(interval);
        return interval.intersects(floor) || interval.intersects(ceiling);
    }


    /**
     * Returns a new  interval set resulting of the intersection of this  interval set and a given  interval set
     * @param intervalSet the  interval set to intersect
     * @return a new  interval set
     */
    public IntervalSet intersection(IntervalSet intervalSet)
    {
        IntervalSet intervals = new IntervalSet();
        if (intervalSet == null || intervalSet.container == null || this.container == null)
            return intervals;
        for (Interval oi : intervalSet.container) {
            this.container.stream().filter(ti -> oi.intersects(ti)).forEach(ti -> intervals.add(oi.intersection(ti)));
        }
        return intervals;
    }


    /**
     * Returns a new  interval set reprenting the complement in [|Integer.MIN_VALUE,Integer.MAX_VALUE|] of this  interval set
     * @return a new  interval set
     */
    public IntervalSet complement()  {
        IntervalSet ret = new IntervalSet();
        if (this.container == null || this.container.isEmpty()) {
            ret.add(new Interval(Integer.MIN_VALUE,Integer.MAX_VALUE));
            return ret;
        }
        Interval last = null;
        for (Interval i : this.container)
        {
            if (i.min != Integer.MIN_VALUE && last == null) {
                ret.add(new Interval(Integer.MIN_VALUE,i.min -1));
                if (i.max != Integer.MAX_VALUE)
                    last = new Interval(i.max + 1, Integer.MAX_VALUE);
            }
            else if (i.min != Integer.MIN_VALUE) {
                last.max = i.min - 1;
                ret.add(last);
                if (i.max != Integer.MAX_VALUE)
                    last = new Interval(i.max + 1,Integer.MAX_VALUE);
            }
            else if (i.max != Integer.MAX_VALUE) {
                last = new Interval(i.max + 1,Integer.MAX_VALUE);
            }
        }
        if (last != null)
            ret.add(last);
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
        List<IntervalSet> list = new ArrayList<>(Arrays.asList(others));
        list.add(this);
        return IntervalSet.union(list);
    }


    @Override
    public String toString() {
        return container.toString();
    }

    @Override
    public IntervalSet clone(){
        IntervalSet clone = null;
        try {
            clone = (IntervalSet) super.clone();
            clone.container = new TreeSet<>();
            for (Interval i : this.container){
                clone.container.add(i.clone());
            }
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
        for (IntervalSet is : intervals) {
            out.add(is);
        }
        return out;
    }

    /**
     * Returns the {@link java.util.TreeSet} container of {@link fr.menana.automaton.Interval}
     * @return a sorted set of {@link fr.menana.automaton.Interval}
     */
    public TreeSet<Interval> getIntervals() {
        return container;
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
        Arrays.sort(values);
        IntervalSet set = new IntervalSet();
        Interval tmp = null;
        int last = Integer.MAX_VALUE;
        for (int value : values)
        {
            if (tmp == null) {
                tmp = new Interval(value, Integer.MAX_VALUE);
            }
            else if (value - 1 != last) {
                tmp.max = last;
                set.add(tmp);
                tmp = new Interval(value,Integer.MAX_VALUE);
            }
            last = value;

        }
        if (tmp != null && tmp.max == Integer.MAX_VALUE) {
            tmp.max = last;
            set.add(tmp);
        }
        return set;
    }


    /**
     * Consturcts a new interval set from a single {@link fr.menana.automaton.Interval}
     * @param values the {@link fr.menana.automaton.Interval}
     * @return a new interval set
     */
    public static IntervalSet fromInterval(Interval values) {
        IntervalSet set = new IntervalSet();
        set.add(values);
        return set;
    }

    public static void main(String[] args) {
        IntervalSet set = IntervalSet.fromIntArray(1,2,3);
        IntervalSet set2 = IntervalSet.fromIntArray(2,3,4);
        System.out.println(set.intersection(set2));
    }
}
