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
 * Regular expressions over integers, turned into automata by {@link fr.menana.automaton.Automaton#dfaFromString(String)},
 * {@link fr.menana.automaton.Automaton#nfaFromString(String)} or {@link fr.menana.automaton.regexp.RegExpParser}.
 *
 * <h2>Syntax</h2>
 * <table class="striped">
 *   <caption>Regular expression syntax</caption>
 *   <thead><tr><th scope="col">Syntax</th><th scope="col">Meaning</th></tr></thead>
 *   <tbody>
 *   <tr><td><code>5</code></td><td>the symbol 5 : every digit is a symbol</td></tr>
 *   <tr><td><code>&lt;42&gt;</code>, <code>&lt;-3&gt;</code></td><td>any integer, written between angle brackets</td></tr>
 *   <tr><td><code>.</code></td><td>any integer</td></tr>
 *   <tr><td><code>ab</code></td><td>concatenation</td></tr>
 *   <tr><td><code>a|b</code></td><td>alternative</td></tr>
 *   <tr><td><code>a&amp;b</code></td><td>intersection : words matching both</td></tr>
 *   <tr><td><code>~a</code></td><td>complement : every word not matching <code>a</code></td></tr>
 *   <tr><td><code>a*</code>, <code>a+</code>, <code>a?</code></td><td>zero or more, one or more, zero or one</td></tr>
 *   <tr><td><code>a{n}</code>, <code>a{n,m}</code>, <code>a{n,}</code>, <code>a{,m}</code></td>
 *       <td>exactly n, between n and m, at least n, at most m repetitions</td></tr>
 *   <tr><td><code>( )</code></td><td>grouping</td></tr>
 *   </tbody>
 * </table>
 * <p>
 * <code>12</code> is the symbol 1 followed by the symbol 2, while <code>&lt;12&gt;</code> is the single symbol 12.
 * Priorities, from the strongest : repetitions, complement, concatenation, intersection, alternative.
 * For instance <code>~12</code> is <code>(~1)2</code> and <code>1&amp;1|2</code> is <code>(1&amp;1)|2</code>.
 * <p>
 * An invalid expression throws an {@link java.lang.IllegalArgumentException} giving the position of the error.
 */
package fr.menana.automaton.regexp;
