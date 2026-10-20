import fr.menana.automaton.*;
import fr.menana.automaton.regexp.*;
import java.math.BigInteger;
import java.util.*;

/** Executable examples used verbatim in the library documentation. */
public final class AutomatonTour {
    private static void check(boolean condition) { if (!condition) throw new AssertionError("Documentation example failed"); }
    private static void equal(double expected, double actual) { check(Double.compare(expected, actual) == 0); }
    public static void main(String[] args) {
        construction(); intervals(); regex(); languages(); compilation(); scalar();
        distances(); horizons(); vectors(); correlation(); advanced();
        System.out.println("Documentation examples passed: 11");
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
    static void scalar() {
        // BEGIN scalar
        WeightedAutomaton.Builder builder = WeightedAutomaton.builder();
        int from = builder.addState(), to = builder.addState();
        builder.setInitial(from, 0.5);
        builder.setAccept(to, 0.25);
        builder.addTransition(from, to, new Interval(0, 9), 3.5);
        builder.addTransition(from, to, new Interval(0, 9), 1.5);
        WeightedAutomaton a = builder.build();
        equal(2.25, a.cost(4));
        WeightedPath path = a.bestPath(4);
        equal(a.cost(4), path.cost());
        check(path.edgeCount() == 1 && path.length() == 1);
        check(a.bestPath(new int[0]) == null);
        equal(2.25, a.bestPath().cost());
        WeightedAutomaton.Evaluator evaluator = a.evaluator();
        equal(2.25, evaluator.cost(8));
        // END scalar
    }
    static void distances() {
        // BEGIN distances
        WeightedAutomaton.Builder builder = WeightedAutomaton.builder();
        int s = builder.addState(), middle = builder.addState(), end = builder.addState();
        builder.setInitial(s, 1);
        builder.setAccept(end, 0.5);
        int edge = builder.addTransition(s, middle, 1, -2);
        builder.addTransition(middle, end, 2, 4);
        builder.addTransition(s, end, 3, 5);
        WeightedAutomaton a = builder.build();
        ShortestPathResult result = WeightedPaths.shortestPaths(a);
        equal(3, result.distanceTo(end));
        equal(3.5, result.optimalCost());
        equal(6.5, WeightedPaths.dagLongestPaths(a).optimalCost());
        WeightedPaths.Analysis analysis = WeightedPaths.analyze(a);
        equal(3.5, analysis.costThroughState(middle));
        equal(3.5, analysis.costThroughTransition(edge));
        equal(2, WeightedPaths.shortestPaths(a, s).distanceTo(end));
        // END distances
    }
    static void horizons() {
        // BEGIN horizons
        WeightedAutomaton.Builder builder = WeightedAutomaton.builder();
        int state = builder.addState(); builder.setInitial(state); builder.setAccept(state);
        builder.addTransition(state, state, 1, 2);
        builder.addTransition(state, state, 2, 3);
        WeightedAutomaton a = builder.build();
        equal(6, a.costOfLength(3));
        equal(0, a.costOfLengthAtMost(3));
        LayeredAutomaton layers = LayeredAutomaton.exactly(a.graph(), 3);
        WeightedAutomaton changed = a.withWeights(new double[]{4, 1});
        equal(3, layers.evaluate(changed).optimalCost());
        check(layers.evaluate(a).graph() == layers.evaluate(changed).graph());
        WeightedAutomaton positioned = layers.evaluate(a, (position, edge) -> a.weight(edge) + (position == 1 ? 1 : 0));
        equal(7, positioned.optimalCost());
        equal(7, layers.analyze(a).costAt(1, 2));
        check(Arrays.equals(new int[]{1, 1, 1}, layers.bestPath(a).word()));
        // END horizons
    }
    static void vectors() {
        // BEGIN vectors
        WeightedAutomaton.Builder builder = WeightedAutomaton.builder();
        int start = builder.addState(), left = builder.addState(), right = builder.addState();
        builder.setInitial(start); builder.setAccept(left); builder.setAccept(right);
        builder.addTransition(start, left, 0, 5);
        builder.addTransition(start, right, 1, 10);
        WeightedAutomaton cost = builder.build();
        WeightedAutomaton duration = cost.withWeights(new double[]{6, 1});
        MultiWeightedAutomaton multi = MultiWeightedAutomaton.fromWeightings(cost, duration);
        check(multi.graph() == cost.graph());
        check(multi.paretoFront().size() == 2);
        check(Arrays.equals(new int[]{0}, multi.lexicographicBestPath().word()));
        check(Arrays.equals(new int[]{1}, multi.scalarize(1, 2).bestPath().word()));
        CompiledMultiWeightedDfa compiled = multi.compile();
        double[] output = new double[2];
        check(compiled.costs(new int[]{1}, output));
        check(Arrays.equals(new double[]{10, 1}, output));
        equal(12, compiled.scalarize(1, 2).cost(1));
        // END vectors
    }
    static void correlation() {
        // BEGIN correlation
        WeightedAutomaton.Builder builder = WeightedAutomaton.builder();
        int start = builder.addState(), end = builder.addState();
        builder.setInitial(start); builder.setAccept(end);
        builder.addTransition(start, end, 7, 0);
        builder.addTransition(start, end, 7, 10);
        WeightedAutomaton a = builder.build(), b = a.withWeights(new double[]{10, 0});
        MultiWeightedAutomaton correlated = MultiWeightedAutomaton.fromWeightings(a, b);
        MultiWeightedAutomaton independent = MultiWeightedAutomaton.synchronizedProduct(a, b);
        equal(10, correlated.scalarize(1, 1).cost(7));
        equal(0, independent.scalarize(1, 1).cost(7));
        check(correlated.paretoForWord(7).size() == 2);
        // END correlation
    }
    static void advanced() {
        // BEGIN advanced
        WeightedAutomaton.Builder builder = WeightedAutomaton.builder();
        int start = builder.addState(), end = builder.addState();
        builder.setInitial(start, 0.25); builder.setAccept(end, 0.5);
        builder.addEpsilonTransition(start, start, 0);
        builder.addTransition(start, end, 7, -0.25);
        builder.addTransition(end, end, 8, 0.5);
        WeightedAutomaton a = builder.build();
        List<WeightedPath> paths = a.kBestPaths(3);
        for (WeightedPath path : paths) equal(0.5, path.cost());
        Iterator<WeightedWord> words = a.wordsByCost();
        check(Arrays.equals(new int[]{7}, words.next().word()));
        check(Arrays.equals(new int[]{7, 8}, words.next().word()));
        WeightedAutomaton minimal = a.determinize(32).minimize();
        equal(1, minimal.cost(7, 8));
        equal(1, KBestWords.ofLength(a, 2, 2).get(0).cost());
        // END advanced
    }
}
