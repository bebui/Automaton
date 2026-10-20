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

import fr.menana.automaton.regexp.RegExpParser;
import fr.menana.automaton.internal.GraphTraversal;
import fr.menana.automaton.weighted.MultiWeightedAutomaton;
import fr.menana.automaton.weighted.WeightedAutomaton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.ToDoubleFunction;

/**
 * A finite automaton whose symbols are integers, deterministic or not. <br>
 * Each transition carries a set of integer intervals, and possibly an epsilon. An automaton has at most one initial
 * state and any number of accepting states.
 *
 * <h2>From a regular expression</h2>
 * <pre>{@code
 * Automaton auto = Automaton.dfaFromString("(1|2)*12(1|2)*");
 * auto.run(1, 1, 2);    // true
 * }</pre>
 * See {@link fr.menana.automaton.regexp the regular expression syntax}.
 *
 * <h2>State by state</h2>
 * <pre>{@code
 * Automaton auto = new Automaton();           // one or more digits
 * State s0 = auto.addState();
 * State s1 = auto.addState();
 * auto.setInitial(s0);
 * auto.setAccept(s1);
 * auto.addTransition(s0, s1, new Interval(0, 9));
 * auto.addTransition(s1, s1, new Interval(0, 9));
 * }</pre>
 * Adding an epsilon transition, or two transitions sharing a value from the same state, makes the automaton
 * non-deterministic : {@link #isDeterministic()} tells which kind it is.
 *
 * <h2>Using it</h2>
 * Transformations ({@link #determinize()}, {@link #minimize()}, ...) and boolean operations ({@link #union(Automaton)},
 * {@link #intersection(Automaton)}, ...) return new automata. Questions on the language ({@link #isEmpty()},
 * {@link #sameLanguage(Automaton)}, ...) do not modify the automaton. To read many words, {@link #compile()} it;
 * to search inside sequences, use a {@link #searcher()}. The {@link fr.menana.automaton package overview} lists everything.
 *
 * <h2>Adding costs</h2>
 * {@link #weighted(java.util.function.ToDoubleFunction)} captures scalar costs;
 * {@link #multiWeighted(java.util.function.ToDoubleFunction...)} captures vector costs.
 * Reuse one {@link #snapshot()} for several weightings sharing the exact structure:
 * <pre>{@code
 * AutomatonSnapshot snapshot = auto.snapshot();
 * WeightedAutomaton cost = snapshot.weighted(transition -> 2.0);
 * WeightedAutomaton duration = snapshot.weighted(transition -> 5.0);
 * }</pre>
 * Source mutations do not change captured structure or costs. Weighted objects
 * expose run() for Boolean membership and toAutomaton() to explicitly forget costs.
 * <p>
 * An automaton is mutable and not thread safe.
 *
 * @author Julien Menana
 */
public class Automaton implements Cloneable {


    /**
     * State list of the automaton
     */
    private List<State> states;

    /**
     * index of the initial state in the list
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
     * Constructs the minimal deterministic automaton of a regular expression, see {@link fr.menana.automaton.regexp the regular expression syntax}.
     * Same as {@link #dfaFromString(String)}
     * @param regexp the regular expression
     * @throws IllegalArgumentException if the expression is invalid
     */
    public Automaton(String regexp) {
        Automaton auto = Automaton.dfaFromString(regexp);
        this.states = auto.states;
        this.initIndex = auto.initIndex;
        this.deterministic = auto.deterministic;
    }

    /**
     * Captures one immutable structure for reuse across scalar/vector weightings.
     * Later source mutations do not affect it; original object identities remain metadata.
     * @return structural snapshot with mappings to this automaton's states/transitions
     * @since 4.1.0
     */
    public AutomatonSnapshot snapshot() { return AutomatonSnapshot.of(this); }

    /**
     * @param weights finite scalar transition costs; boundaries default to zero
     * @return independent weighted snapshot of this automaton
     * @since 4.1.0
     */
    public WeightedAutomaton weighted(ToDoubleFunction<Transition> weights) {
        return snapshot().weighted(weights);
    }

    /**
     * @param dimensions one or more finite transition-cost callbacks, in dimension order
     * @return correlated vector costs on one common structural snapshot
     * @since 4.1.0
     */
    @SafeVarargs
    public final MultiWeightedAutomaton multiWeighted(ToDoubleFunction<Transition>... dimensions) {
        return snapshot().multiWeighted(dimensions);
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
        return this.states.stream()
                .flatMap(state -> state.getTransitions().values().stream())
                .collect(Collectors.toList());
    }

