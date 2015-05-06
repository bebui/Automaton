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
 * This class represent an interval of integers.
 * It can be manipulated using the given primitives
 * Created by Julien Menana on 01/05/2015.
 */
public class Interval implements Comparable<Interval>,Cloneable {

    /**
     * The lower bound of the interval
     */
    int min;

    /**
     * The upper bound of the interval
     */
    int max;


    /**
     * Constructs a new {@link fr.menana.automaton.Interval} between a lower and an upper bound.
     * Bounds are included in the {@link fr.menana.automaton.Interval}
     *
     * @param min the lower bound
     * @param max the upper bound
     */
    public Interval(int min, int max)
    {
        this.min = min;
        this.max = max;
        if (this.min > this.max) {
            System.err.println("min > max, on echange les valeurs");
            this.min = max;
            this.max = min;
        }
    }

    /**
     * Checks if this {@link fr.menana.automaton.Interval} contains a given integer value
     * @param value the value to be checked
     * @return <code>true</code> if and only if the value belongs to this {@link fr.menana.automaton.Interval}
     */
    public boolean contains(int value)
    {
        return value >= this.min && value <= this.max;
    }

    /**
     * Checks if this {@link fr.menana.automaton.Interval} contains the given {@link fr.menana.automaton.Interval}
     * @param value the {@link fr.menana.automaton.Interval} to be checked
     * @return <code>true</code> if and only if the {@link fr.menana.automaton.Interval} is included in this {@link fr.menana.automaton.Interval}
     */
    public boolean contains(Interval value)
    {
        return this.contains(value.min) && this.contains(value.max) ||
                this.equals(value);
    }

    /**
     * Checks if a given {@link fr.menana.automaton.Interval} intersects with this {@link fr.menana.automaton.Interval}
     * @param interval the other {@link fr.menana.automaton.Interval}
     * @return <code>true</code> if and only if the two intervals intersect
     */
    public boolean intersects(Interval interval) {
        if (interval == null)
            return false;
        return !(this.max < interval.min || interval.max < this.min);

    }

    public int compareTo(Interval o) {
        if (this == o || this.equals(o))
            return 0;
        if (this.min < o.min)
            return -1;
        else if (this.min > o.min)
            return 1;
        else
            return this.max - o.max;

    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        else if (o != null && o instanceof Interval) {
            Interval i = (Interval) o;
            return this.min == i.min && this.max == i.max;
        }
        return false;
    }

    public String toString() {
        String min = Integer.toString(this.min);
        String max = Integer.toString(this.max);
        if (this.min == Integer.MIN_VALUE)
            min = "-"+Character.toString('\u221e');
        else if (this.min == Integer.MAX_VALUE)
            min = "-"+Character.toString('\u221e');
        if (this.max == Integer.MIN_VALUE)
            max = "-"+Character.toString('\u221e');
        if (this.max == Integer.MAX_VALUE)
            max = Character.toString('\u221e');

        return "["+min+(this.max==this.min ? "]" : ","+max+"]");
    }

    /**
     * Returns the intersection of this {@link fr.menana.automaton.Interval} with a given {@link fr.menana.automaton.Interval}
     * @param interval the other {@link fr.menana.automaton.Interval}
     * @return a new {@link fr.menana.automaton.Interval} if and only if the intersection is not empty, null otherwise.
     */
    public Interval intersection(Interval interval) {
        if (! intersects(interval))
            return null;
        Interval ret = new Interval(Math.max(this.min,interval.min),Math.min(this.max, interval.max));
        return ret;
    }

    /**
     * Returns the complement of this {@link fr.menana.automaton.Interval} in [|Integer.MIN_VALUE,Integer.MAX_VALUE|]
     * @return a list of {@link fr.menana.automaton.Interval}
     */
    public List<Interval> complement()
    {
        List<Interval> out = new ArrayList<>(2);
        if (this.min != Integer.MIN_VALUE && this.max != Integer.MAX_VALUE) {
            out.add(new Interval(Integer.MIN_VALUE,this.min - 1));
            out.add(new Interval(this.max + 1 , Integer.MAX_VALUE));
        }
        else if (this.min == Integer.MIN_VALUE && this.max != Integer.MAX_VALUE) {
            out.add(new Interval(this.max +1, Integer.MAX_VALUE));
        }
        else if (this.min != Integer.MIN_VALUE && this.max == Integer.MAX_VALUE) {
            out.add(new Interval(Integer.MIN_VALUE,this.min - 1));
        }
        return out;
    }

    public Interval clone() {
        Interval clone = null;
        try {
            clone = (Interval) super.clone();
            clone.max = this.max;
            clone.min = this.min;
        } catch (CloneNotSupportedException e) {}
        return clone;
    }


}
