/*
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
 * Represents a regular expression element composed of a regular expression with a Kleene star symbol.  <br>
 * The Kleene star symbol stands for zero or more times the given regular expression. 
 *
 * @author Julien Menana
 */
class RegExpKleeneStar extends RegExp {

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
    State build(Automaton auto, State start) {
        // The loop goes through a fresh state, so that it never adds paths to other expressions sharing start
        State loop = auto.addState();
        auto.addEpsilonTransition(start, loop);
        auto.addEpsilonTransition(this.internal.build(auto, loop), loop);
        return loop;
    }
}
