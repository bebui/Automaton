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
 * Represents the intersection of two regular expressions, written <code>left&amp;right</code> <p>
 */
public class RegExpAnd extends RegExp {

    private final RegExp left;
    private final RegExp right;

    /**
     * Constructs a new regular expression recognizing the words of both given expressions
     * @param left the first regular expression
     * @param right the second regular expression
     */
    RegExpAnd(RegExp left, RegExp right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + this.left + "&" + this.right + ")";
    }

    @Override
    State build(Automaton auto, State start) {
        return embed(auto, start, this.left.toNFA().intersection(this.right.toNFA()));
    }
}
