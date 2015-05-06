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

import fr.menana.automaton.regexp.RegExpParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Main class to create and manipulate finite automaton.
 * An automaton can be created using primitives of this class or using the regexp parser
 * Created by Julien Menana on 01/05/2015.
 */
public class Automaton implements Cloneable {


    /**
     * State list of the {@link fr.menana.automaton.Automaton}
     */
    private List<State> states;

    /**
     * index of the intial state in the list
     */
    private int initIndex;

    /**
     * indexes of the accepting states
     */
    private BitSet acceptIndexes;

    /**
     * <code>true</code> if automaton is deterministic, <code>false</code> otherwise
     */
    private boolean deterministic;


    /**
     *  Constructs an empty {@link fr.menana.automaton.Automaton}
     */
    public Automaton(){
        this.initIndex = -1;
        this.states = new ArrayList<>();
        this.acceptIndexes = new BitSet();
        this.deterministic = true;
    }

    /**
     * Constructs an new {@link fr.menana.automaton.Automaton} from a regular expression.
     * @param regexp the regular expression
     */
    public Automaton(String regexp) {
        Automaton auto = Automaton.dfaFromString(regexp);
        this.states = auto.states;
        this.initIndex = auto.initIndex;
        this.deterministic = auto.deterministic;
        this.acceptIndexes = auto.acceptIndexes;
    }

    /**
     * Returns the list of the states of the {@link fr.menana.automaton.Automaton}
     * @return a list of {@link fr.menana.automaton.State}
     */
    public List<State> getStates() {
        return states;
    }


    /**
     * Creates and adds a new State in the {@link fr.menana.automaton.Automaton}
     * @return a newly created {@link fr.menana.automaton.State}
     */
    public State addState() {
        State s = new State();
        this.states.add(s);
        s.index = this.states.size() - 1;
        return s;
    }

    /**
     * Sets given state to be the initial state of the {@link fr.menana.automaton.Automaton}.
     * If a previous state was set initial it is not consider initial anymore.
     * A finite {@link fr.menana.automaton.Automaton} can have only one initial state.
     * @param state the {@link fr.menana.automaton.State} to be set initial
     */
    public void setInitial(State state) {
        if (this.initIndex >= 0) {
            this.states.get(this.initIndex).initial = false;
        }
        this.initIndex = state.index;
        state.initial = true;
    }

    /**
     * Sets a given state to be in the accepting state set of the {@link fr.menana.automaton.Automaton}.
     * @param state a {@link fr.menana.automaton.State} to be set accepting
     */
    public void setAccept(State state) {
        this.setAccept(state,true);
    }

    /**
     * Sets a given state to be in the accepting state set or to be removed from the accepting state set
     * @param state a {@link fr.menana.automaton.State} to be set accepting or non accepting
     * @param accept true if the state is an accepting state, false otherwise
     */
    public void setAccept(State state,boolean accept) {
        this.acceptIndexes.set(state.index, accept);
        state.accept = accept;
    }

    /**
     * Checks whether a state is the initial state of the {@link fr.menana.automaton.Automaton}
     * @param state a {@link fr.menana.automaton.State} to be checked
     * @return  <code>true</code> if and only if the state is the initial state of the automaton, false otherwise
     */
    public boolean isInitial(State state) {
        return state.index == this.initIndex;
    }

    /**
     * Returns the initial state of the {@link fr.menana.automaton.Automaton}
     * @return a {@link fr.menana.automaton.State}
     */
    public State getInitial() {
        if (this.initIndex >= 0)
            return this.states.get(this.initIndex);
        return null;
    }

    /**
     * Constructs and return a list containing all the accepting states
     * @return a list of accepting {@link fr.menana.automaton.State}
     */
    public List<State> getAcceptList() {
        List<State> liste = new ArrayList<State>(this.acceptIndexes.cardinality());
        for (int i = this.acceptIndexes.nextSetBit(0) ; i >= 0 ; i = this.acceptIndexes.nextSetBit(i+1)) {
            liste.add(this.states.get(i));
        }
        return liste;
    }

