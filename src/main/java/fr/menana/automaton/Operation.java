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
import java.util.stream.Collectors;

/**
 * This class consists of static methods to manipulate a finite {@link fr.menana.automaton.Automaton}.
 * The operation that can be performed are minimization, determinization, union, concatenation, reversion and complement
 * Created by Julien Menana on 01/05/2015.
 */
public class Operation {

    /**
     * Enum of the available minimization algorithms
     * Brzozowski performs in O(2^n) where n is the number of states in the automaton, but has a good average case-complexity
     * Hopcroft performs in O(n s log(n))
     */
    public enum MINIMIZATION_ALGO {
        Brzozowski,
        Hopcroft,
    }

    /**
     * The minimization algorithm to use when {@link fr.menana.automaton.Operation#minimize(Automaton)} is called
     */
    public static MINIMIZATION_ALGO minimization_method = MINIMIZATION_ALGO.Brzozowski;

    /**
     * Returns a new minimal {@link fr.menana.automaton.Automaton} that recognizes the same language as the given {@link fr.menana.automaton.Automaton}
     * The algorithm used is set by the variable {@link fr.menana.automaton.Operation#minimization_method}
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @return an equivalent minimal  automaton
     */
    public static Automaton minimize(Automaton base) {
        return minimize(base, minimization_method);
    }

        /**
     * Returns a new minimal {@link fr.menana.automaton.Automaton} that recognizes the same language as the given {@link fr.menana.automaton.Automaton}
     * The algorithm used is set by the element in the given enum {@link fr.menana.automaton.Operation.MINIMIZATION_ALGO}
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @return an equivalent minimal  automaton
     */
    public static Automaton minimize(Automaton base, MINIMIZATION_ALGO method) {
        switch(method){
            case Brzozowski : return minimizeBrzozowski(base);
            case Hopcroft   : return minimizeHopcroft(base);
            default         : return minimizeBrzozowski(base);
        }
    }

    /**
     * Minimizes the given {@link fr.menana.automaton.Automaton} using Hopcroft algorithm
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @return a minimal {@link fr.menana.automaton.Automaton}
     */
    @SuppressWarnings("unused")
    public static Automaton minimizeHopcroft(Automaton base) {
        throw new UnsupportedOperationException();
    }

        /**
     * Minimizes the given {@link fr.menana.automaton.Automaton} using Brzozowski algorithm
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @return a minimal {@link fr.menana.automaton.Automaton}
     */
    public static Automaton minimizeBrzozowski(Automaton base) {
        Automaton a = base.isDeterministic() ? base : base.determinize();
        return a.revert().determinize().revert().determinize();
    }


    /**
     * Determinizes a given {@link fr.menana.automaton.Automaton} using the subset construction algorithm.
     * If the {@link fr.menana.automaton.Automaton} is already deterministic, return a clone.
     * @param nfa a non-deterministic {@link fr.menana.automaton.Automaton}
     * @return  a minimal {@link fr.menana.automaton.Automaton}
     */
    public static Automaton determinize(Automaton nfa) {
        if (nfa.isDeterministic())
            return nfa.clone();


        if (nfa.getInitial() == null)
            return null;

        Automaton dfa = new Automaton();


        Stack<Set<State>> toVisit = new Stack<>();

        Set<State> start = closure(nfa.getInitial());


        State dfaInit = dfa.addState();
        dfaInit.initial = true;
        dfaInit.accept = State.hasAcceptingState(start);
        dfa.setInitial(dfaInit);
        if (dfaInit.accept)
            dfa.setAccept(dfaInit);

        Map<Set<State>,State> association = new HashMap<>();
        association.put(start, dfaInit);

        toVisit.push(start);
        Set<Set<State>> visited = new HashSet<>();

        while (!toVisit.isEmpty()) {

            Set<State> current = toVisit.pop();
            visited.add(current);
            //System.out.println("CURRENT : " + current);

            Set<Interval> intervals = new TreeSet<>();
            for (State s : current) {
                for (Transition tr : s.transitions.values()) {
                    intervals.addAll(tr.getIntervals());
                }
            }

            List<Interval> intervalAlphabet = getDisjunction(intervals);
            for (Interval i : intervalAlphabet) {
                Set<State> next = move(current, i);
                if (!next.isEmpty()) {
                    State dfaState = association.get(next);
                    if (dfaState == null) {
                        dfaState = dfa.addState();
                        dfaState.accept = State.hasAcceptingState(next);
                        if (dfaState.accept)
                            dfa.setAccept(dfaState);
                        association.put(next, dfaState);
                    }
                    dfa.addTransition(association.get(current), dfaState, i.clone());
                    if (!toVisit.contains(next) && !visited.contains(next))
                        toVisit.push(next);
                }
            }
        }



        return dfa;
    }

