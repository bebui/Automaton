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
import java.util.Objects;

/**
 * This class represents a transition carrying values and/or epsilon from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State} <br>
 * Created by Julien Menana on 01/05/2015.
 */
public class Transition {

    /**
     * The origin {@link fr.menana.automaton.State} of this transition
     */
    State orig;

    /**
     * The destination {@link fr.menana.automaton.State} of this transition
     */
    State dest;

    /**
     * the values carried by this transition represented as an {@link fr.menana.automaton.IntervalSet},
     * <code>null</code> for a pure epsilon transition
     */
    IntervalSet values;

    /**
     * Inidicates if this transition carries an epsilon
     */
    boolean epsilon;

    /**
     * Constructs a new transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State} with
     * values given as an {@link fr.menana.automaton.IntervalSet}
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param values the values as an {@link fr.menana.automaton.IntervalSet}
     */
     Transition(State orig, State dest, IntervalSet values) {
        this.orig = orig;
        this.dest = dest;
        this.values = values;
    }

    /**
     * Constructs a new transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State} with
     * values given as an integer array
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param values the values as an integer array
     */
    Transition(State orig, State dest, int[] values) {
        this(orig,dest,IntervalSet.fromIntArray(values));
    }
    /**
     * Constructs a new transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State} with
     * values given as an {@link fr.menana.automaton.Interval}
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param values the values as an {@link fr.menana.automaton.Interval}
     */
    Transition(State orig, State dest, Interval values)
    {
        this(orig,dest,IntervalSet.fromInterval(values));
    }

    /**
     * Constructs a new transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State} with
     * values given as an single integer
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param value the value as an integer
     */
    @SuppressWarnings("unused")
    Transition(State orig, State dest, int value)
    {
        this(orig,dest,new int[]{value}) ;
    }

    /**
     * Constructs a new epsilon-transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State} with
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     */
    Transition(State orig,State dest)
    {
        this(orig,dest,(IntervalSet) null);
        this.epsilon = true;
    }

    /**
     * Returns the origin {@link fr.menana.automaton.State} of this transition
     * @return the origin state
     */
    public State getOrig() {
        return orig;
    }

    /**
     * Returns the destination {@link fr.menana.automaton.State} of this transition
     * @return the destination state
     */
    public State getDest() {
        return dest;
    }

    /**
     * Returns a copy of the values carried by this transition.
     * Modifying the returned set does not modify the automaton.
     * @return the values, or <code>null</code> if this transition only carries an epsilon
     */
    public IntervalSet getValues() {
        return values == null ? null : values.clone();
    }

    /**
     * Checks if this transition carries values, as opposed to a pure epsilon transition
     * @return <code>true</code> if and only if the transition carries at least one value
     */
    public boolean hasValues() {
        return values != null && !values.isEmpty();
    }

    /**
     * Checks if this transition can be taken by reading the given value
     * @param value the value to read
     * @return <code>true</code> if and only if the transition carries the value
     */
    public boolean accepts(int value) {
        return values != null && values.contains(value);
    }

    /**
     * Checks if this transition carries an epsilon
     * @return <code>true</code> if and only if the transition carries an epsilon
     */
    public boolean hasEpsilon() {
        return this.epsilon;
    }

    /**
     * Adds an epsilon to this transition
     */
    @SuppressWarnings("unused")
    void addEpsilon() {
        this.epsilon = true;
    }

    /**
     * Returns the list of {@link fr.menana.automaton.Interval} carried by this transition
     * @return the list of {@link fr.menana.automaton.Interval} carried by this transition
     */
    List<Interval> getIntervals() {
        return values != null ? new ArrayList<>(this.values.getIntervals()) : new ArrayList<>(0);
    }

    public boolean equals(Object other) {
        if (other == null || !(other instanceof Transition))
            return false;
        Transition tr = (Transition) other;
        return tr.orig.sameAs(this.orig) && tr.dest.sameAs(this.dest) && tr.epsilon == this.epsilon && Objects.equals(tr.values, this.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orig.index, dest.index, epsilon, values);
    }


    public String toString() {
        return orig + " -> " + (values != null ?values:"") + (epsilon?"e":"")+" -> "+dest;
    }

}
