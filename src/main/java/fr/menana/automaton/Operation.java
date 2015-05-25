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
 * This class consists of static methods to manipulate a finite {@link fr.menana.automaton.Automaton}. <br>
 * The operation that can be performed are minimization, determinization, union, concatenation, reversion and complement <br>
 * Created by Julien Menana on 01/05/2015.
 */
public class Operation {

    /**
     * Enum of the available minimization algorithms <br>
     * Brzozowski performs in O(2^n) where n is the number of states in the automaton, but has a good average case-complexity <br>
     * Hopcroft performs in O(n s log(n))
     */
    public enum MINIMIZATION_ALGO {
        Brzozowski,
        Hopcroft,
    }

    /**
     * The minimization algorithm to use when {@link fr.menana.automaton.Operation#minimize(Automaton)} is called
     */
    public static MINIMIZATION_ALGO minimization_method = MINIMIZATION_ALGO.Hopcroft;

    /**
     * Returns a new minimal {@link fr.menana.automaton.Automaton} that recognizes the same language as the given {@link fr.menana.automaton.Automaton}<br>
     * The algorithm used is set by the variable {@link fr.menana.automaton.Operation#minimization_method}
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @return an equivalent minimal  automaton
     */
    public static Automaton minimize(Automaton base) {
        return minimize(base, minimization_method);
    }

    /**
     * Returns a new minimal {@link fr.menana.automaton.Automaton} that recognizes the same language as the given {@link fr.menana.automaton.Automaton}<br>
     * The algorithm used is set by the element in the given enum {@link fr.menana.automaton.Operation.MINIMIZATION_ALGO}
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @param method the minimization algorithm to use
     * @return an equivalent minimal  automaton
     */
    public static Automaton minimize(Automaton base, MINIMIZATION_ALGO method) {
        switch(method){
            case Brzozowski : return minimizeBrzozowski(base);
            case Hopcroft   : return minimizeHopcroft(base);
            default         : return minimizeHopcroft(base);
        }
    }

    /**
     * Minimizes the given {@link fr.menana.automaton.Automaton} using Hopcroft algorithm
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @return a minimal {@link fr.menana.automaton.Automaton}
     */
    public static Automaton minimizeHopcroft(Automaton base) {
        base = base.isDeterministic() ? base.clone() : base.determinize();
        if (base.getNbStates() == 0)
            return new Automaton();
        base.removeDeadStates();
        Set<Interval> alphabet = new TreeSet<>();
        List<Transition> allTransitions = base.getAllTransitions();
        for (Transition tr : allTransitions) {
            alphabet.addAll(tr.getIntervals());
        }
        alphabet = getDisjunction(alphabet);

        Set<Set<State>> p = new HashSet<>();
        Set<Set<State>> w = new HashSet<>();
        Set<State> acceptSet = new HashSet<>(base.getAcceptList());
        Set<State> nonAcceptSet = new HashSet<>(base.getNonAcceptList());
        if (!acceptSet.isEmpty())
            p.add(acceptSet);
        if (!nonAcceptSet.isEmpty())
            p.add(nonAcceptSet);

        w.add(acceptSet);
        w.add(nonAcceptSet);

        while (!w.isEmpty()) {
            Set<State> a = w.iterator().next();
            w.remove(a);
            for (Interval interval : alphabet) {
                Set<State> x = allTransitions.stream().filter(tr -> a.contains(tr.dest) && tr.values.intersects(interval)).map(tr -> tr.orig).collect(Collectors.toSet());
                if (!x.isEmpty()) {
                    for (Set<State> y : new HashSet<>(p)) {
                        Set<State> inter = intersection(x, y);
                        Set<State> comp = minus(y, x);
                        if (!inter.isEmpty() && !comp.isEmpty()) {
                            p.remove(y);
                            p.add(inter);
                            p.add(comp);
                            if (w.contains(y)) {
                                w.remove(y);
                                w.add(inter);
                                w.add(comp);
                            } else if (inter.size() <= comp.size()) {
                                w.add(inter);
                            } else {
                                w.add(comp);
                            }
                        }
                    }
                }
            }

        }
        Automaton out = new Automaton();
        Map<Set<State>,State> setToNewState = new HashMap<>();
        Map<State,Set<State>> oldStateToSet = new HashMap<>();
        State newInit = null;
        for (Set<State> group : p) {
         //   System.out.println(group);
            State newState = out.addState();
            setToNewState.put(group, newState);
            if (State.hasAcceptingState(group))
                out.setAccept(newState);
            if (State.hasInitialState(group))
                newInit = newState;
            for (State s : group) {
                oldStateToSet.put(s,group);
            }
        }
        for (Set<State> group : p) {
            State newState = setToNewState.get(group);
            for (State oldState : group) {
                for (Transition tr : oldState.getTransitions().values()) {
                    out.addTransition(newState,setToNewState.get(oldStateToSet.get(tr.dest)), tr.values.clone());
                }
            }
        }
        out.setInitial(newInit);

        out.reIndex();


        return out;
    }

