package fr.menana.automaton.regexp;

import fr.menana.automaton.Automaton;
import fr.menana.automaton.State;

/**
 * Created by Julien Menana on 05/05/2015.
 */
public class RegExpKleeneStar extends RegExp {
    private RegExp internal;

    public RegExpKleeneStar(RegExp internal) {
        this.internal = internal;
    }

    public String toString() {
        return "(" + this.internal.toString() + ")*";
    }

    @Override
    public Automaton toNFA() {
        Automaton auto = this.internal.toNFA();
        State init = auto.addState();
        State end  = auto.addState();

        State oldInit = auto.getInitial();


        auto.addEpsilonTransition(init, end);
        auto.addEpsilonTransition(init,oldInit);

        for (State s : auto.getAcceptList()) {
            auto.addEpsilonTransition(s,oldInit);
            auto.setAccept(s, false);
            auto.addEpsilonTransition(s,end);
        }
        auto.setInitial(init);
        auto.setAccept(end);
        return auto;
    }
}
