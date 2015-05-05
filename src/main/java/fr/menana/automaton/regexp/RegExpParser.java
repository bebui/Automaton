package fr.menana.automaton.regexp;

import fr.menana.automaton.Automaton;
import fr.menana.automaton.Util;

/**
 * Created by A66572 on 05/05/15.
 */
public class RegExpParser {


    private String input;

    public RegExpParser(String input) {
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
        return input.length() > 0 ;
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
            StringBuffer b = new StringBuffer();
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
        String s = peek();
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


    public RegExp parse() {
        return this.regex();
    }


    public static void main(String[] args) {
        String toto = "1234(<10>|<12>|<11>){1,3}.1234";
        RegExpParser parser = new RegExpParser(toto);

        RegExp r = parser.parse();
        System.out.println(r);

        Automaton dfa = r.toNFA().minimize();
        System.out.println(dfa);

        System.out.println(dfa.run(1, 2, 3, 4, 11,1,1,2,3,4));

    }

}
