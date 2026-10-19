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

import java.util.ArrayDeque;
import java.util.Deque;

/** Represents a sequence or concatenation of two regular expressions <p>
 * Created by Julien Menana on 05/05/2015.
 */
public class RegExpSequence extends RegExp {

    /**
     * The left hand side regular expression of the sequence
     */
    private RegExp left;

    /**
     * The right hand side regular expression of the sequence
     */
    private RegExp right;

    /**
     * Constructs a new regular expression as the sequence or concatenation of two given regular expression
     * @param left the first regular expression composing the sequence
     * @param right the second regular expressino composing the sequence
     */
    RegExpSequence(RegExp left, RegExp right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return left.toString() + right.toString();
    }

    @Override
    State build(Automaton auto, State start) {
        // Sequences built by the parser are nested on the left : walks them without recursion
        Deque<RegExp> parts = new ArrayDeque<>();
        RegExp current = this;
        while (current instanceof RegExpSequence) {
            parts.push(((RegExpSequence) current).right);
            current = ((RegExpSequence) current).left;
        }
        State end = current.build(auto, start);
        for (RegExp part : parts)
            end = part.build(auto, end);
        return end;
    }
}
