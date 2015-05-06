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

    public State orig;
    public State dest;
    public IntervalSet values;
    boolean epsilon;

    public Transition(State orig, State dest, IntervalSet values)
    {
        this.orig = orig;
        this.dest = dest;
        this.values = values;
    }

    public Transition(State orig, State dest, int[] values)
    {
        this(orig,dest,IntervalSet.fromIntArray(values));
    }
    public Transition(State orig, State dest, Interval values)
    {
        this(orig,dest,IntervalSet.fromInterval(values));
    }

    public Transition(State orig, State dest, int value)
    {
        this(orig,dest,new int[]{value}) ;
    }

    public Transition(State orig,State dest)
    {
        this(orig,dest,(IntervalSet) null);
        this.epsilon = true;
    }

    public boolean hasEpsilon() {
        return this.epsilon;
    }

    public void addEpsilon() {
        this.epsilon = true;
    }

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
