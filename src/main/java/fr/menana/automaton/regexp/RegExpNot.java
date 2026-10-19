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
 * Represents the complement of a regular expression, written <code>~expression</code> :
 * every word over the integers that the expression does not recognize
 */
class RegExpNot extends RegExp {

    private final RegExp internal;

    /**
     * Constructs a new regular expression recognizing the words the given expression does not recognize
     * @param internal the complemented regular expression
     */
    RegExpNot(RegExp internal) {
        this.internal = internal;
    }

    @Override
    public String toString() {
        return "~(" + this.internal + ")";
    }

    @Override
    State build(Automaton auto, State start) {
        return embed(auto, start, this.internal.toNFA().complement().minimize());
    }
}
