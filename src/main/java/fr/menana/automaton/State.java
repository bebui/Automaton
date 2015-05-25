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
 * This class represents a state in a finite automaton <br>
 * It is meant to be created via the {@link Automaton#addState()} method <p>
 * Created by Julien Menana on 01/05/2015.
 */
public class State { //implements Comparable<State>{

    /**
     * The index of this state in the {@link fr.menana.automaton.Automaton}
     */
    int index;

    /**
     *  Indicates if this state is the initial state of the {@link fr.menana.automaton.Automaton}
     */
    boolean initial;

    /**
     * Inidicates if this state is an accepting state
     */
    boolean accept;

    /**
     * Structure that map all destination state to its {@link fr.menana.automaton.Transition}
     */
    Map<State,Transition> transitions;

    /**
     * Construts a new state
     */
    State() {
        this.transitions = new HashMap<>();
        this.initial = false;
        this.accept = false;
    }

    /**
     * Check if a given word can be read from this state
     * @param idx the start index in the word
     * @param word the word being read
     * @return <code>true</code> if and only if the word ends on an accepting state
     */
    boolean run(int idx, int[] word) {
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
     * Add a {@link fr.menana.automaton.Transition} to this state
     * @param transition the {@link fr.menana.automaton.Transition} to add
     * @return <code>true</code> if and only if this transition does not contains value already used by other transition from this state
     */
    boolean addTransition(Transition transition)
    {

        boolean intersect = false;
        if (transition.values == null) {
            this.addToTransitionMap(transition);
            return false;
        }
        for (Transition t : transitions.values())
            intersect |= transition.values.intersects(t.values);
        this.addToTransitionMap(transition);
        return intersect;
    }

    void addToTransitionMap(Transition tr) {
        Transition ex = transitions.get(tr.dest);
        if (ex == null) {
            transitions.put(tr.dest, tr);
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
     * Checks if this state is the initial state of the {@link fr.menana.automaton.Automaton}
     * @return <code>true</code> if and only if this state is the initial state of the {@link fr.menana.automaton.Automaton}
     */
    boolean isInitial() {
        return initial;
    }

    /**
     * Checks if this state is an acceptingstate of the {@link fr.menana.automaton.Automaton}
     * @return  <code>true</code> if and only if this state is an accepting state of the {@link fr.menana.automaton.Automaton}
     */
    boolean isAccept() {
        return accept;
    }



    public String toString() {
        return (initial?"Q":"q")+index+(accept?"*":"");
    }

    /**
     * Checks if a set of state contains an accepting state
     * @param states a set of state
     * @return <code>true</code> if and only if at least one of the given state is an accepting state
     */
    static boolean hasAcceptingState(Collection<State> states) {
        for (State s : states)
            if (s.accept)
                return true;
        return false;
    }
    /**
     * Checks if a set of state contains the initial state
     * @param states a set of state
     * @return <code>true</code> if and only if at least one of the given state is the initial state
     */
    static boolean hasInitialState(Collection<State> states) {
        for (State s : states)
            if (s.initial)
                return true;
        return false;
    }

    /**
     * Returns the index of this state in the containing {@link fr.menana.automaton.Automaton}
     * @return the index of this state
     */
    public int getIndex() {
        return index;
    }

    void setInitial() {
        this.initial = true;
    }

  /*  public int compareTo(State other) {
        return new Integer(this.index).compareTo(other.index);
    }  */


    public boolean hasTransitionWith(int value) {
        for (Transition tr : this.transitions.values()) {
            if (tr.values.contains(value))
                return true;
        }
        return false;
    }

    public boolean equals(Object other) {
        if (other == this)
            return true;
        else if (other != null && other instanceof State) {
            State state = (State) other;
            return (state.index == this.index && state.accept == this.accept && state.initial == this.initial);
        }
        return false;
    }


}
