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
import java.util.stream.Collectors;

/**
 * Main class to create and manipulate finite automaton. <br>
 * An automaton can be created using primitives of this class or using the regexp parser <p>
 * Created by Julien Menana on 01/05/2015.
 */
public class Automaton implements Cloneable {


    /**
     * State list of the automaton
     */
    private List<State> states;

    /**
     * index of the intial state in the list
     */
    private int initIndex;

    /**
     * <code>true</code> if automaton is deterministic, <code>false</code> otherwise
     */
    private boolean deterministic;


    /**
     *  Constructs an empty automaton
     */
    public Automaton(){
        this.initIndex = -1;
        this.states = new ArrayList<>();
        this.deterministic = true;
    }

    /**
     * Constructs an new automaton from a regular expression.
     * @param regexp the regular expression
     */
    public Automaton(String regexp) {
        Automaton auto = Automaton.dfaFromString(regexp);
        this.states = auto.states;
        this.initIndex = auto.initIndex;
        this.deterministic = auto.deterministic;
    }

    /**
     * Returns the list of the states of the automaton
     * @return a list of {@link fr.menana.automaton.State}
     */
    public List<State> getStates() {
        return states;
    }

    /**
     * Return the state with the given index
     * @param idx the index of the {@link fr.menana.automaton.State} to retrieve
     * @return a {@link fr.menana.automaton.State}
     */
    @SuppressWarnings("unused")
    public State getState(int idx) {
        if (idx >= 0 && idx < this.states.size())
            return this.states.get(idx);
        return null;
    }

    /**
     * Returns the number of states in this automaton
     * @return the number of  {@link fr.menana.automaton.State}
     */
    public int getNbStates() {
        return states.size();
    }


    /**
     * Creates and adds a new State in the automaton
     * @return a newly created {@link fr.menana.automaton.State}
     */
    public State addState() {
        State s = new State();
        this.states.add(s);
        s.index = this.states.size() - 1;
        return s;
    }

    /**
     * Sets given state to be the initial state of the automaton. <br>
     * If a previous state was set initial it is not consider initial anymore. <br>
     * A finite automaton can have only one initial state.
     * @param state the {@link fr.menana.automaton.State} to be set initial
     */
    public void setInitial(State state) {
        if (this.initIndex >= 0) {
            this.states.get(this.initIndex).initial = false;
        }
        this.initIndex = state.index;
        state.setInitial();
    }

    /**
     * Sets a given state to be in the accepting state set of the automaton.
     * @param state a {@link fr.menana.automaton.State} to be set accepting
     */
    public void setAccept(State state) {
        this.setAccept(state, true);
    }

    /**
     * Sets a given state to be in the accepting state set or to be removed from the accepting state set
     * @param state a {@link fr.menana.automaton.State} to be set accepting or non accepting
     * @param accept true if the state is an accepting state, false otherwise
     */
    public void setAccept(State state,boolean accept) {
        state.accept = accept;
    }

    /**
     * Checks whether a state is the initial state of the automaton
     * @param state a {@link fr.menana.automaton.State} to be checked
     * @return  <code>true</code> if and only if the state is the initial state of the automaton, false otherwise
     */
    @SuppressWarnings("unused")
    public boolean isInitial(State state) {
        return state.index == this.initIndex;
    }

    /**
     * Returns the initial state of the automaton
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
        return this.states.stream().filter(State::isAccept).collect(Collectors.toList());
    }
    /**
     * Constructs and returns a list containing all the non-accepting states
     * @return a list of non-accepting {@link fr.menana.automaton.State}
     */
    public List<State> getNonAcceptList() {
        return this.states.stream().filter(s -> !s.isAccept()).collect(Collectors.toList());
    }

    /**
     * Constructs and returns a list containing all transition in the automaton.
     * @return a list of all transition in the automaton
     */
    public List<Transition> getAllTransitions() {
        Set<Transition> transitions = new HashSet<>();
        for (State state : this.states) {
            transitions.addAll(state.getTransitions().values().stream().collect(Collectors.toList()));
        }
        return transitions.stream().collect(Collectors.toList());
    }

    /**
     * Adds a new transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State}. <br>
     * The values are represented by an {@link fr.menana.automaton.IntervalSet}
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
     * Adds a new transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State}. <br>
     * The values are represented by an {@link fr.menana.automaton.Interval}
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
     * Adds a new transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State}. <br>
     * The values are represented by an array of int
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param values the symbols of the transition as an array of int
     */
    public void addTransition(State orig, State dest, int... values) {
        Transition t = new Transition(orig,dest,values);

        deterministic &= !orig.addTransition(t);
    }

