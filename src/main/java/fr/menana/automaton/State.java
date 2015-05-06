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
public class State  {

    int index;
    boolean initial;
    boolean accept;

    Map<State,Transition> transitions;

    public State() {
        this.transitions = new HashMap<State, Transition>();
        this.initial = false;
        this.accept = false;
    }

    public boolean run(int idx, int[] word) {
        if (idx == word.length)
            return this.accept;
        int symbol = word[idx];
        boolean ret = false;
        for (Transition t : transitions.values()) {
            if (t.values == null) {
                ret |= t.dest.run(idx,word);
            }
            else if (t.values.contains(symbol)) {
                ret |= t.dest.run(idx+1,word);
            }
        }
        return ret;
    }

    public boolean addTransition(Transition tr)
    {

        boolean intersect = false;
        if (tr.values == null) {
            this.addToTransitionMap(tr);
            return intersect;
        }
        for (Transition t : transitions.values())
            intersect |= tr.values.intersects(t.values);
        this.addToTransitionMap(tr);
        return intersect;
    }

    private void addToTransitionMap(Transition tr) {
        Transition ex = transitions.get(tr.dest);
        if (ex == null) {
            transitions.put(tr.dest, tr);
            ex = tr;
        }

        else {
            if (ex.values == null)
                ex.values = tr.values;
            else
                ex.values.add(tr.values);

            ex.epsilon |= tr.epsilon;
        }
    }

    public Map<State,Transition> getTransitions() {
        return transitions;
    }
    public boolean isInitial() {
        return initial;
    }
    public boolean isAccept() {
        return accept;
    }



    public String toString() {
        return (initial?"Q":"q")+index+(accept?"*":"");
    }

    public static boolean hasAcceptingState(Collection<State> states) {
        for (State s : states)
            if (s.accept)
                return true;
        return false;
    }


    public int getIndex() {
        return index;
    }
}