    /**
     * Adds a new transition from an origin {@link fr.menana.automaton.State} to a destination {@link fr.menana.automaton.State}. <br>
     * The values are represented by an {@link fr.menana.automaton.IntervalSet}
     * @param orig the origin {@link fr.menana.automaton.State}
     * @param dest the destination {@link fr.menana.automaton.State}
     * @param values the symbols of the transition as an {@link fr.menana.automaton.IntervalSet}, copied by the automaton
     */
    public void addTransition(State orig, State dest, IntervalSet values)
    {
        Transition t = new Transition(orig,dest,values.clone());

        addChecked(t);
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
        addChecked(t);
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

        addChecked(t);
    }

    /**
     * Adds a transition, checking whether it keeps the automaton deterministic. <br>
     * The overlap check with the other transitions of the origin state is skipped once the automaton is non-deterministic
     * @param t the transition to add
     */
    private void addChecked(Transition t) {
        if (deterministic)
            deterministic = !t.orig.addTransition(t);
        else
            t.orig.addToTransitionMap(t);
    }

    /**
     * Adds a transition known not to overlap any other transition leaving <code>orig</code>.
     * Skips the determinism check, the caller must guarantee it
     * @param orig the origin state
     * @param dest the destination state
     * @param values the transition values, owned by the transition afterwards
     */
    void addDisjointTransition(State orig, State dest, IntervalSet values) {
        orig.addToTransitionMap(new Transition(orig, dest, values));
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
        State init = this.getInitial();
        if (init == null)
            return false;
        if (this.deterministic) {
            State current = init;
            for (int symbol : word) {
                current = current.step(symbol);
                if (current == null)
                    return false;
            }
            return current.accept;
        }
        // Simulation of the non deterministic automaton on the set of reachable states
        Set<State> current = epsilonClosure(Collections.singleton(init));
        for (int symbol : word) {
            List<State> next = new ArrayList<>();
            for (State s : current) {
                for (Transition t : s.transitions.values()) {
                    if (t.values != null && t.values.contains(symbol))
                        next.add(t.dest);
                }
            }
            if (next.isEmpty())
                return false;
            current = epsilonClosure(next);
        }
        return State.hasAcceptingState(current);
    }

