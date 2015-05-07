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

/**
 * Represents a regular expression element composed of a regular expression with a Kleene plus symbol.
 * The Kleene plus symbol stands for one or more times the given regular expression.
 * It is constructed using a {@link fr.menana.automaton.regexp.RegExpSequence} and a {@link fr.menana.automaton.regexp.RegExpKleeneStar}
 * Created by Julien Menana on 05/05/2015.
 */
public class RegExpKleenePlus extends RegExp {

    /**
     * The regular expression on which to apply the Kleene plus
     */
    private RegExp internal;

    /**
     * Constructs a new regular expression with the Kleene plus symbol
     * @param internal the regular expression the Kleene plus is applied
     */
    RegExpKleenePlus(RegExp internal) {
        this.internal = new RegExpSequence(internal, new RegExpKleeneStar(internal));
    }

    @Override
    public String toString() {
        return "(" + this.internal.toString() + ")*";
    }

    @Override
    public Automaton toNFA() {
        return this.internal.toNFA();
    }
}
