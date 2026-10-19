/*
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

/**
 * Finite automata whose symbols are integers. <br>
 * Transitions are labelled with sets of integer intervals ({@link fr.menana.automaton.IntervalSet}), so an automaton
 * works on the whole <code>int</code> range without enumerating it.
 *
 * <h2>Getting started</h2>
 * <pre>{@code
 * Automaton auto = Automaton.dfaFromString("(1|2)*12(1|2)*");   // words over {1, 2} containing 1 2
 * auto.run(1, 1, 2);    // true
 * auto.run(2, 1);       // false
 * }</pre>
 * The syntax of regular expressions is described in {@link fr.menana.automaton.regexp}.
 * Automata can also be built state by state, see {@link fr.menana.automaton.Automaton}.
 *
 * <h2>What the library provides</h2>
 * <table class="striped">
 *   <caption>Main features</caption>
 *   <thead><tr><th scope="col">Topic</th><th scope="col">Methods</th></tr></thead>
 *   <tbody>
 *   <tr><th scope="row">Construction</th>
 *       <td>{@link fr.menana.automaton.Automaton#dfaFromString(String)}, {@link fr.menana.automaton.Automaton#nfaFromString(String)},
 *       {@link fr.menana.automaton.Automaton#addState()}, {@link fr.menana.automaton.Automaton#addTransition(State, State, int...)}</td></tr>
 *   <tr><th scope="row">Transformations</th>
 *       <td>{@link fr.menana.automaton.Automaton#determinize()}, {@link fr.menana.automaton.Automaton#minimize()},
 *       {@link fr.menana.automaton.Automaton#removeEpsilon()}, {@link fr.menana.automaton.Automaton#reduce()},
 *       {@link fr.menana.automaton.Automaton#revert()}</td></tr>
 *   <tr><th scope="row">Boolean operations</th>
 *       <td>{@link fr.menana.automaton.Automaton#union(Automaton)}, {@link fr.menana.automaton.Automaton#intersection(Automaton)},
 *       {@link fr.menana.automaton.Automaton#difference(Automaton)}, {@link fr.menana.automaton.Automaton#complement()},
 *       {@link fr.menana.automaton.Automaton#concatenate(Automaton)}</td></tr>
 *   <tr><th scope="row">Questions on languages</th>
 *       <td>{@link fr.menana.automaton.Automaton#isEmpty()}, {@link fr.menana.automaton.Automaton#isFinite()},
 *       {@link fr.menana.automaton.Automaton#isUniversal()}, {@link fr.menana.automaton.Automaton#sameLanguage(Automaton)},
 *       {@link fr.menana.automaton.Automaton#isSubsetOf(Automaton)}, {@link fr.menana.automaton.Automaton#shortestWord()},
 *       {@link fr.menana.automaton.Automaton#counterExample(Automaton)}</td></tr>
 *   <tr><th scope="row">Words</th>
 *       <td>{@link fr.menana.automaton.Automaton#run(int...)}, {@link fr.menana.automaton.Automaton#countWords(int)},
 *       {@link fr.menana.automaton.Automaton#randomWord(int, java.util.Random)}, {@link fr.menana.automaton.Automaton#words(int)}</td></tr>
 *   <tr><th scope="row">Fast recognition and search</th>
 *       <td>{@link fr.menana.automaton.CompiledDfa}, {@link fr.menana.automaton.Searcher}</td></tr>
 *   <tr><th scope="row">Visualization</th>
 *       <td>{@link fr.menana.automaton.Automaton#toDot()}, {@link fr.menana.automaton.Automaton#toDotty(String)}</td></tr>
 *   </tbody>
 * </table>
 *
 * <h2>Structure and language</h2>
 * {@link fr.menana.automaton.Automaton#equals(Object)} compares the structure of two automata, while
 * {@link fr.menana.automaton.Automaton#sameLanguage(Automaton)} compares the words they accept.
 * Operations return new automata and leave their operands unchanged.
 *
 * <h2>Threads</h2>
 * {@link fr.menana.automaton.Automaton} and {@link fr.menana.automaton.IntervalSet} are mutable and not thread safe.
 * {@link fr.menana.automaton.CompiledDfa} and {@link fr.menana.automaton.Searcher} are immutable and can be shared between threads.
 */
package fr.menana.automaton;