    private static Set<State> intersection(Collection<State> left, Collection<State> right) {
        return left.stream().filter(right::contains).collect(Collectors.toSet());
    }
    private static Set<State> minus(Collection<State> left, Collection<State> right) {
        Set<State> out = new HashSet<>(left);
        right.forEach(out::remove);
        return out;
    }

    /**
     * Minimizes the given {@link fr.menana.automaton.Automaton} using Brzozowski algorithm
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @return a minimal {@link fr.menana.automaton.Automaton}
     */
    public static Automaton minimizeBrzozowski(Automaton base) {
        Automaton a = base.isDeterministic() ? base : base.determinize();
        if (a.getNbStates() == 0)
            return new Automaton();
        a = a.revert();//
        a = a.determinize(); // .determinize();
        a = a.revert().determinize();
        a.removeDeadStates();
        return a;
    }


    /**
     * Determinizes a given {@link fr.menana.automaton.Automaton} using the subset construction algorithm.<br>
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


        Set<Set<State>> toVisit = new HashSet<>();

        Set<State> start = closure(nfa.getInitial());


        State dfaInit = dfa.addState();
        dfaInit.initial = true;
        dfaInit.accept = State.hasAcceptingState(start);
        if (dfaInit.accept)
            dfa.setAccept(dfaInit);

        Map<Set<State>,State> association = new HashMap<>();
        association.put(start, dfaInit);

        toVisit.add(start);
        Set<Set<State>> visited = new HashSet<>();

        while (!toVisit.isEmpty()) {
            Iterator<Set<State>> it = toVisit.iterator();


            Set<State> current = it.next();
            it.remove();
            visited.add(current);
            //System.out.println("CURRENT : " + current);

            Set<Interval> intervals = new TreeSet<>();
            for (State s : current) {
                for (Transition tr : s.transitions.values()) {
                    intervals.addAll(tr.getIntervals());
                }
            }

            Set<Interval> intervalAlphabet = getDisjunction(intervals);
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
                        toVisit.add(next);
                }
            }
        }

        dfa.setInitial(dfaInit);
        dfa.reIndex();


        return dfa;
    }

    /**
     * Get the minimal ? list of {@link fr.menana.automaton.Interval} representing the same set of values as the given collection of overlapping {@link fr.menana.automaton.Interval}
     * @param intervals a collection of overlapping {@link Interval}
     * @return a set of non overlapping {@link fr.menana.automaton.Interval}
     */
    private static TreeSet<Interval> getDisjunction(Collection<Interval> intervals) {
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
        return new TreeSet<>(stack.stream().map(Interval::clone).collect(Collectors.toSet()));



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
     * using one of the integer value of the given {@link fr.menana.automaton.Interval}
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
     * @param automaton the {@link fr.menana.automaton.Automaton} to revert
     * @return a reverted {@link fr.menana.automaton.Automaton}
     */
    public static Automaton revert(Automaton automaton) {
        Map<State, State> map = new HashMap<>();
        Automaton out = new Automaton();
        for (State s : automaton.getStates()) {
            map.put(s, out.addState());
        }
        for (State s : automaton.getStates()) {
            for (Transition t : s.transitions.values())
            {
                State d = t.dest;
                out.addTransition(map.get(d),map.get(s),t.values.clone());
            }
        }
        out.setAccept(map.get(automaton.getInitial()));
        State init = out.addState();
        out.setInitial(init);
        for (State s : automaton.getAcceptList())
        {
            out.addEpsilonTransition(init,map.get(s));
        }
        return out;
    }

    /**
     * Returns a new automaton recognizing the concatenation of the languages defined by the {@link fr.menana.automaton.Automaton} given as a parameter
     * The returned automaton is deterministic if and only if both given automatons already are
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
        return (first.isDeterministic() && second.isDeterministic()?out.minimize():out) ;
    }

    /**
     * Returns a new automaton recognizing the union of the languages defined by the {@link fr.menana.automaton.Automaton} given as a parameter
     * The returned automaton is deterministic if and only if both given automatons already are
     * @param first the first {@link fr.menana.automaton.Automaton} used for the union
     * @param second the second {@link fr.menana.automaton.Automaton} used for the union
     * @return a new automaton recognizing the union of the languages defined by the {@link fr.menana.automaton.Automaton} parameters
     */
    public static Automaton union(Automaton first, Automaton second) {
        if (first.getInitial() == null)
            return second.clone();
        else if (second.getInitial() == null)
            return first.clone();
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
        return (first.isDeterministic() && second.isDeterministic()?out.minimize():out) ;
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


  /*  public static void main(String[] args) {
        Automaton a = Automaton.nfaFromString("((72345)|((7|8)+))");
       // System.out.println(a);
        a = a.determinize();
        System.out.println(a);
        a.toDotty("det.dot");
        //System.out.println(a.minimize());
        Automaton b = a.minimize();
        //State out = b.addState();
        //b.addTransition(out,b.getInitial(),13,14);
       // b = b.minimize();
        b.toDotty("hop.dot");
        System.out.println(b);
        Operation.minimization_method = MINIMIZATION_ALGO.Brzozowski;
        a.minimize().toDotty("brz.dot");
       // System.out.println(a.minimize());



    }*/
}