    /**
     * Adds a new transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State}
     * the values are represented by an {@link fr.menana.automaton.IntervalSet}
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param values the symbols of the transition as an {@link fr.menana.automaton.IntervalSet}
     */
    public void addTransition(State orig, State dest, IntervalSet values)
    {
        Transition t = new Transition(orig,dest,values);

        deterministic &= !orig.addTransition(t);
    }

    /**
     * Adds a new transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State}
     * the values are represented by an {@link fr.menana.automaton.Interval}
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param values the symbols of the transition as an {@link fr.menana.automaton.Interval}
     */
    public void addTransition(State orig, State dest, Interval values)
    {
        Transition t = new Transition(orig,dest,values);
        deterministic &= !orig.addTransition(t);
    }

    /**
     * Adds a new transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State}
     * the values are represented by an array of int
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param values the symbols of the transition as an array of int
     */
    public void addTransition(State orig, State dest, int... values) {
        Transition t = new Transition(orig,dest,values);

        deterministic &= !orig.addTransition(t);
    }

    /**
     * Adds a new epsilon transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State}
     * As soon as such a transition is added, the {@link fr.menana.automaton.Automaton} becomes a NFA
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     */
    public void addEpsilonTransition(State orig,State dest) {
        Transition t = new Transition(orig,dest);
        orig.addTransition(t);
        deterministic = false;
    }

    /**
     * Checks whether this {@link fr.menana.automaton.Automaton} is deterministic or not
     * @return  <code>true</code> if and only if the automaton is deterministic
     */
    public boolean isDeterministic() {
        return deterministic;
    }

    /**
     * Checks if a word given as an int array is a word in the language defined by this automaton
     * @param word a word as an int array
     * @return <code>true</code> if and only if the word belongs to the language of the {@link fr.menana.automaton.Automaton}
     */
    public boolean run(int... word)
    {
        return this.getInitial().run(0, word);
    }

    /**
     * Returns a new deterministic {@link fr.menana.automaton.Automaton} that recognizes the same language as this {@link fr.menana.automaton.Automaton}
     * If this {@link fr.menana.automaton.Automaton} is already deterministic, returns a clone of this {@link fr.menana.automaton.Automaton}
     * @return an equivalent deterministic {@link fr.menana.automaton.Automaton}
     */
    public Automaton determinize()
    {
        return Operation.determinize(this);
    }

    /**
     * Returns a new minimal {@link fr.menana.automaton.Automaton} that recognizes the same language as this {@link fr.menana.automaton.Automaton}
     * The algorithm used is set by the variable {@link fr.menana.automaton.Operation.MINIMIZATION_ALGO}
     * @return an equivalent minimal  {@link fr.menana.automaton.Automaton}
     */
    public Automaton minimize() {
        return Operation.minimize(this);
    }

    /**
     * Returns a new {@link fr.menana.automaton.Automaton} that recognizes the mirror language defined by this {@link fr.menana.automaton.Automaton}
     * @return a reverted {@link fr.menana.automaton.Automaton}
     */
    public Automaton revert() {
        return Operation.revert(this);
    }

    /**
     * Returns a new {@link fr.menana.automaton.Automaton} recognizing the concatenation of the languages defined by the current {@link fr.menana.automaton.Automaton}
     * and the {@link fr.menana.automaton.Automaton} parameter
     * The returned {@link fr.menana.automaton.Automaton} may no be deterministic
     * @param other the {@link fr.menana.automaton.Automaton} to be concatenated
     * @return a new {@link fr.menana.automaton.Automaton}
     */
    public Automaton concatenate(Automaton other) { return Operation.concatenate(this, other);}

    /**
     * Returns a new {@link fr.menana.automaton.Automaton} recognizing the union of the languages defined by the current {@link fr.menana.automaton.Automaton}
     * and the {@link fr.menana.automaton.Automaton} given as a parameter
     * @param other the {@link fr.menana.automaton.Automaton} used for the union
     * @return a new {@link fr.menana.automaton.Automaton}
     */
    public Automaton union(Automaton other) { return Operation.union(this, other);}