    /**
     * Adds a new epsilon transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State}. <br>
     * As soon as such a transition is added, the automaton becomes a NFA
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     */
    public void addEpsilonTransition(State orig,State dest) {
        Transition t = new Transition(orig,dest);
        orig.addTransition(t);
        deterministic = false;
    }

    /**
     * Checks whether this automaton is deterministic or not
     * @return  <code>true</code> if and only if the automaton is deterministic
     */
    public boolean isDeterministic() {
        return deterministic;
    }

    /**
     * Checks if a word given as an int array is a word in the language defined by this automaton
     * @param word a word as an int array
     * @return <code>true</code> if and only if the word belongs to the language of the automaton
     */
    @SuppressWarnings("unused")
    public boolean run(int... word)
    {
        return this.getInitial().run(0, word);
    }

    /**
     * Calls {@link fr.menana.automaton.Operation#determinize(Automaton)} on this automaton
     * @see fr.menana.automaton.Operation#determinize(Automaton)
     * @return a new automaton deterministic automaton
     */
    public Automaton determinize()
    {
        return Operation.determinize(this);
    }


    /**
     * Calls {@link fr.menana.automaton.Operation#minimize(Automaton)} on this automaton
     * @see fr.menana.automaton.Operation#minimize(Automaton)
     * @return a new automaton resulting of the minimization
     */
    public Automaton minimize() {
        return Operation.minimize(this);
    }

    /**
     * Calls {@link fr.menana.automaton.Operation#revert(Automaton)} on this automaton
     * @see fr.menana.automaton.Operation#revert(Automaton)
     * @return a new automaton resulting of the reversion
     */
    public Automaton revert() {
        return Operation.revert(this);
    }


    /**
     * Calls {@link fr.menana.automaton.Operation#concatenate(Automaton, Automaton)} on this automaton and the given one
     * @see fr.menana.automaton.Operation#concatenate(Automaton, Automaton)
     * @param other the automaton to append
     * @return a new automaton resulting of the concatenation
     */
    public Automaton concatenate(Automaton other) {
        return Operation.concatenate(this, other);}

    /**
     * Calls {@link fr.menana.automaton.Operation#union(Automaton, Automaton)} on this automaton and the given one
     * @see fr.menana.automaton.Operation#union(Automaton, Automaton)
     * @param other the automaton the union is performed with
     * @return a new automaton resulting of the union
     */
    @SuppressWarnings("unused")
    public Automaton union(Automaton other) {
        return Operation.union(this, other);}

    /**
     * Calls {@link fr.menana.automaton.Operation#complement(Automaton)} on this automaton
     * @see fr.menana.automaton.Operation#complement(Automaton)
     * @return a new automaton recognizing the complementary language of this automaton
     */
    @SuppressWarnings("unused")
    public Automaton complement() { return Operation.complement(this);}

    public void removeDeadStates() {
        Set<State> used = this.getUseFulStates();
      //  System.out.println(this);
        for (State s : this.states) {
            for (Iterator<State> it = s.transitions.keySet().iterator() ; it.hasNext();) {
                State next = it.next();
               // System.out.println("St: "+next+" set: "+used+" contains ? "+used.contains(next));
                if (!used.contains(next)) {
               //     System.out.println("REM");
                    it.remove();
                }
            }
        }
       // System.out.println("DEADSTATE : "+this);
        this.reIndex();
      //  System.out.println("REINDEX : "+this);
    }

    public Set<State> getUseFulStates() {
        if (this.getNbStates() == 0)
            return new HashSet<>();
        else {
            Set<State> useful = new HashSet<>();
            Stack<State> toTest = new Stack<>();
            Map<State,Set<Transition>> reverse = new HashMap<>();
            List<Transition> trs = this.getAllTransitions();
            for (Transition tr : trs) {
                if (!reverse.containsKey(tr.dest)) {
                    reverse.put(tr.dest,new HashSet<>());
                }
                reverse.get(tr.dest).add(tr);
            }
            toTest.addAll(this.getAcceptList().stream().collect(Collectors.toList()));
            while (!toTest.isEmpty()) {
                State s = toTest.pop();
                useful.add(s);
                if (reverse.containsKey(s)) {
                    for (Iterator<Transition> it = reverse.get(s).iterator(); it.hasNext(); ) {
                        Transition tmp = it.next();
                        if (tmp != null) {
                            toTest.push(tmp.orig);
                            it.remove();
                        }
                    }
                }

            }
            return useful;
        }

    }


