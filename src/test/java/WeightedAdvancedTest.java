import fr.menana.automaton.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class WeightedAdvancedTest {
    private static double enumerateWord(WeightedAutomaton a, int s, int[] word, int at, double cost) {
        double result = at == word.length && a.graph().isAccept(s) ? cost + a.finalWeight(s) : Double.POSITIVE_INFINITY;
        for (int e : a.graph().outgoingEdges(s)) {
            boolean eps = a.graph().isEpsilon(e);
            if (eps || (at < word.length && a.graph().accepts(e, word[at])))
                result = Math.min(result, enumerateWord(a, a.graph().target(e), word, at + (eps ? 0 : 1), cost + a.weight(e)));
        }
        return result;
    }
    private static List<int[]> words() {
        List<int[]> result = new ArrayList<>(); result.add(new int[0]); int from = 0;
        for (int length = 1; length <= 3; length++) {
            int to = result.size();
            for (int i = from; i < to; i++) for (int symbol = 0; symbol < 3; symbol++) { int[] word = Arrays.copyOf(result.get(i), length); word[length - 1] = symbol; result.add(word); }
            from = to;
        }
        return result;
    }
    @Test public void residualDeterminizationMinimizationAndEpsilonRemovalAgainstEnumeration() {
        Random random = new Random(7711);
        for (int round = 0; round < 100; round++) {
            WeightedAutomaton.Builder b = WeightedAutomaton.builder(); int n = 2 + random.nextInt(6);
            for (int s = 0; s < n; s++) { b.addState(); if (random.nextBoolean()) b.setAccept(s, random.nextInt(9) - 4); }
            b.setInitial(0, random.nextInt(7) - 3);
            for (int i = 0; i < 15; i++) {
                int s = random.nextInt(n - 1), t = s + 1 + random.nextInt(n - s - 1); double w = random.nextInt(11) - 5;
                if (random.nextInt(4) == 0) b.addEpsilonTransition(s, t, w);
                else { int lo = random.nextInt(3); b.addTransition(s, t, new Interval(lo, lo + random.nextInt(3 - lo)), w); }
            }
            WeightedAutomaton a = b.build(), noEps = a.removeEpsilon(), dfa = a.determinize(), minimal = dfa.minimize();
            assertTrue(dfa.graph().isDeterministic()); assertTrue(minimal.graph().isDeterministic());
            assertTrue(minimal.graph().getNbStates() <= dfa.graph().getNbStates());
            for (int e = 0; e < noEps.graph().getNbTransitions(); e++) assertFalse(noEps.graph().isEpsilon(e));
            for (int[] word : words()) {
                double expected = enumerateWord(a, 0, word, 0, a.initialWeight());
                assertEquals(expected, noEps.cost(word), 0); assertEquals(expected, dfa.cost(word), 0); assertEquals(expected, minimal.cost(word), 0);
            }
        }
    }
    @Test public void weightedMinimizationPushesDifferentOffsetsWithoutLosingCosts() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int i = 0; i < 5; i++) b.addState();
        b.setInitial(0); b.setAccept(3, 4); b.setAccept(4, 1);
        b.addTransition(0, 1, 0, 2); b.addTransition(0, 2, 1, 3); b.addTransition(1, 3, 2, 1); b.addTransition(2, 4, 2, 4);
        WeightedAutomaton a = b.build(), min = a.minimize();
        assertEquals(3, min.graph().getNbStates()); assertEquals(7, min.cost(0, 2), 0); assertEquals(8, min.cost(1, 2), 0);
        assertEquals(5, a.graph().getNbStates());
        assertEquals(min.graph().getNbStates(), min.minimize().graph().getNbStates());
    }
    private record CostPath(double cost, String edges) { }
    private static void paths(WeightedAutomaton a, int s, int remaining, double cost, List<Integer> prefix, List<CostPath> result) {
        if ((remaining < 0 || remaining == 0) && a.graph().isAccept(s)) result.add(new CostPath(cost + a.finalWeight(s), prefix.toString()));
        if (remaining == 0) return;
        for (int e : a.graph().outgoingEdges(s)) {
            prefix.add(e); paths(a, a.graph().target(e), remaining < 0 ? -1 : remaining - 1, cost + a.weight(e), prefix, result); prefix.remove(prefix.size() - 1);
        }
    }
    @Test public void kBestDagsAndBoundedWalksAgainstExhaustiveLists() {
        Random random = new Random(7712);
        for (int round = 0; round < 80; round++) {
            WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int i = 0; i < 6; i++) b.addState(); b.setInitial(0, 2); b.setAccept(5, 3);
            for (int i = 0; i < 15; i++) { int s = random.nextInt(5), t = s + 1 + random.nextInt(5 - s); b.addTransition(s, t, 1, random.nextInt(9) - 4); }
            WeightedAutomaton a = b.build(); List<CostPath> all = new ArrayList<>(); paths(a, 0, -1, 2, new ArrayList<>(), all); all.sort(Comparator.comparingDouble(CostPath::cost));
            List<WeightedPath> actual = a.kBestPaths(12); assertEquals(Math.min(12, all.size()), actual.size());
            Set<String> distinct = new HashSet<>();
            for (int i = 0; i < actual.size(); i++) { assertEquals(all.get(i).cost, actual.get(i).cost(), 0); distinct.add(Arrays.toString(actual.get(i).edges())); }
            assertEquals(actual.size(), distinct.size());
        }
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); b.addState(); b.addState(); b.setInitial(0); b.setAccept(1);
        b.addTransition(0, 0, 1, 2); b.addTransition(0, 1, 1, 1); b.addTransition(1, 0, 1, -1); b.addTransition(1, 1, 1, 3);
        WeightedAutomaton cyclic = b.build(); List<CostPath> all = new ArrayList<>(); paths(cyclic, 0, 4, 0, new ArrayList<>(), all); all.sort(Comparator.comparingDouble(CostPath::cost));
        List<WeightedPath> k = KBestPaths.ofLength(cyclic, 4, 5); assertEquals(5, k.size());
        for (int i = 0; i < k.size(); i++) { assertEquals(all.get(i).cost, k.get(i).cost(), 0); assertEquals(4, k.get(i).length()); }
        assertEquals(2, cyclic.kBestPaths(2).size());
    }
    @Test public void weightedCompilationAndReweightingReuseTheGraph() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); b.addState(); b.addState(); b.setInitial(0, 1); b.setAccept(1, -1);
        IntervalSet labels = IntervalSet.fromInterval(new Interval(Integer.MIN_VALUE, -1)); labels.add(new Interval(5, Integer.MAX_VALUE));
        b.addTransition(0, 1, labels, 2.5); b.addTransition(0, 1, new Interval(0, 4), 4);
        WeightedAutomaton a = b.build(); CompiledWeightedDfa compiled = a.compile();
        for (int symbol : new int[]{Integer.MIN_VALUE, -1, 0, 4, 5, Integer.MAX_VALUE}) assertEquals(a.cost(symbol), compiled.cost(symbol), 0);
        assertEquals(2.5, compiled.cost(new int[]{99, -1, 99}, 1, 2), 0);
        assertEquals(Double.POSITIVE_INFINITY, compiled.cost(), 0); assertEquals(7, compiled.reweight(a.withWeights(new double[]{7, 9})).cost(-1), 0);
        assertThrows(IndexOutOfBoundsException.class, () -> compiled.cost(new int[]{1}, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> compiled.reweight(WeightedAutomaton.builder().build()));
    }
    @Test public void unsupportedFamiliesAndBudgetsFailExplicitly() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); b.addState(); b.addState(); b.setInitial(0); b.setAccept(1);
        b.addTransition(0, 1, 1, 0.5); b.addTransition(0, 1, 1, 1);
        WeightedAutomaton decimal = b.build(); assertEquals(0.5, decimal.determinize().cost(1), 0);
        assertThrows(IllegalArgumentException.class, decimal::compile);
        WeightedAutomaton integral = decimal.withWeights(new double[]{1, 2});
        assertThrows(IllegalStateException.class, () -> WeightedTransformations.determinizeDag(integral, 1));
        b = WeightedAutomaton.builder(); b.addState(); b.addState(); b.setInitial(0); b.setAccept(1); b.addEpsilonTransition(0, 0, -1); b.addTransition(0, 1, 2, 0);
        WeightedAutomaton cycle = b.build(); assertThrows(IllegalArgumentException.class, cycle::removeEpsilon); assertThrows(IllegalArgumentException.class, cycle::determinize);
        assertThrows(IllegalArgumentException.class, () -> integral.kBestPaths(-1));
    }
    @Test public void kBestUsesExactFallbackForCancellation() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int i = 0; i < 4; i++) b.addState(); b.setInitial(0); b.setAccept(3);
        b.addTransition(0, 1, 1, 1e16); b.addTransition(1, 2, 1, 1); b.addTransition(2, 3, 1, -1e16); b.addTransition(0, 3, 2, 0.5);
        List<WeightedPath> paths = b.build().kBestPaths(2); assertEquals(0.5, paths.get(0).cost(), 0); assertEquals(1, paths.get(1).cost(), 0);
        assertArrayEquals(new int[]{2}, paths.get(0).word());
    }
}
