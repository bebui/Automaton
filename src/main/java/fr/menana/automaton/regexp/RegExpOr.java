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
 * Represents a regular expression constructed by the union of two regular expressions
 *
 * @author Julien Menana
 */
class RegExpOr extends RegExp {
    /**
     * Regular expression composing the union
     */
    private RegExp left;

    /**
     * Regular expression composing the union
     */
    private RegExp right;

    /**
     * Constructs a new regular expression as the union of two given regular expression
     * @param left the first regular expression composing the union
     * @param right the second regular expression composing the union
     */
    RegExpOr(RegExp left, RegExp right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + this.left + "|" + this.right + ")";
    }

    @Override
    State build(Automaton auto, State start) {
        // Alternatives built by the parser are nested on the right : walks them without recursion
        State end = auto.addState();
        RegExp current = this;
        while (current instanceof RegExpOr) {
            auto.addEpsilonTransition(((RegExpOr) current).left.build(auto, start), end);
            current = ((RegExpOr) current).right;
        }
        auto.addEpsilonTransition(current.build(auto, start), end);
        return end;
    }
}
