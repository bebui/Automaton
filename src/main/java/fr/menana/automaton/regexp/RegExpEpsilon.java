package fr.menana.automaton.regexp;

import fr.menana.automaton.Automaton;
import fr.menana.automaton.State;

/**
 * Created by julien on 05/05/2015.
 */
public class RegExpEpsilon extends RegExp {


    public String toString() {
        return "";
    }

    @Override
    public Automaton toNFA() {
        Automaton auto = new Automaton();
        State s1 = auto.addState();
        State s2 = auto.addState();
        auto.setInitial(s1);
        auto.setAccept(s2);
        auto.addEpsilonTransition(s1,s2);
        return auto;
    }
}
