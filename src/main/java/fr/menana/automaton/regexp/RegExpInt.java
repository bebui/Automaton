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
 * A regular expression element representing symbols of the regular expression as integers
 * Created by Julien Menana on 05/05/2015.
 */
public class RegExpInt extends RegExp {

    /**
     * An integer used as a symbol in the alphabet of the regular expression
     */
    private int symbol;

    /**
     * Constructs a regular expression element that contains an integer
     * @param symbol the integer represented by this element
     */
    RegExpInt(int symbol) {
        this.symbol = symbol;
    }

    @Override
    public String toString() {
        if (symbol >= 0 && symbol < 10)
            return Integer.toString(symbol);
        else
            return "<" + Integer.toString(symbol) + ">";
    }

    @Override
    public Automaton toNFA() {
        Automaton auto = new Automaton();
        State s1 = auto.addState();
        State s2 = auto.addState();
        auto.setInitial(s1);
        auto.setAccept(s2);
        auto.addTransition(s1,s2,this.symbol);
        return auto;
    }
}
