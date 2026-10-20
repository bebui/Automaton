import fr.menana.automaton.*;
import fr.menana.automaton.regexp.*;
import java.math.BigInteger;
import java.util.*;

/** Compile against original 3.0; execute the unchanged client bytecode with 4.0. */
public final class LegacyClientSmoke {
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) {
        construction(); intervals(); regex(); languages(); compilation();
        System.out.println("Original 3.0 client bytecode passed with 4.0: five scenarios");
    }
    static void construction() {
        // BEGIN construction
        Automaton a = new Automaton();
        State start = a.addState();
        State digits = a.addState();
        a.setInitial(start);
        a.setAccept(digits);
        a.addTransition(start, digits, new Interval(0, 9));
        a.addTransition(digits, digits, new Interval(0, 9));
        check(a.run(4, 2));
        check(!a.run());
        Automaton copy = a.clone();
        copy.setAccept(copy.getInitial());
        check(copy.run() && !a.run());
        // END construction
    }
    static void intervals() {
        // BEGIN intervals
        IntervalSet symbols = IntervalSet.fromInterval(new Interval(-5, 5));
        symbols.add(20, 30);
        check(symbols.contains(-3));
        check(symbols.size() == 13L);
        IntervalSet withoutZero = symbols.minus(IntervalSet.fromIntArray(0));
        check(!withoutZero.contains(0) && symbols.contains(0));
        check(IntervalSet.ALL.size() == 4_294_967_296L);
        Interval wide = new Interval(Integer.MIN_VALUE, Integer.MAX_VALUE);
        long cardinality = (long) wide.getMax() - wide.getMin() + 1;
        check(cardinality == 4_294_967_296L);
        // END intervals
    }
    static void regex() {
        // BEGIN regex
        Automaton sequence = Automaton.dfaFromString("12");
        Automaton symbol = Automaton.dfaFromString("<12>");
        check(sequence.run(1, 2) && !sequence.run(12));
        check(symbol.run(12) && !symbol.run(1, 2));
        RegExp expression = RegExpParser.toRegExp("<10><-3>.*");
        Automaton nfa = expression.toNFA();
        check(nfa.run(10, -3, Integer.MAX_VALUE));
        check(Automaton.dfaFromString("").run());
        check(Automaton.dfaFromString("(1|2){2,3}").run(1, 2));
        // END regex
    }
    static void languages() {
        // BEGIN languages
        Automaton a = Automaton.dfaFromString("(1|2)*1");
        Automaton b = Automaton.dfaFromString("(1|2)*1(1|2)*");
        check(a.isSubsetOf(b) && !a.sameLanguage(b));
        check(!a.isFinite());
        check(a.countWords(3).equals(BigInteger.valueOf(4)));
        int[] witness = a.counterExample(b);
        check(a.run(witness) != b.run(witness));
        int[] sampled = a.randomWord(3, new Random(42));
        check(sampled.length == 3 && a.run(sampled));
        int count = 0;
        for (int[] word : a.words(2)) { check(a.run(word)); count++; }
        check(count == 3);
        Automaton joined = Automaton.dfaFromString("12").concatenate(Automaton.dfaFromString("34"));
        check(joined.run(1, 2, 3, 4));
        check(Automaton.dfaFromString("12").revert().run(2, 1));
        // END languages
    }
    static void compilation() {
        // BEGIN compilation
        Automaton a = Automaton.dfaFromString("12+");
        CompiledDfa compiled = a.compile();
        int[] text = {3, 1, 2, 2, 4};
        check(compiled.run(text, 1, 4));
        int state = compiled.step(compiled.getInitialState(), 1);
        state = compiled.step(state, 2);
        check(compiled.isAccept(state));
        Searcher search = a.searcher();
        Searcher.Match match = search.find(text);
        check(match.start() == 1 && match.end() == 4 && match.length() == 3);
        check(search.findAll(1, 2, 0, 1, 2, 2).size() == 2);
        // END compilation
    }
}