    /**
     * Get the minimal ? list of {@link fr.menana.automaton.Interval} representing the same set of values as the given collection of overlapping {@link fr.menana.automaton.Interval}
     * @param intervals a collection of overlapping {@link fr.menana.automaton.Interval}
     * @return a list of non overlapping {@link fr.menana.automaton.Interval}
     */
    private static List<Interval> getDisjunction(Collection<Interval> intervals) {
        boolean somethingDone = true;
        if (intervals.isEmpty())
            somethingDone = false;
        List<Interval> stack = new ArrayList<>(intervals.size());
        stack.addAll(intervals.stream().map(Interval::clone).collect(Collectors.toList()));
        List<Interval> toAdd = new ArrayList<>();
        List<Interval> toRemove = new ArrayList<>();
        int index = 0;
        while(somethingDone) {
            toAdd.clear();
            Interval inter= stack.get(index);
            for (int i = 0 ; i < stack.size() ; ++i) {
                if (i != index) {
                    Interval other = stack.get(i);
                    Interval intersection = inter.intersection(other);
                    if (intersection != null  && !intersection.equals(inter)) {
                        if (!stack.contains(intersection))
                            toAdd.add(intersection);
                        toRemove.add(inter);
                        List<Interval> comp = other.complement();
                        for (Interval o : comp) {
                            Interval compInter = inter.intersection(o);
                            if (compInter != null && !stack.contains(compInter))
                                toAdd.add(compInter);
                        }
                        break;
                    }
                }
            }

            if (!toAdd.isEmpty() || !toRemove.isEmpty() ) {
                stack.removeAll(toRemove);
                stack.addAll(0, toAdd);
                toAdd.clear();
                toRemove.clear();
                index = 0;
            }
            else if (index == stack.size() - 1) {
                somethingDone = false;
            }
            else {
                ++index;
            }
        }
        // Cloning the interval within the result to avoid side effects
        stack = stack.stream().map(Interval::clone).collect(Collectors.toList());

        return stack;

    }

    /**
     * Computes the epsilon-closure of a given {@link fr.menana.automaton.State}
     * @param state the {@link fr.menana.automaton.State} from whom to compute the epsilon-closure
     * @return the epsilon-closure of the given {@link fr.menana.automaton.State}
     */
    private static Set<State> closure(State state) {
        return closure(Collections.singleton(state));
    }

    /**
     * Computes the epsilon-closure of a given collection of {@link fr.menana.automaton.State}
     * @param states the collection of {@link fr.menana.automaton.State} from whom to compute the epsilon-closure
     * @return the epsilon-closure of the given collection of {@link fr.menana.automaton.State}
     */
    private static Set<State> closure(Collection<State> states) {
        Set<State> out = new HashSet<>();
        Set<State> toRem = new HashSet<>();
        out.addAll(states);

        while (true) {
            Set<State> toAdd = new HashSet<>();
            for (State s : out) {
                boolean onlyEpsilon = !s.transitions.isEmpty();
                for (Transition t : s.transitions.values())
                    if (t.hasEpsilon()) {
                        if (!out.contains(t.dest))
                            toAdd.add(t.dest);
                        if (t.values != null && !t.values.isEmpty())
                            onlyEpsilon = false;
                    } else if (t.values != null && !t.values.isEmpty())
                        onlyEpsilon = false;
                if (onlyEpsilon)
                    toRem.add(s);
            }

            if (toAdd.isEmpty())
                break;
            else
                out.addAll(toAdd);
        }
        out.removeAll(toRem);

        return out;


    }

    /**
     * Returns a set of {@link fr.menana.automaton.State} that can be reached from a given collection of {@link fr.menana.automaton.State}
     * using on of the integer value of the given {@link fr.menana.automaton.Interval}
     * @param states a collection of origin {@link fr.menana.automaton.State}
     * @param symbol an {@link fr.menana.automaton.Interval} of integer values
     * @return the set of reached {@link fr.menana.automaton.State}
     */
    private static Set<State> move(Collection<State> states,Interval symbol){
        Set<State> out = new HashSet<>();
        for (State s :states) {
            out.addAll(s.transitions.values().stream().filter(tr -> tr.values != null && tr.values.intersects(symbol)).map(tr -> tr.dest).collect(Collectors.toList()));
        }
        return closure(out);

    }


