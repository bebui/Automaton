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
import fr.menana.automaton.Util;

/**
 * This class is a parser for regular expression whose alphabet is solely composed of integers. <p>
 * It supports : <p>
 *  - &lt;x&gt; notation for integer greater than 10 or negative integer. ("12&lt;12&gt;" is the regular expression for 1 followed by 2 followed by 12)  <p>
 *  - * notation for Kleene star <p>
 *  - + notation for Kleene plus <p>
 *  - . notation for any integer <p>
 *  - {x,y} notation to enforce the appearance of an expression between x and y times <p>
 *  - ? notation for zero or one. <p> <p>
 *  - Created by Julien Menana on 05/05/15.
 */
public class RegExpParser {


    /**
     * The input to consume and form the regular expression
     */
    private String input;

    /**
     * Consturcts a new parser from a string
     * @param input a regular expression as a string
     */
    private RegExpParser(String input) {
        this.input = input;
    }


    private String peek() {
        if (input.charAt(0) == '<') {
            return input.substring(1,input.indexOf('>',1));
        }
        return input.substring(0,1) ;
    }

    private void eat(String c) {
        if (peek().equals(c)) {
            if (c.length() == 1)
                this.input = this.input.substring(c.length());
            else
                this.input = this.input.substring(c.length()+2);
        }
        else
            throw new RuntimeException("Expected: " + c + "; got: " + peek()) ;
    }

    private String next() {
        String c = peek() ;
        eat(c) ;
        return c ;
    }

    private boolean more() {
        return !input.isEmpty();
    }
    private RegExp regex() {
        RegExp term = term() ;

        if (more() && peek().equals("|")) {
            eat ("|") ;
            RegExp regex = regex() ;
            return new RegExpOr(term,regex) ;
        } else {
            return term ;
        }
    }

    private RegExp term() {
        RegExp factor = RegExp.blank ;

        while (more() && !peek().equals(")") && !peek().equals("|")) {
            RegExp nextFactor = factor() ;
            factor = new RegExpSequence(factor,nextFactor) ;
        }

        return factor ;
    }

    private RegExp factor() {

        RegExp base = base() ;

        while (more() && peek().equals("*")) {
            eat("*") ;
            base = new RegExpKleeneStar(base) ;
        }
        while (more() && peek().equals("+")) {
            eat("+") ;
            base = new RegExpKleenePlus(base) ;
        }
        if (more() && peek().equals("{")) {
            eat("{");
            StringBuilder b = new StringBuilder();
            while (more() && !peek().equals("}")) {
                String s = peek();
                b.append(s);
                eat(s);
            }
            eat("}");
            base = new RegExpKleeneRange(base,b.toString());
        }

        return base ;
    }

    private RegExp base() {
        if (peek().equals("(")) {
            eat("(");
            RegExp r = regex() ;
            eat(")") ;
            return r ;
        }
        else {
            String next = next();
            if (Util.isInteger(next)) {
                return new RegExpInt(Integer.parseInt(next));
            }
            else if (next.equals(".")) {
                return new RegExpAny();
            }
            else {
                throw new RuntimeException("Char allowed are int and . for any int");
            }

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
        return this.regex();
    }

}
