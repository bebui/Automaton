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
 * Created by Julien Menana on 01/05/2015.
 */
public class IntervalSet implements Cloneable{

    private TreeSet<Interval> container;

    public static IntervalSet ALL = new IntervalSet();
    static {
        ALL.add(new Interval(Integer.MIN_VALUE,Integer.MAX_VALUE));
    }
    public static IntervalSet EMPTY = new IntervalSet();



    public IntervalSet() {
        this.container = new TreeSet<Interval>();
    }

    private void merge(Interval a, Interval b)
    {
        a.max = b.max;
        container.remove(b);
    }

    public boolean add(int... values) {
        return this.add(IntervalSet.fromIntArray(values));
    }

    public boolean add(int value) {
        Interval tmp = new Interval(value, value);
        Interval floor = container.floor(tmp);
        Interval ceiling = container.ceiling(tmp);


//        System.transitions.println("VAL : "+tmp);
//        System.transitions.println("FLOOR : "+floor);
//        System.transitions.println("CEILING : "+ceiling);

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
                    merge(floor,ceiling);
            }
            else if (ceiling.min == value + 1) {
                ceiling.min = value;
                if (floor.max == ceiling.min - 1)
                    merge(floor,ceiling);
            }
            else
                container.add(tmp);

        }


        return true;
    }

    public boolean add(IntervalSet intervalSet) {
        boolean ret = false;
        if (intervalSet == null)
            return ret;
        for (Interval i : intervalSet.container) {
            //System.out.println("on a "+this);
           // System.out.println("on ajoute "+i);
            ret |= this.add(i);
           // System.out.println("on obtient "+this);
           // System.out.println();
        }
        return ret;
    }

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


    public void remove(IntervalSet other) {
        this.container = this.intersection(other.complement()).container;

    }

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

    public boolean isEmpty()
    {
        return this.container.isEmpty();
    }

    public boolean intersects(IntervalSet o)
    {
        if (o == null)
            return false;
        for (Interval i : o.container)
        {
            Interval floor = container.floor(i);
            Interval ceiling = container.ceiling(i);
            if (i.intersects(floor) || i.intersects(ceiling))
                return true;
        }
        return false;
    }

    public boolean intersects(Interval i)
    {
        if (i == null)
            return false;
        Interval floor = container.floor(i);
        Interval ceiling = container.ceiling(i);
        return i.intersects(floor) || i.intersects(ceiling);
    }

    public boolean contains(IntervalSet o)
    {
        return false;
    }

    public IntervalSet intersection(IntervalSet o)
    {
        IntervalSet intervals = new IntervalSet();
        if (o == null || o.container == null || this.container == null)
            return intervals;
        for (Interval oi : o.container) {
            for (Interval ti : this.container) {
                if (oi.intersects(ti)) {
                    intervals.add(oi.intersection(ti));
                }
            }
        }
        return intervals;
    }


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

    public IntervalSet minus(IntervalSet... others) {
        IntervalSet ret = this;
        for (IntervalSet other : others) {
            ret = ret.intersection(other.complement());
        }
        return ret;
    }

    public IntervalSet union(IntervalSet... others) {
        List<IntervalSet> list = new ArrayList<IntervalSet>(Arrays.asList(others));
        list.add(this);
        IntervalSet ret = IntervalSet.union(list);
        return ret;
    }


    public String toString() {
        return container.toString();
    }

    public IntervalSet clone(){
        IntervalSet clone = null;
        try {
            clone = (IntervalSet) super.clone();
            clone.container = new TreeSet<Interval>();
            for (Interval i : this.container){
                clone.container.add(i.clone());
            }
        } catch (CloneNotSupportedException e) {

        }
        return clone;
    }

    public static IntervalSet union(Collection<IntervalSet> intervals) {
        IntervalSet out = new IntervalSet();
        for (IntervalSet is : intervals) {
            out.add(is);
        }
        return out;
    }

    public TreeSet<Interval> getIntervals() {
        return container;
    }

    public static IntervalSet intersection(Collection<IntervalSet> intervals) {
        IntervalSet out = new IntervalSet();
        if (intervals.isEmpty())
            return out;

        out = out.complement();
        for (IntervalSet is : intervals)
        {
            out = out.intersection(is);
        }

        return out;
    }

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


    public static IntervalSet fromInterval(Interval values) {
        IntervalSet set = new IntervalSet();
        set.add(values);
        return set;
    }
}