    /**
     * Computes the set of states reachable from the given states using only epsilon transitions
     * @param states the start states
     * @return the epsilon-closure of the given states
     */
    private static Set<State> epsilonClosure(Collection<State> states) {
        Set<State> out = new HashSet<>(states);
        ArrayDeque<State> toVisit = new ArrayDeque<>(states);
        while (!toVisit.isEmpty()) {
            State s = toVisit.poll();
            for (Transition t : s.transitions.values()) {
                if (t.epsilon && out.add(t.dest))
                    toVisit.add(t.dest);
            }
        }
        return out;
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

    /**
     * Returns a new minimal deterministic automaton recognizing the words accepted by this automaton and by another one
     * @param other the other automaton
     * @return the intersection of both languages
     * @since 2.0.0
     */
    public Automaton intersection(Automaton other) {
        return Operation.intersection(this, other);
    }

    /**
     * Returns a new minimal deterministic automaton recognizing the words accepted by this automaton and not by another one
     * @param other the automaton whose words are removed
     * @return the difference of both languages
     * @since 2.0.0
     */
    public Automaton difference(Automaton other) {
        return Operation.difference(this, other);
    }

    /**
     * Returns an equivalent automaton without epsilon transitions, see {@link Operation#removeEpsilon(Automaton)}
     * @return an equivalent automaton without epsilon transitions
     * @since 2.0.0
     */
    public Automaton removeEpsilon() {
        return Operation.removeEpsilon(this);
    }

    /**
     * Returns an equivalent automaton with fewer states, without determinizing it, see {@link Operation#reduce(Automaton)}
     * @return a smaller or equal equivalent automaton
     * @since 2.0.0
     */
    public Automaton reduce() {
        return Operation.reduce(this);
    }

    /**
     * Compiles this automaton into an immutable deterministic automaton, to read many words quickly. <br>
     * Call {@link #minimize()} first to compile the smallest automaton
     * @return the compiled automaton
     * @since 2.0.0
     */
    public CompiledDfa compile() {
        return CompiledDfa.of(this);
    }

    /**
     * Prepares the search of the words of this language inside sequences of integers
     * @return a reusable searcher
     * @since 2.0.0
     */
    public Searcher searcher() {
        return new Searcher(this);
    }

    /**
     * Returns the leftmost-longest factor of a text that belongs to this language. <br>
     * Builds a new {@link Searcher} on each call : use {@link #searcher()} to search several texts
     * @param text the text
     * @return the match, or <code>null</code> if there is none
     * @since 2.0.0
     */
    public Searcher.Match find(int... text) {
        return searcher().find(text);
    }

    /**
     * Checks if a factor of a text belongs to this language. <br>
     * Builds a new {@link Searcher} on each call : use {@link #searcher()} to search several texts
     * @param text the text
     * @return <code>true</code> if and only if a factor of the text is accepted
     * @since 2.0.0
     */
    public boolean contains(int... text) {
        return searcher().contains(text);
    }

    /**
     * Returns the successive non overlapping leftmost-longest matches of a text. <br>
     * Builds a new {@link Searcher} on each call : use {@link #searcher()} to search several texts
     * @param text the text
     * @return the matches in increasing order
     * @since 2.0.0
     */
    public List<Searcher.Match> findAll(int... text) {
        return searcher().findAll(text);
    }

    /**
     * Checks if this automaton accepts no word
     * @return <code>true</code> if and only if the language is empty
     * @since 2.0.0
     */
    public boolean isEmpty() {
        return Languages.shortestWord(this) == null;
    }

    /**
     * Checks if this automaton accepts finitely many words
     * @return <code>true</code> if and only if the language is finite
     * @since 2.0.0
     */
    public boolean isFinite() {
        return Languages.isFinite(this);
    }

    /**
     * Checks if this automaton accepts every word over the integers
     * @return <code>true</code> if and only if the language contains all words
     * @since 2.0.0
     */
    public boolean isUniversal() {
        return this.complement().isEmpty();
    }

    /**
     * Checks if this automaton and another one recognize the same language, whatever their structure. <br>
     * Uses the algorithm of Hopcroft and Karp on their deterministic automata, in almost linear time.
     * Use {@link #equals(Object)} to compare the structure of two automata
     * @param other the other automaton
     * @return <code>true</code> if and only if both languages are equal
     * @since 2.0.0
     */
    public boolean sameLanguage(Automaton other) {
        return Languages.sameLanguage(this, other);
    }

    /**
     * Checks if every word accepted by this automaton is accepted by another one. <br>
     * Uses antichains, which avoids determinizing the automata
     * @param other the other automaton
     * @return <code>true</code> if and only if the language of this automaton is included in the other one
     * @since 2.0.0
     */
    public boolean isSubsetOf(Automaton other) {
        return Languages.isSubsetOf(this, other);
    }

    /**
     * Returns a shortest accepted word. Among words of the same length, the result is not necessarily the smallest one
     * @return a shortest word, or <code>null</code> if the language is empty
     * @since 2.0.0
     */
    public int[] shortestWord() {
        return Languages.shortestWord(this);
    }

    /**
     * Returns a shortest word accepted by exactly one of this automaton and another one, which helps understanding why
     * two automata differ
     * @param other the other automaton
     * @return a shortest distinguishing word, or <code>null</code> if both languages are equal
     * @since 2.0.0
     */
    public int[] counterExample(Automaton other) {
        return Languages.counterExample(this, other);
    }

    /**
     * Counts the accepted words of a given length
     * @param length the length of the words
     * @return the number of accepted words of this length
     * @since 2.0.0
     */
    public java.math.BigInteger countWords(int length) {
        return Languages.countWords(this, length);
    }

    /**
     * Draws an accepted word of a given length, uniformly among all the accepted words of that length
     * @param length the length of the word
     * @param random the source of randomness
     * @return a random word, or <code>null</code> if no word of this length is accepted
     * @since 2.0.0
     */
    public int[] randomWord(int length, Random random) {
        return Languages.randomWord(this, length, random);
    }

    /**
     * Enumerates lazily the accepted words of length at most <code>maxLength</code>,
     * by increasing length and then in lexicographic order
     * @param maxLength the maximum length of the words
     * @return the accepted words
     * @since 2.0.0
     */
    public Iterable<int[]> words(int maxLength) {
        return Languages.words(this, maxLength);
    }

    /**
     * Removes states that are not reachable from the initial state.
     */
    public void removeDeadStates() {
        Set<State> used = this.getUseFulStates();
        this.states.forEach(s -> s.transitions.keySet().removeIf(next -> !used.contains(next)));
        this.reIndex();
    }

    /**
     * Returns states reaching a final over stored transition-map arcs, including unreachable states.
     * @return a set of useful states
     */
    public Set<State> getUseFulStates() {
        int n = states.size();
        Set<State> useful = new HashSet<>();
        if (n == 0) return useful;
        IdentityHashMap<State, Integer> ids = null;
        for (int s = 0; s < n; s++) if (states.get(s).index != s) {
            ids = new IdentityHashMap<>(n);
            for (int t = 0; t < n; t++) ids.put(states.get(t), t);
            break;
        }
        int[] starts = new int[n + 1];
        for (State s : states) for (State destination : s.transitions.keySet()) {
            int id = structuralStateId(destination, ids);
            if (id >= 0) starts[id + 1]++;
        }
        for (int s = 0; s < n; s++) starts[s + 1] += starts[s];
        int[] predecessors = new int[starts[n]], fill = starts.clone();
        boolean[] reached = new boolean[n]; int[] queue = new int[n]; int tail = 0;
        for (int s = 0; s < n; s++) {
            if (states.get(s).accept) { reached[s] = true; queue[tail++] = s; }
            for (State destination : states.get(s).transitions.keySet()) {
                int id = structuralStateId(destination, ids);
                if (id >= 0) predecessors[fill[id]++] = s;
            }
        }
        GraphTraversal.propagate(starts, null, predecessors, reached, queue, tail, null, null);
        for (int s = 0; s < n; s++) if (reached[s]) useful.add(states.get(s));
        return useful;
    }

    private int structuralStateId(State state, IdentityHashMap<State, Integer> ids) {
        if (ids != null) {
            Integer id = ids.get(state);
            return id == null ? -1 : id;
        }
        if (state == null) return -1;
        int id = state.index;
        return id >= 0 && id < states.size() && states.get(id) == state ? id : -1;
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
        State init = this.getInitial();
        if (init != null) {
            this.states.forEach(s -> s.index = Integer.MAX_VALUE);
            List<State> ordered = new ArrayList<>(this.states.size());
            // Breadth first numbering from the initial state
            init.index = 0;
            ordered.add(init);
            for (int i = 0; i < ordered.size(); ++i) {
                for (State next : ordered.get(i).transitions.keySet()) {
                    if (next.index == Integer.MAX_VALUE) {
                        next.index = ordered.size();
                        ordered.add(next);
                    }
                }
            }
            this.initIndex = 0;
            for (State s : ordered)
                s.transitions.keySet().removeIf(st -> st.index == Integer.MAX_VALUE);
            this.states = ordered;
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
        this.getStates().forEach(s -> {
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
        });
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
        return states.stream()
                .flatMap(s -> s.getTransitions().values().stream())
                .map(Transition::toString)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Checks if the given object is an automaton with the same structure once dead states are removed :
     * same number of states, same accepting states and same transitions between states of same index. <br>
     * Two automata recognizing the same language are not necessarily equal, compare their minimal automata for that
     * @param other the object to compare
     * @return <code>true</code> if and only if both automata have the same structure
     */
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof Automaton))
            return false;
        Automaton auto = ((Automaton) other).withoutDeadStates();
        Automaton orig = this.withoutDeadStates();
        if (orig.getNbStates() != auto.getNbStates())
            return false;
        for (int i = 0; i < orig.getNbStates(); i++)
            if (orig.states.get(i).accept != auto.states.get(i).accept)
                return false;
        Comparator<Transition> byStates = Comparator.<Transition>comparingInt(tr -> tr.orig.index).thenComparingInt(tr -> tr.dest.index);
        List<Transition> myTr = orig.getAllTransitions();
        List<Transition> oTr  = auto.getAllTransitions();
        myTr.sort(byStates);
        oTr.sort(byStates);
        return myTr.equals(oTr);
    }

