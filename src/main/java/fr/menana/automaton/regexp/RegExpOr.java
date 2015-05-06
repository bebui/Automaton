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

/**
 * Created by Julien Menana on 05/05/2015.
 */
public class RegExpOr extends RegExp {
    private RegExp thisOne;
    private RegExp thatOne;

    public RegExpOr(RegExp thisOne, RegExp thatOne) {
        this.thisOne = thisOne;
        this.thatOne = thatOne;
    }

    public String toString() {
        return "(" + this.thisOne + "|" + this.thatOne + ")";
    }

    @Override
    public Automaton toNFA() {
        Automaton auto = new Automaton();
        State s1 = auto.addState();
        State s2 = auto.addState();

        Automaton[] choice = new Automaton[]{thisOne.toNFA(),thatOne.toNFA()};

        for (Automaton a : choice) {
            Map<State,State> asso = new HashMap<>();
            for (State s : a.getStates()) {
                asso.put(s,auto.addState());
            }
            for (State s : a.getStates()) {
                for (Transition tr : s.getTransitions().values()) {
                    if (tr.values != null) {
                        IntervalSet set = tr.values.clone();
                        auto.addTransition(asso.get(tr.orig), asso.get(tr.dest), set);
                    }
                    if (tr.hasEpsilon())
                        auto.addEpsilonTransition(asso.get(tr.orig),asso.get(tr.dest));
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
