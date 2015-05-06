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
public class RegExpSequence extends RegExp {
    private RegExp first;
    private RegExp second;

    public RegExpSequence(RegExp first, RegExp second) {
        this.first = first;
        this.second = second;
    }

    public String toString() {
        return first.toString() + second.toString();
    }

    @Override
    public Automaton toNFA() {
        Automaton auto = this.first.toNFA();
        Automaton second = this.second.toNFA();

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