    @Override
    public int hashCode() {
        Automaton auto = this.withoutDeadStates();
        int h = auto.getNbStates();
        for (State s : auto.states)
            h = 31 * h + (s.accept ? 1 : 0);
        // Order independent, like the comparison of transitions in equals
        int transitions = 0;
        for (Transition tr : auto.getAllTransitions())
            transitions += tr.hashCode();
        return 31 * h + transitions;
    }

    private Automaton withoutDeadStates() {
        Automaton auto = this.clone();
        auto.removeDeadStates();
        return auto;
    }


    /**
     * Constructs a non-deterministic automaton from a regular expression, see {@link fr.menana.automaton.regexp the regular expression syntax}. <br>
     * Uses Thompson's construction, linear in the size of the expression
     * @param regexp a regular expression
     * @return a new non-deterministic automaton recognizing the same language as the regular expression
     * @throws IllegalArgumentException if the expression is invalid
     */
    @SuppressWarnings("unused")
    public static Automaton nfaFromString(String regexp) {
        return RegExpParser.toNFA(regexp);
    }

    /**
     * Constructs the minimal deterministic automaton of a regular expression, see {@link fr.menana.automaton.regexp the regular expression syntax}.
     * @param regexp a regular expression
     * @return a new minimal deterministic automaton recognizing the same language as the regular expression
     * @throws IllegalArgumentException if the expression is invalid
     */
    public static Automaton dfaFromString(String regexp) {
        return RegExpParser.toDFA(regexp);
    }

}
