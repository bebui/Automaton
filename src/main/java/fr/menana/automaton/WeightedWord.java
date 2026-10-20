/*
 * Automaton
 * Copyright (c) 2026, Julien Menana, All rights reserved.
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
package fr.menana.automaton;

/** Immutable concrete word and its minimum accepting cost. */
public final class WeightedWord {
    private final int[] word;
    private final double cost;
    WeightedWord(int[] word, double cost) { this.word = word.clone(); this.cost = cost; }
    /** @return a defensive copy of the symbols */
    public int[] word() { return word.clone(); }
    /** @return number of consumed symbols */
    public int length() { return word.length; }
    /** @return minimum accepting cost, rounded once from an exact binary-input sum */
    public double cost() { return cost; }
}
