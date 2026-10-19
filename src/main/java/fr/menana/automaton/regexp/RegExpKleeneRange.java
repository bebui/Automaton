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
     * Value of {@link #max} when the number of repetitions is not bounded
     */
    static final int UNBOUNDED = -1;

    /**
     * Parses the range given between braces : "n" (exactly n times), "n,m" (between n and m times),
     * "n," (at least n times) or ",m" (at most m times)
     * @param intRange the range
     * @throws IllegalArgumentException if the range is malformed
     */
    private void  parse(String intRange) {
        int comma = intRange.indexOf(',');
        if (comma != intRange.lastIndexOf(','))
            throw new IllegalArgumentException("Invalid repetition {" + intRange + "}");
        if (comma < 0) {
            this.min = parseBound(intRange, intRange);
            this.max = this.min;
        } else {
            String low = intRange.substring(0, comma);
            String high = intRange.substring(comma + 1);
            if (low.isEmpty() && high.isEmpty())
                throw new IllegalArgumentException("Invalid repetition {" + intRange + "}");
            this.min = low.isEmpty() ? 0 : parseBound(low, intRange);
            this.max = high.isEmpty() ? UNBOUNDED : parseBound(high, intRange);
        }
        if (this.max != UNBOUNDED && this.max < this.min)
            throw new IllegalArgumentException("Invalid repetition {" + intRange + "}: " + this.max + " < " + this.min);
    }

    private static int parseBound(String bound, String intRange) {
        try {
            int value = Integer.parseInt(bound);
            if (value >= 0)
                return value;
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException("Invalid repetition {" + intRange + "}");
    }

    @Override
    public String toString() {
        String range;
        if (this.max == UNBOUNDED)
            range = this.min + ",";
        else if (this.max == this.min)
            range = Integer.toString(this.min);
        else
            range = this.min + "," + this.max;
        return "(" + this.internal.toString() + "){" + range + "}";
    }

    @Override
    State build(Automaton auto, State start) {
        State current = start;
        for (int i = 0 ; i < this.min ; ++i)
            current = this.internal.build(auto, current);
        if (this.max == UNBOUNDED)
            return new RegExpKleeneStar(this.internal).build(auto, current);
        if (this.max == this.min)
            return current;
        // Every state reached after min to max repetitions leads to the end
        State end = auto.addState();
        for (int i = this.min ; i < this.max ; ++i) {
            auto.addEpsilonTransition(current, end);
            current = this.internal.build(auto, current);
        }
        auto.addEpsilonTransition(current, end);
        return end;
    }


}
