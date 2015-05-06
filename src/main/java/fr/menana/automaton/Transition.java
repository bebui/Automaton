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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Julien Menana on 01/05/2015.
 */
public class Transition  {

    /**
     * The origin {@link fr.menana.automaton.State} of this transition
     */
    public State orig;

    /**
     * The destination {@link fr.menana.automaton.State} of this transition
     */
    public State dest;

    /**
     * the values carried by this transition represented as an {@link fr.menana.automaton.IntervalSet}
     */
    public IntervalSet values;

    /**
     * Inidicates if this transition carries an epsilon
     */
    boolean epsilon;

    /**
     * Constructs a new Transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State} with
     * values given as an {@link fr.menana.automaton.IntervalSet}
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param values the values as an {@link fr.menana.automaton.IntervalSet}
     */
    public Transition(State orig, State dest, IntervalSet values) {
        this.orig = orig;
        this.dest = dest;
        this.values = values;
    }

    /**
     * Constructs a new Transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State} with
     * values given as an integer array
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param values the values as an integer array
     */
    public Transition(State orig, State dest, int[] values) {
        this(orig,dest,IntervalSet.fromIntArray(values));
    }
    /**
     * Constructs a new Transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State} with
     * values given as an {@link fr.menana.automaton.Interval}
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param values the values as an {@link fr.menana.automaton.Interval}
     */
    public Transition(State orig, State dest, Interval values)
    {
        this(orig,dest,IntervalSet.fromInterval(values));
    }

    /**
     * Constructs a new Transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State} with
     * values given as an single integer
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param value the value as an integer
     */
    public Transition(State orig, State dest, int value)
    {
        this(orig,dest,new int[]{value}) ;
    }

    /**
     * Constructs a new epsilon-Transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State} with
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     */
    public Transition(State orig,State dest)
    {
        this(orig,dest,(IntervalSet) null);
        this.epsilon = true;
    }

    /**
     * Checks if this transition carries an epsilon
     * @return
     */
    public boolean hasEpsilon() {
        return this.epsilon;
    }

    /**
     * Adds an epsilon to this transition
     */
    public void addEpsilon() {
        this.epsilon = true;
    }

    /**
     * Returns the list of {@link fr.menana.automaton.Interval} carried by this transition
     * @return
     */
    public List<Interval> getIntervals() {
        if (values != null)
            return new ArrayList<>(this.values.getIntervals());
        else
            return new ArrayList<>(0);
    }


    public String toString() {
        return orig + " -> " + (values != null ?values:"") + (epsilon?"e":"")+" -> "+dest;
    }



}
