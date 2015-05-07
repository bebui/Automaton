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
import fr.menana.automaton.IntervalSet;
import fr.menana.automaton.State;
import fr.menana.automaton.Transition;

import java.util.HashMap;
import java.util.Map;

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
    public Automaton toNFA() {
        Automaton auto = this.left.toNFA();
        Automaton second = this.right.toNFA();

        Map<State,State> asso = new HashMap<>();
        for (State s : second.getStates())
        {
            asso.put(s,auto.addState());
        }
        for (State s : second.getStates())
        {
            for (Transition tr : s.getTransitions().values()) {
                if (tr.values != null) {
                    IntervalSet set = tr.values.clone();
                    auto.addTransition(asso.get(tr.orig), asso.get(tr.dest), set);
                }
                if (tr.hasEpsilon())
                    auto.addEpsilonTransition(asso.get(tr.orig),asso.get(tr.dest));
            }
        }
        for (State s : auto.getAcceptList()) {
            auto.addEpsilonTransition(s,asso.get(second.getInitial()));
            auto.setAccept(s, false);
        }
        for (State s : second.getAcceptList()) {
            auto.setAccept(asso.get(s));
        }

        return auto;

    }
}
