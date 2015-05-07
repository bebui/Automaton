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
package fr.menana.automaton.regexp;

import fr.menana.automaton.Automaton;
import fr.menana.automaton.State;


/**
 * Represents a regular expression element composed of a regular expression with a Kleene star symbol.
 * The Kleene star symbol stands for zero or more times the given regular expression.
 * Created by Julien Menana on 05/05/2015.
 */
public class RegExpKleeneStar extends RegExp {

    /**
     * The regular expression on which to apply the Kleene star
     */
    private RegExp internal;

    /**
     * Constructs a new regular expression with the Kleene stàr symbol
     * @param internal the regular expression the Kleene star is applied
     */
    RegExpKleeneStar(RegExp internal) {
        this.internal = internal;
    }

    @Override
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
