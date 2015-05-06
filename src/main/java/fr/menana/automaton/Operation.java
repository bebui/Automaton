package fr.menana.automaton;

import fr.menana.automaton.regexp.RegExpParser;

import java.util.*;

/**
 * Created by julien on 01/05/2015.
 */
public class Operation {

    public enum MINIMIZATION_ALGO {
        Brzozowski,
        Hopcroft,
    }
    public static MINIMIZATION_ALGO minimization_method = MINIMIZATION_ALGO.Brzozowski;

    public static Automaton minimize(Automaton base) {
        return minimize(base, minimization_method);
    }

    public static Automaton minimize(Automaton base, MINIMIZATION_ALGO method) {
        switch(method){
            case Brzozowski : return minimizeBrzozowski(base);
            case Hopcroft   : return minimizeHopcroft(base);
            default         : return minimizeBrzozowski(base);
        }
    }

    public static Automaton minimizeHopcroft(Automaton base) {
        throw new UnsupportedOperationException();
    }

    public static Automaton minimizeBrzozowski(Automaton base) {
        Automaton a = base.isDeterministic() ? base : base.determinize();
        return a.revert().determinize().revert().determinize();
    }


    public static Automaton determinize(Automaton nfa) {
        if (nfa.isDeterministic())
            return nfa.clone();


        if (nfa.getInitial() == null)
            return null;

        Automaton dfa = new Automaton();


        Stack<Set<State>> toVisit = new Stack<Set<State>>();

        Set<State> start = closure(nfa.getInitial());


        State dfaInit = dfa.addState();
        dfaInit.initial = true;
        dfaInit.accept = State.hasAcceptingState(start);
        dfa.setInitial(dfaInit);
        if (dfaInit.accept)
            dfa.setAccept(dfaInit);

        Map<Set<State>,State> association = new HashMap<Set<State>, State>();
        association.put(start, dfaInit);

        toVisit.push(start);
        Set<Set<State>> visited = new HashSet<Set<State>>();

        while (!toVisit.isEmpty()) {

            Set<State> current = toVisit.pop();
            visited.add(current);
            //System.out.println("CURRENT : " + current);

            Set<State> dests = new HashSet<State>();
            Set<Interval> intervals = new TreeSet<>();
            for (State s : current) {
                dests.addAll(s.transitions.keySet());
                for (Transition tr : s.transitions.values()) {
                    intervals.addAll(tr.getIntervals());
                }
            }
            //System.out.println(intervals);
            //System.out.println(current);
           // System.out.println("Intervals : "+intervals);
            List<Interval> intervalAlphabet = getDisjunction(intervals);
           // System.out.println("ALPHABET : " + intervalAlphabet);
            for (Interval i : intervalAlphabet) {
                Set<State> next = move(current, i);
                //System.out.println(current+"  -> "+ i+" -> "+next);
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
                  //  System.out.println(current + "(" + association.get(current) + ") -> " + i + " -> " + next + "(" + association.get(next) + ")");
                    if (!toVisit.contains(next) && !visited.contains(next))
                        toVisit.push(next);
                }
            }
        }



        return dfa;
    }

    private static List<Interval> getDisjunction(Collection<Interval> intervals) {
        boolean somethingDone = true;
        if (intervals.isEmpty())
            somethingDone = false;
        ArrayList<Interval> stack = new ArrayList<>(intervals.size());
        for (Interval i : intervals)
            stack.add(i.clone());
        List<Interval> toAdd = new ArrayList<>();
        List<Interval> toRemove = new ArrayList<>();
        int index = 0;
        while(somethingDone) {
            toAdd.clear();
            Interval inter= stack.get(index);
          //    System.out.println("INTER :"+inter);
            for (int i = 0 ; i < stack.size() ; ++i) {
                if (i != index) {
                    Interval other = stack.get(i);
                   // System.out.println("OTHER : "+other);
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

          /*  System.out.println("STACK :"+stack);
            System.out.println("toAdd :"+toAdd);
            System.out.println("toRem :"+toRemove);*/
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
            //System.exit(0);
        }
     //   System.out.println(stack);
        return (List<Interval>)stack.clone();

    }

    private static Set<State> closure(State s) {
        return closure(Collections.singleton(s));
    }

    private static Set<State> closure(Collection<State> states) {
        Set<State> out = new HashSet<State>();
        Set<State> toRem = new HashSet<State>();
        out.addAll(states);

        while (true) {
            Set<State> toAdd = new HashSet<State>();
            for (State s : out) {
                boolean onlyEpsilon = s.transitions.size() > 0 ? true : false;
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

    private static Set<State> move(Collection<State> states,Interval symbol){
        Set<State> out = new HashSet<>();
        for (State s :states) {
            for (Transition tr : s.transitions.values()) {
                if (tr.values != null && tr.values.intersects(symbol))
                    out.add(tr.dest);
            }
        }
        return closure(out);

    }


    public static Automaton revert(Automaton a) {
        Map<State, State> map = new HashMap<State, State>();
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

    public static Automaton concatenate(Automaton first, Automaton second) {
        Automaton out = first.clone();
        Map<State, State> map = new HashMap<State, State>();
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

    public static Automaton union(Automaton first, Automaton second) {
        Automaton out = first.clone();
        Map<State, State> map = new HashMap<State, State>();
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

    public static Automaton complement(Automaton auto) {
        Automaton out = auto.clone();
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


    public static void main(String[] args) {
        Automaton a = new RegExpParser("10").parse().toNFA().minimize();
        Automaton b = new RegExpParser("50").parse().toNFA().minimize();

        System.out.println(a);
        System.out.println(b);
        System.out.println(complement(a).minimize());




    }



}
