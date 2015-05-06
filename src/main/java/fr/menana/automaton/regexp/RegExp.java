package fr.menana.automaton.regexp;

import fr.menana.automaton.Automaton;

/**
 * Created by Julien Menana on 05/05/2015.
 */
public abstract class RegExp {

    public static RegExp blank = new RegExpEpsilon();

    public abstract String toString();

    public abstract Automaton toNFA();
}
