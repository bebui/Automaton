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
import fr.menana.automaton.State;

/**
 * Created by Julien Menana on 05/05/2015.
 */
public class RegExpInt extends RegExp {
    private int c;

    public RegExpInt(int c) {
        this.c = c;
    }

    public String toString() {
        if (c >= 0 && c < 10)
            return Integer.toString(c);
        else
            return "<" + Integer.toString(c) + ">";
    }

    @Override
    public Automaton toNFA() {
        Automaton auto = new Automaton();
        State s1 = auto.addState();
        State s2 = auto.addState();
        auto.setInitial(s1);
        auto.setAccept(s2);
        auto.addTransition(s1,s2,this.c);
        return auto;
    }
}