    @Override
    public Automaton clone() {

        Automaton clone = null;
        try {
            clone = (Automaton) super.clone();
            clone.states = new ArrayList<>(this.states.size());
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
            System.err.println(e+"Should not happen !");
        }

        return clone;


    }

    /**
     * Returns the set of {@link fr.menana.automaton.State} that can be reached from a given {@link fr.menana.automaton.State} with transition of the given value <br>
     * The set has only one element if the automaton is deterministic
     * @param orig The {@link fr.menana.automaton.State} from which to compute the reachable {@link fr.menana.automaton.State}
     * @param value The value taken by the transiton
     * @return a set of {@link fr.menana.automaton.State} reachable from the given {@link fr.menana.automaton.State} using the value transition.
     */
    @SuppressWarnings("unused")
    public Set<State> delta(State orig,int value) {
        return orig.delta(value);
    }

    /**
     * Returns the set of {@link fr.menana.automaton.State} that can be reached from a given {@link fr.menana.automaton.State} with an epsilon transition
     * @param orig The {@link fr.menana.automaton.State} from which to compute the reachable {@link fr.menana.automaton.State}
     * @return a set of {@link fr.menana.automaton.State} reachable from the given {@link fr.menana.automaton.State} using the epsilon transition.
     */
    @SuppressWarnings("unused")
    public Set<State> deltaEpsilon(State orig) {
        return orig.deltaEpsilon();
    }


    /**
     * Re-indexes the automaton state indexes. <br>
     * The initial state is indexed at 0  <br>
     * Removes non connected states
     */
    public void reIndex() {
        if (this.getNbStates() != 0) {

            ArrayDeque<State> toVisit = new ArrayDeque<>();
            Set<State> visited = new HashSet<>();
            toVisit.push(this.getInitial());
            for (State s : this.states) {
                s.index = Integer.MAX_VALUE;
            }
            int idx = 0;
            while (!toVisit.isEmpty()) {
                State s = toVisit.poll();
                if (!visited.contains(s)) {
                    visited.add(s);
                    s.index = idx++;
                    Set<State> nexts = new HashSet<>(s.getTransitions().keySet());
                    nexts.removeAll(visited);
                    toVisit.addAll(nexts);
                }

            }
            this.initIndex = 0;
            //System.out.println(this.states);
            Collections.sort(this.states, (o1, o2) -> new Integer(o1.index).compareTo(o2.index));
            for (State s : this.states)
                s.transitions.keySet().removeIf(st -> st.index == Integer.MAX_VALUE);

            this.states.removeIf(s -> s.index == Integer.MAX_VALUE);
        }

    }

    /**
     * Generates a Graphviz dotty file that pictures the current automaton
     * @param filename the filename
     */
    @SuppressWarnings("unused")
    public void toDotty(String filename) {
        String s = this.toDot();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
            bw.write(s);
            bw.close();
        } catch (IOException e) {
            System.err.println("Unable to write dotty file " + filename);
        }
    }

    /**
     * Returns a {@link java.lang.String} representing the automaton in dot format
     * @return a {@link java.lang.String} in Graphviz dot format
     */
    public String toDot() {
        StringBuilder b = new StringBuilder("digraph Automaton {\n");
        b.append(" rankdir = LR;\n");
        List<State> states = this.getStates();
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
        StringBuilder buffer = new StringBuilder();
        for (State s : states) {
            for (Transition t : s.transitions.values())
                buffer.append(t).append("\n");
        }
        return buffer.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || ! (other instanceof Automaton))
            return false;
        Automaton auto = ((Automaton) other).clone();
        Automaton orig = this.clone();
        auto.removeDeadStates();
        orig.removeDeadStates();
        //auto.toDotty("auto.dot");
        //orig.toDotty("orig.dot");
        if (orig.getNbStates() != auto.getNbStates())
            return false;
        List<Transition> myTr = orig.getAllTransitions();
        List<Transition> oTr  = auto.getAllTransitions();
        return myTr.equals(oTr);
    }


    /**
     * Constucts an NFA from a regexp given as a {@link java.lang.String}
     * @param regexp a regular expression
     * @return a new non-deterministic automaton recognizing the same language as the regular expression
     */
    @SuppressWarnings("unused")
    public static Automaton nfaFromString(String regexp) {
        return RegExpParser.toNFA(regexp);
    }

    /**
     * Constucts an DFA from a regexp given as a {@link java.lang.String}
     * @param regexp a regular expression
     * @return a new deterministic automaton recognizing the same language as the regular expression
     */
    public static Automaton dfaFromString(String regexp) {
        return RegExpParser.toDFA(regexp);
    }

}
