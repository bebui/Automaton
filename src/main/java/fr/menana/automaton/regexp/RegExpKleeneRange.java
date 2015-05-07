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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a regular expression that is repeated a number of time in a given range  <p>
 * Created by Julien Menana on 05/05/2015.
 */
public class RegExpKleeneRange extends RegExp {

    /**
     * The regular expression that has to be repeated
     */
    RegExp internal;

    /**
     * the minimum number of repetition of the given regular expression
     */
    int min;

    /**
     * the maximum number of repetition of the given regular expression
     */
    int max;

    /**
     * Constructs a new regular expression that is repeated a number of time in a given range
     * @param internal the regular expression thas has to be repeated
     * @param intRange the range within the regular expression has to be repeated
     */
    RegExpKleeneRange(RegExp internal, String intRange) {
        this.internal = internal;
        this.parse(intRange);
    }

    /**
     * Parse a range or a integer into min and max
     * @param intRange a integer range in the form X or X,Y
     */
    private void  parse(String intRange) {
        String[] splitted = intRange.split(",");
        this.min = Integer.parseInt(splitted[0]);
        if (splitted.length > 1)
            this.max = Integer.parseInt(splitted[1]);
        else
            this.max = this.min;
    }

    @Override
    public String toString() {
        return this.internal.toString()+"{"+(this.max==this.min?this.min:(this.min+","+this.max))+"}";
    }

    @Override
    public Automaton toNFA() {
        Automaton auto = new RegExpEpsilon().toNFA();
        for (int i = 0 ; i < this.min ; ++i) {
            auto = auto.concatenate(this.internal.toNFA());
        }
        List<Integer> finalIndexes = new ArrayList<>();
        for (int i = this.min ; i < this.max ; ++i) {
            finalIndexes.addAll(auto.getAcceptList().stream().map(State::getIndex).collect(Collectors.toList()));
            auto = auto.concatenate(this.internal.toNFA());
        }
        State accept = auto.addState();
        for (int i : finalIndexes)
            auto.addEpsilonTransition(auto.getStates().get(i),accept);
        for (State s : auto.getAcceptList()) {
            auto.addEpsilonTransition(s,accept);
            auto.setAccept(s,false);
        }
        auto.setAccept(accept);

        return auto;

    }


}