    /**
     * Returns a new {@link fr.menana.automaton.Automaton} that recognizes the mirror of the language defined by the given {@link fr.menana.automaton.Automaton}
     * @return a reverted {@link fr.menana.automaton.Automaton}
     */
    public static Automaton revert(Automaton a) {
        Map<State, State> map = new HashMap<>();
        Automaton out = new Automaton();
        for (State s : a.getStates()) {
            map.put(s, out.addState());
        }
        for (State s : a.getStates()) {
            for (Transition t : s.transitions.values())
            {
                State d = t.dest;
                out.addTransition(map.get(d),map.get(s),t.values.clone());
            }
        }
        out.setAccept(map.get(a.getInitial()));
        State init = out.addState();
        out.setInitial(init);
        for (State s : a.getAcceptList())
        {
            out.addEpsilonTransition(init,map.get(s));
        }
        return out;
    }

    /**
     * Returns a new automaton recognizing the concatenation of the languages defined by the {@link fr.menana.automaton.Automaton} given as a parameter
     * @param first the first {@link fr.menana.automaton.Automaton} used for the concatenation
     * @param second the second {@link fr.menana.automaton.Automaton} used for the concatenation
     * @return a new automaton recognizing the concatenation of the languages defined by the {@link fr.menana.automaton.Automaton} parameters
     */
    public static Automaton concatenate(Automaton first, Automaton second) {
        Automaton out = first.clone();
        Map<State, State> map = new HashMap<>();
        for (State s : second.getStates()) {
            map.put(s, out.addState());
        }
        for (State s : out.getAcceptList()) {
            out.addEpsilonTransition(s,map.get(second.getInitial()));
            out.setAccept(s, false);
        }
        for (State s : second.getAcceptList()) {
            out.setAccept(map.get(s));
        }
        for (State s : second.getStates()) {
            for (Transition tr : s.getTransitions().values()) {
                if (tr.values != null) {
                    out.addTransition(map.get(tr.orig),map.get(tr.dest),tr.values.clone());
                }
                if (tr.hasEpsilon())
                    out.addEpsilonTransition(map.get(tr.orig),map.get(tr.dest));
            }
        }
        return out;
    }

    /**
     * Returns a new automaton recognizing the union of the languages defined by the {@link fr.menana.automaton.Automaton} given as a parameter
     * @param first the first {@link fr.menana.automaton.Automaton} used for the union
     * @param second the second {@link fr.menana.automaton.Automaton} used for the union
     * @return a new automaton recognizing the union of the languages defined by the {@link fr.menana.automaton.Automaton} parameters
     */
    public static Automaton union(Automaton first, Automaton second) {
        Automaton out = first.clone();
        Map<State, State> map = new HashMap<>();
        for (State s : second.getStates()) {
            map.put(s, out.addState());
        }
        for (State s : second.getStates()) {
            for (Transition tr : s.getTransitions().values()) {
                if (tr.values != null) {
                    out.addTransition(map.get(tr.orig),map.get(tr.dest),tr.values.clone());
                }
                if (tr.hasEpsilon())
                    out.addEpsilonTransition(map.get(tr.orig),map.get(tr.dest));
            }
        }
        for (State s : second.getAcceptList()) {
            out.setAccept(map.get(s));
        }
        State init = out.addState();
        out.addEpsilonTransition(init,out.getInitial());
        out.addEpsilonTransition(init, map.get(second.getInitial()));
        out.setInitial(init);
        return out;
    }

    /**
     * Returns a new automaton that recognizes the complementary language of the given {@link fr.menana.automaton.Automaton}
     * @param automaton the automaton to complement
     * @return a new automaton recognizing the complementary language of the given  {@link fr.menana.automaton.Automaton}
     */
    public static Automaton complement(Automaton automaton) {
        Automaton out = automaton.clone();
        State poubelle = out.addState();
        Map<State,IntervalSet> toAdd = new HashMap<>();
        for (State s : out.getStates()) {
            IntervalSet set = IntervalSet.ALL.clone();
            for (Transition tr : s.transitions.values()) {
                if (tr.values != null) {
                    set = set.intersection(tr.values.complement());
                }
            }
            out.setAccept(s,!s.accept);
            toAdd.put(s,set);
        }
        for (Map.Entry<State,IntervalSet> couple : toAdd.entrySet()) {
            out.addTransition(couple.getKey(),poubelle,couple.getValue());
        }
        out.setAccept(poubelle);
        return out;
    }
}
