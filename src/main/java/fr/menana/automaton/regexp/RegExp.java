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
 * Abstract class for regular expression. <p>
 * Created by Julien Menana on 05/05/2015.
 */
public abstract class RegExp {

    RegExp(){}

    /**
     * Singleton for epsilon regular expression
     */
    public static RegExp blank = new RegExpEpsilon();

    @Override
    public abstract String toString();

    /**
     * Returns the non-deterministic {@link fr.menana.automaton.Automaton} that represents the language accepted by this regular expression
     * @return a non deterministic {@link fr.menana.automaton.Automaton} equivalent to this regular expression
     */
    public abstract Automaton toNFA();
}