    /**
     * Returns a new {@link fr.menana.automaton.Automaton} that recognizes the complementary language of the current {@link fr.menana.automaton.Automaton}
     * @return a new {@link fr.menana.automaton.Automaton}
     */
    public Automaton complement() { return Operation.complement(this);}

    @Override
    public Automaton clone() {

        Automaton clone = null;
        try {
            clone = (Automaton) super.clone();
            clone.states = new ArrayList<State>(this.states.size());
            clone.acceptIndexes = (BitSet)this.acceptIndexes.clone();
            clone.deterministic = this.deterministic;
            clone.initIndex = this.initIndex;
            for (int i = 0 ; i < this.states.size(); ++i)
                clone.addState();
            for (State s : this.states) {
                int idx = s.index;
                State newS = clone.states.get(idx);
                if (s.initial)
                    clone.setInitial(newS);
                if (s.accept)
                    clone.setAccept(newS);
                for (Transition t : s.transitions.values()) {
                    Transition nt;
                    if (t.values != null)
                        nt = new Transition(newS,clone.states.get(t.dest.index),t.values.clone());
                    else
                        nt = new Transition(newS,clone.states.get(t.dest.index));
                    nt.epsilon = t.epsilon;
                    newS.transitions.put(nt.dest, nt);
                }
            }

        } catch (CloneNotSupportedException e) {
            System.err.println("ne devrait pas arriver");
        }

        return clone;


    }

    /**
     * Generates a Graphviz dotty file that pictures the current {@link fr.menana.automaton.Automaton}
     * @param filename the filename
     */
    public void toDotty(String filename) {
        String s = this.toDot();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
            bw.write(s);
            bw.close();
        } catch (IOException e) {
// System.err.println("Unable to write dotty file " + f);
        }
    }

    /**
     * Returns a {@link java.lang.String} representing the {@link fr.menana.automaton.Automaton} in dot format
     * @return a {@link java.lang.String} in Graphviz dot format
     */
    public String toDot() {
        StringBuilder b = new StringBuilder("digraph Automaton {\n");
        b.append(" rankdir = LR;\n");
        List<State> states = this.getStates();
// setStateNumbers(states);
        for (State s : states) {
            int idx = s.getIndex();
            b.append(" ").append(idx);
            if (s.isAccept())
                b.append(" [shape=doublecircle];\n");
            else
                b.append(" [shape=circle];\n");
            if (s.isInitial()) {
                b.append(" initial [shape=plaintext,label=\"\"];\n");
                b.append(" initial -> ").append(idx).append("\n");
            }
            for (Transition t : s.getTransitions().values()) {
                b.append(" ").append(idx);
                appendDot(t, b);
            }
        }
        return b.append("}\n").toString();
    }


    private void appendDot(Transition t, StringBuilder b) {
        int destIdx = t.dest.getIndex();
        b.append(" -> ").append(destIdx).append(" [label=\"");
        if (t.values == null && t.hasEpsilon())
            b.append("{eps}");
        else {
            b.append("{");
                for (Interval i : t.getIntervals()) {
                    b.append(i);
                    b.append(",");
                }
            if (b.charAt(b.length()-1) == ',')
                b.deleteCharAt(b.length()-1);

            b.append("}");

        }
        b.append("\"]\n");
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        for (State s : states) {
            for (Transition t : s.transitions.values())
                buffer.append(t).append("\n");
        }
        return buffer.toString();
    }

    /**
     * Constucts an NFA from a regexp given as a {@link java.lang.String}
     * @param regexp a regular expression
     * @return a new non-deterministic {@link fr.menana.automaton.Automaton} recognizing the same language as the regular expression
     */
    public static Automaton nfaFromString(String regexp) {
        return new RegExpParser(regexp).parse().toNFA();
    }

    /**
     * Constucts an DFA from a regexp given as a {@link java.lang.String}
     * @param regexp a regular expression
     * @return a new deterministic {@link fr.menana.automaton.Automaton} recognizing the same language as the regular expression
     */
    public static Automaton dfaFromString(String regexp) {
        return nfaFromString(regexp).minimize();
    }
}
