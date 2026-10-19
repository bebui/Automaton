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
import fr.menana.automaton.Util;

/**
 * This class is a parser for regular expression whose alphabet is solely composed of integers. <br>
 * Largely inspired from <a href="http://matt.might.net/articles/parsing-regex-with-recursive-descent/">http://matt.might.net/articles/parsing-regex-with-recursive-descent/</a>
 * Invalid expressions are rejected with an {@link IllegalArgumentException} giving the error position. <br>
 * It supports : <br>
 *    - &lt;x&gt; notation for integer greater than 10 or negative integer. ("12&lt;12&gt;" is the regular expression for 1 followed by 2 followed by 12) <br>
 *    - * notation for Kleene star <br>
 *    - + notation for Kleene plus <br>
 *    - . notation for any integer <br>
 *    - {x,y} notation to enforce the appearance of an expression between x and y times, {x} exactly x times, {x,} at least x times <br>
 *    - ? notation for zero or one <br>
 *    - &amp; notation for intersection, with a lower priority than concatenation and a higher one than | <br>
 *    - ~ notation for complement, applying to the expression that follows with its repetition operators : ~12 is (~1)2 <p>
 * Created by Julien Menana on 05/05/15.
 */
public class RegExpParser {


    /**
     * The whole regular expression, used to report error positions
     */
    private final String expression;

    /**
     * Position of the next character to read in the expression
     */
    private int pos;

    /**
     * Consturcts a new parser from a string
     * @param input a regular expression as a string
     */
    private RegExpParser(String input) {
        this.expression = input;
        this.pos = 0;
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + pos + " in \"" + expression + "\"");
    }

    private boolean more() {
        return pos < expression.length();
    }

    /**
     * Checks if the next character is the given operator
     */
    private boolean peekIs(char operator) {
        return more() && expression.charAt(pos) == operator;
    }

    private void eat(char operator) {
        if (!more())
            throw error("Expected '" + operator + "' but reached the end");
        if (expression.charAt(pos) != operator)
            throw error("Expected '" + operator + "' but found '" + expression.charAt(pos) + "'");
        pos++;
    }

    private RegExp regex() {
        RegExp intersection = intersection() ;

        if (peekIs('|')) {
            eat ('|') ;
            RegExp regex = regex() ;
            return new RegExpOr(intersection,regex) ;
        } else {
            return intersection ;
        }
    }

    private RegExp intersection() {
        RegExp term = term() ;

        if (peekIs('&')) {
            eat ('&') ;
            RegExp intersection = intersection() ;
            return new RegExpAnd(term,intersection) ;
        } else {
            return term ;
        }
    }

    private RegExp term() {
        RegExp factor = RegExp.blank ;

        while (more() && !peekIs(')') && !peekIs('|') && !peekIs('&')) {
            RegExp nextFactor = complement() ;
            factor = new RegExpSequence(factor,nextFactor) ;
        }

        return factor ;
    }

    private RegExp complement() {
        if (peekIs('~')) {
            eat('~');
            return new RegExpNot(complement());
        }
        return factor();
    }

    private RegExp factor() {

        RegExp base = base() ;

        while (peekIs('*')) {
            eat('*') ;
            base = new RegExpKleeneStar(base) ;
        }
        while (peekIs('+')) {
            eat('+') ;
            base = new RegExpKleenePlus(base) ;
        }
        while (peekIs('?')) {
            eat('?') ;
            base = new RegExpKleeneRange(base,"0,1");
        }
        if (peekIs('{')) {
            int end = expression.indexOf('}', pos);
            if (end < 0)
                throw error("Missing '}'");
            String range = expression.substring(pos + 1, end);
            try {
                base = new RegExpKleeneRange(base, range);
            } catch (IllegalArgumentException e) {
                throw error(e.getMessage());
            }
            pos = end + 1;
        }

        return base ;
    }

    private RegExp base() {
        if (!more())
            throw error("Unexpected end of expression");
        char c = expression.charAt(pos);
        if (c == '(') {
            eat('(');
            RegExp r = regex() ;
            eat(')') ;
            return r ;
        }
        else if (c == '.') {
            eat('.');
            return new RegExpAny();
        }
        else if (c == '<') {
            int end = expression.indexOf('>', pos);
            if (end < 0)
                throw error("Missing '>'");
            String number = expression.substring(pos + 1, end);
            if (!Util.isInteger(number))
                throw error("Expected an integer between '<' and '>' but found \"" + number + "\"");
            int value;
            try {
                value = Integer.parseInt(number);
            } catch (NumberFormatException e) {
                throw error("Integer out of range: " + number);
            }
            pos = end + 1;
            return new RegExpInt(value);
        }
        else if (c >= '0' && c <= '9') {
            pos++;
            return new RegExpInt(c - '0');
        }
        else {
            throw error("Unexpected character '" + c + "', allowed symbols are digits, <integer> and .");
        }
    }

    /**
     * Returns a new {@link fr.menana.automaton.regexp.RegExp} constructed from a regular expression expressed as a string
     * @param regexp the regular expression string to parse
     * @return a new {@link fr.menana.automaton.regexp.RegExp} tree from the input string
     */
    public static RegExp toRegExp(String regexp) {
        return new RegExpParser(regexp).parse();
    }

    /**
     * Returns a new non-deterministic {@link fr.menana.automaton.Automaton} from a regular expression represented by the given string
     * @param regexp the regular expression string to parse
     * @return a new non-deterministic {@link fr.menana.automaton.Automaton} constructed from the input string
     */
    public static Automaton toNFA(String regexp) {
        return toRegExp(regexp).toNFA();
    }

    /**
     * Returns a new deterministic {@link fr.menana.automaton.Automaton} from a regular expression represented by the given string
     * @param regexp the regular expression string to parse
     * @return a new deterministic {@link fr.menana.automaton.Automaton} constructed from the input string
     */
    @SuppressWarnings("unused")
    public static Automaton toDFA(String regexp) {
        return toNFA(regexp).minimize();
    }


    private RegExp parse() {
        RegExp regexp = this.regex();
        if (more())
            throw error("Unexpected '" + expression.charAt(pos) + "'");
        return regexp;
    }



}
