package fr.menana.automaton.regexp;

import fr.menana.automaton.Automaton;

/**
 * Created by Julien Menana on 05/05/2015.
 */
public class RegExpKleenePlus extends RegExp {
    private RegExp internal;

    public RegExpKleenePlus(RegExp internal) {
        this.internal = new RegExpSequence(internal, new RegExpKleeneStar(internal));
    }

    public String toString() {
        return "(" + this.internal.toString() + ")*";
    }

    @Override
    public Automaton toNFA() {
        return this.internal.toNFA();
    }
}
