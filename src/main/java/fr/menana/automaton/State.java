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

    /**
     * The index of this {@link fr.menana.automaton.State} in the {@link fr.menana.automaton.Automaton}
     */
    int index;

    /**
     *  Indicates if this {@link fr.menana.automaton.State} is the initial state of the {@link fr.menana.automaton.Automaton}
     */
    boolean initial;

    /**
     * Inidicates if this {@link fr.menana.automaton.State} is an accepting state
     */
    boolean accept;

    /**
     * Structure that map all destination {@link fr.menana.automaton.State} to its {@link fr.menana.automaton.Transition}
     */
    Map<State,Transition> transitions;

    /**
     * Construts a new {@link fr.menana.automaton.State}
     */
    public State() {
        this.transitions = new HashMap<State, Transition>();
        this.initial = false;
        this.accept = false;
    }

    /**
     * Check if a given word can be read from this {@link fr.menana.automaton.State}
     * @param idx the start index in the word
     * @param word the word being read
     * @return <code>true</code> if and only if the word ends on an accepting {@link fr.menana.automaton.State}
     */
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

    /**
     * Add a {@link fr.menana.automaton.Transition} to this {@link fr.menana.automaton.State}
     * @param transition the {@link fr.menana.automaton.Transition} to add
     * @return <code>true</code> if and only if this transition does not contains value already used by other transition from this state
     */
    public boolean addTransition(Transition transition)
    {

        boolean intersect = false;
        if (transition.values == null) {
            this.addToTransitionMap(transition);
            return intersect;
        }
        for (Transition t : transitions.values())
            intersect |= transition.values.intersects(t.values);
        this.addToTransitionMap(transition);
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

    /**
     * Returns the map of {@link fr.menana.automaton.Transition}
     * @return a map of {@link fr.menana.automaton.Transition}
     */
    public Map<State,Transition> getTransitions() {
        return transitions;
    }

    /**
     * Checks if this {@link fr.menana.automaton.State} is the initial {@link fr.menana.automaton.State} of the {@link fr.menana.automaton.Automaton}
     * @return <code>true</code> if and only if this {@link fr.menana.automaton.State} is the initial {@link fr.menana.automaton.State} of the {@link fr.menana.automaton.Automaton}
     */
    public boolean isInitial() {
        return initial;
    }

    /**
     * Checks if this {@link fr.menana.automaton.State} is an accepting{@link fr.menana.automaton.State} of the {@link fr.menana.automaton.Automaton}
     * @return  <code>true</code> if and only if this {@link fr.menana.automaton.State} is an accepting {@link fr.menana.automaton.State} of the {@link fr.menana.automaton.Automaton}
     */
    public boolean isAccept() {
        return accept;
    }



    public String toString() {
        return (initial?"Q":"q")+index+(accept?"*":"");
    }

    /**
     * Checks if a set of {@link fr.menana.automaton.State} contains an accepting state
     * @param states a set of {@link fr.menana.automaton.State}
     * @return <code>true</code> if and only if at least one of the given {@link fr.menana.automaton.State} is an accepting state
     */
    static boolean hasAcceptingState(Collection<State> states) {
        for (State s : states)
            if (s.accept)
                return true;
        return false;
    }

    /**
     * Returns the index of this {@link fr.menana.automaton.State} in the containing {@link fr.menana.automaton.Automaton}
     * @return the index of this {@link fr.menana.automaton.State}
     */
    public int getIndex() {
        return index;
    }
}
