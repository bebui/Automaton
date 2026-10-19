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
import java.util.stream.Collectors;

/**
 * This class represents a state in a finite automaton <br>
 * It is meant to be created via the {@link Automaton#addState()} method
 *
 * @author Julien Menana
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
     * Indicates if this state is an accepting state
     */
    boolean accept;

    /**
     * Structure that map all destination state to its {@link fr.menana.automaton.Transition}.
     * A {@link java.util.LinkedHashMap} keeps the iteration order stable from one run to another
     */
    Map<State,Transition> transitions;

    /**
     * Constructs a new state
     */
    State() {
        this.transitions = new LinkedHashMap<>();
        this.initial = false;
        this.accept = false;
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
        for (Transition t : transitions.values()) {
            if (transition.values.intersects(t.values)) {
                intersect = true;
                break;
            }
        }
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
    public boolean isInitial() {
        return initial;
    }

    /**
     * Checks if this state is an accepting state of the {@link fr.menana.automaton.Automaton}
     * @return  <code>true</code> if and only if this state is an accepting state of the {@link fr.menana.automaton.Automaton}
     */
    public boolean isAccept() {
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
        return states.stream().anyMatch(s -> s.accept);
    }
    /**
     * Checks if a set of state contains the initial state
     * @param states a set of state
     * @return <code>true</code> if and only if at least one of the given state is the initial state
     */
    static boolean hasInitialState(Collection<State> states) {
        return states.stream().anyMatch(s -> s.initial);
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

    Set<State> delta(int value) {
        return this.transitions.values().stream().filter(tr -> tr.values != null && tr.values.contains(value)).map(tr -> tr.dest).collect(Collectors.toSet());
    }
    Set<State> deltaEpsilon() {
        return this.transitions.values().stream().filter(tr -> tr.hasEpsilon()).map(tr -> tr.dest).collect(Collectors.toSet());
    }

    /**
     * Compares this state with the specified state for order.
     * @param other the state to be compared.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    public int compareTo(State other) {
        return Integer.compare(this.index, other.index);
    }

    /**
     * Checks if this state has a transition with the given value.
     * @param value the value to check
     * @return <code>true</code> if and only if this state has a transition with the given value
     */
    public boolean hasTransitionWith(int value) {
        return this.transitions.values().stream().anyMatch(tr -> tr.values != null && tr.values.contains(value));
    }

    /**
     * Returns the destination reached from this state by reading the given value,
     * assuming this state belongs to a deterministic automaton.
     * @param value the value to read
     * @return the destination state, or <code>null</code> if there is none
     */
    State step(int value) {
        for (Transition tr : this.transitions.values()) {
            if (tr.values != null && tr.values.contains(value))
                return tr.dest;
        }
        return null;
    }

    /**
     * Checks if this state and the given one have the same index and the same initial and accepting flags.
     * Used to compare states of two different automata. <br>
     * States themselves use identity equality, which keeps them consistent as keys of hash based collections
     * @param other the other state
     * @return <code>true</code> if and only if both states look the same
     */
    boolean sameAs(State other) {
        return other != null && other.index == this.index && other.accept == this.accept && other.initial == this.initial;
    }


}
