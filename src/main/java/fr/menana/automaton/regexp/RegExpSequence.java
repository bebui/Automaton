package fr.menana.automaton.regexp;

import fr.menana.automaton.Automaton;
import fr.menana.automaton.IntervalSet;
import fr.menana.automaton.State;
import fr.menana.automaton.Transition;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Julien Menana on 05/05/2015.
 */
public class RegExpSequence extends RegExp {
    private RegExp first;
    private RegExp second;

    public RegExpSequence(RegExp first, RegExp second) {
        this.first = first;
        this.second = second;
    }

    public String toString() {
        return first.toString() + second.toString();
    }

    @Override
    public Automaton toNFA() {
        Automaton auto = this.first.toNFA();
        Automaton second = this.second.toNFA();

        Map<State,State> asso = new HashMap<>();
        for (State s : second.getStates())
        {
            asso.put(s,auto.addState());
        }
        for (State s : second.getStates())
        {
            for (Transition tr : s.getTransitions().values()) {
                if (tr.values != null) {
                    IntervalSet set = tr.values.clone();
                    auto.addTransition(asso.get(tr.orig), asso.get(tr.dest), set);
                }
                if (tr.hasEpsilon())
                    auto.addEpsilonTransition(asso.get(tr.orig),asso.get(tr.dest));
            }
        }
        for (State s : auto.getAcceptList()) {
            auto.addEpsilonTransition(s,asso.get(second.getInitial()));
            auto.setAccept(s, false);
        }
        for (State s : second.getAcceptList()) {
            auto.setAccept(asso.get(s));
        }

        return auto;

    }
}
