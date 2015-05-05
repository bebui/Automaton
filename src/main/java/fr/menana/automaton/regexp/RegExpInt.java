package fr.menana.automaton.regexp;

import fr.menana.automaton.Automaton;
import fr.menana.automaton.State;

/**
 * Created by julien on 05/05/2015.
 */
public class RegExpInt extends RegExp {
    private int c;

    public RegExpInt(int c) {
        this.c = c;
    }

    public String toString() {
        if (c >= 0 && c < 10)
            return Integer.toString(c);
        else
            return "<" + Integer.toString(c) + ">";
    }

    @Override
    public Automaton toNFA() {
        Automaton auto = new Automaton();
        State s1 = auto.addState();
        State s2 = auto.addState();
        auto.setInitial(s1);
        auto.setAccept(s2);
        auto.addTransition(s1,s2,this.c);
        return auto;
    }
}
