/**
 * Automaton
 * Copyright (c) 2025, Julien Menana, All rights reserved.
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
 * Abstract class for regular expression. <p>
 * Created by Julien Menana on 05/05/2015.
 */
public abstract class RegExp {

    RegExp(){}

    /**
     * Singleton for epsilon regular expression
     */
    public static final RegExp blank = new RegExpEpsilon();

    @Override
    public abstract String toString();

    /**
     * Returns the non-deterministic {@link fr.menana.automaton.Automaton} that represents the language accepted by this regular expression. <br>
     * Uses Thompson's construction : the automaton is built in a single pass, in time linear in the size of the expression
     * @return a non deterministic {@link fr.menana.automaton.Automaton} equivalent to this regular expression
     */
    public Automaton toNFA() {
        Automaton auto = new Automaton();
        State start = auto.addState();
        auto.setInitial(start);
        auto.setAccept(this.build(auto, start));
        return auto;
    }

    /**
     * Adds to an automaton the states and transitions recognizing this expression. <br>
     * The fragment starts at a given state and ends at the returned state. A fragment never adds
     * transitions going into <code>start</code>, so that several fragments can share it
     * @param auto the automaton being built
     * @param start the state from which the expression is read
     * @return the state reached once the expression has been read
     */
    abstract State build(Automaton auto, State start);
}
