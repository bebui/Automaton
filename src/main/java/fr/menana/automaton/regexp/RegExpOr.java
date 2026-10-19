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
import fr.menana.automaton.Transition;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a regular expression constructed by the union of two regular expressions <p>
 * Created by Julien Menana on 05/05/2015.
 */
public class RegExpOr extends RegExp {
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
     * @param right the second regular expressino composing the union
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
    public Automaton toNFA() {
        Automaton auto = new Automaton();
        State s1 = auto.addState();
        State s2 = auto.addState();

        Automaton[] choice = new Automaton[]{left.toNFA(), right.toNFA()};

        for (Automaton a : choice) {
            Map<State,State> asso = new HashMap<>();
            for (State s : a.getStates()) {
                asso.put(s,auto.addState());
            }
            for (State s : a.getStates()) {
                for (Transition tr : s.getTransitions().values()) {
                    if (tr.hasValues()) {
                        auto.addTransition(asso.get(tr.getOrig()), asso.get(tr.getDest()), tr.getValues());
                    }
                    if (tr.hasEpsilon())
                        auto.addEpsilonTransition(asso.get(tr.getOrig()),asso.get(tr.getDest()));
                }
            }
            auto.addEpsilonTransition(s1,asso.get(a.getInitial()));
            for (State s : a.getAcceptList()) {
                auto.addEpsilonTransition(asso.get(s), s2);
            }
        }
        auto.setInitial(s1);
        auto.setAccept(s2);
        return auto;
    }
}
