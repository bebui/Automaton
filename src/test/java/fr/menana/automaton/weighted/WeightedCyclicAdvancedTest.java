package fr.menana.automaton.weighted;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.compiled.*;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.Assert.*;

public class WeightedCyclicAdvancedTest {
    private record Walk(BigDecimal cost, String edges) { }
    private static BigDecimal add(BigDecimal a, double b) { return a.add(new BigDecimal(b)); }
    private static void enumerate(WeightedAutomaton a, int state, int left, BigDecimal cost,
                                  List<Integer> prefix, List<Walk> result) {
        if (a.graph().isAccept(state)) result.add(new Walk(add(cost, a.finalWeight(state)), prefix.toString()));
        if (left == 0) return;
        for (int e : a.graph().outgoingEdges(state)) {
            prefix.add(e);
            enumerate(a, a.graph().target(e), left - 1, add(cost, a.weight(e)), prefix, result);
            prefix.remove(prefix.size() - 1);
        }
    }
    private static BigDecimal pathCost(WeightedAutomaton a, WeightedPath path) {
        BigDecimal result = new BigDecimal(a.initialWeight());
        for (int e : path.edges()) result = add(result, a.weight(e));
        return add(result, a.finalWeight(path.end()));
    }
    private static List<int[]> words(int alphabet, int depth) {
        List<int[]> result = new ArrayList<>(); result.add(new int[0]); int from = 0;
        for (int length = 1; length <= depth; length++) {
            int to = result.size();
            for (int i = from; i < to; i++) for (int c = 0; c < alphabet; c++) {
                int[] word = Arrays.copyOf(result.get(i), length); word[length - 1] = c; result.add(word);
            }
            from = to;
        }
        return result;
    }
    // Independent exact Floyd-Warshall epsilon closure followed by word DP.
    private static BigDecimal wordCost(WeightedAutomaton a, int[] word) {
        AutomatonGraph g = a.graph(); int n = g.getNbStates();
        if (g.getInitialState() < 0) return null;
        BigDecimal[][] eps = new BigDecimal[n][n];
        for (int s = 0; s < n; s++) eps[s][s] = BigDecimal.ZERO;
        for (int e = 0; e < g.getNbTransitions(); e++) if (g.isEpsilon(e)) {
            int s = g.source(e), t = g.target(e); BigDecimal w = new BigDecimal(a.weight(e));
            if (eps[s][t] == null || w.compareTo(eps[s][t]) < 0) eps[s][t] = w;
        }
        for (int k = 0; k < n; k++) for (int s = 0; s < n; s++) for (int t = 0; t < n; t++)
            if (eps[s][k] != null && eps[k][t] != null) {
                BigDecimal v = eps[s][k].add(eps[k][t]);
                if (eps[s][t] == null || v.compareTo(eps[s][t]) < 0) eps[s][t] = v;
            }
        BigDecimal[] current = new BigDecimal[n]; current[g.getInitialState()] = new BigDecimal(a.initialWeight());
        for (int position = 0; position <= word.length; position++) {
            BigDecimal[] closed = new BigDecimal[n];
            for (int s = 0; s < n; s++) for (int t = 0; t < n; t++) if (current[s] != null && eps[s][t] != null) {
                BigDecimal v = current[s].add(eps[s][t]);
                if (closed[t] == null || v.compareTo(closed[t]) < 0) closed[t] = v;
            }
            if (position == word.length) {
                BigDecimal best = null;
                for (int s = 0; s < n; s++) if (closed[s] != null && g.isAccept(s)) {
                    BigDecimal v = add(closed[s], a.finalWeight(s));
                    if (best == null || v.compareTo(best) < 0) best = v;
                }
                return best;
            }
            current = new BigDecimal[n];
            for (int e = 0; e < g.getNbTransitions(); e++) if (closed[g.source(e)] != null && g.accepts(e, word[position])) {
                BigDecimal v = add(closed[g.source(e)], a.weight(e)); int t = g.target(e);
                if (current[t] == null || v.compareTo(current[t]) < 0) current[t] = v;
            }
        }
        throw new AssertionError();
    }
    private static void sameCosts(WeightedAutomaton a, WeightedAutomaton b, List<int[]> words) {
        for (int[] word : words) {
            BigDecimal expected = wordCost(a, word), actual = wordCost(b, word);
            if (expected == null) assertNull(actual);
            else { assertNotNull(actual); assertEquals(Arrays.toString(word), 0, expected.compareTo(actual)); }
        }
    }
    // Independent product exploration: equal languages and a constant difference
    // between all corresponding accepted suffix costs. No normalization/refinement.
    private static boolean equivalentUpToOffset(WeightedAutomaton a, int left, int right) {
        AutomatonGraph g = a.graph(); int n = g.getNbStates();
        BigDecimal[] deltas = new BigDecimal[n * n]; Deque<Integer> work = new ArrayDeque<>();
        int root = left * n + right; deltas[root] = BigDecimal.ZERO; work.add(root); BigDecimal offset = null;
        while (!work.isEmpty()) {
            int pair = work.remove(), s = pair / n, t = pair % n;
            if (g.isAccept(s) != g.isAccept(t)) return false;
            if (g.isAccept(s)) {
                BigDecimal value = add(add(deltas[pair], a.finalWeight(s)), -a.finalWeight(t));
                if (offset == null) offset = value; else if (offset.compareTo(value) != 0) return false;
            }
            for (int symbol = 0; symbol < 2; symbol++) {
                int es = -1, et = -1;
                for (int e : g.outgoingEdges(s)) if (g.accepts(e, symbol)) es = e;
                for (int e : g.outgoingEdges(t)) if (g.accepts(e, symbol)) et = e;
                if ((es < 0) != (et < 0)) return false;
                if (es < 0) continue;
                int next = g.target(es) * n + g.target(et);
                BigDecimal value = add(add(deltas[pair], a.weight(es)), -a.weight(et));
                if (deltas[next] == null) { deltas[next] = value; work.add(next); }
                else if (deltas[next].compareTo(value) != 0) return false;
            }
        }
        return true;
    }
    private static int minimalStateCount(WeightedAutomaton original) {
        WeightedAutomaton a = original.trim(); List<Integer> representatives = new ArrayList<>();
        for (int s = 0; s < a.graph().getNbStates(); s++) {
            boolean found = false;
            for (int t : representatives) if (equivalentUpToOffset(a, s, t)) { found = true; break; }
            if (!found) representatives.add(s);
        }
        return representatives.size();
    }
    @Test public void cyclicKWalksMatchExhaustiveBoundedOracle() {
        Random random = new Random(99120);
        for (int round = 0; round < 60; round++) {
            int n = 4; double[] p = new double[n];
            for (int s = 0; s < n; s++) p[s] = (random.nextInt(5) - 2) / 4.0;
            WeightedAutomaton.Builder b = WeightedAutomaton.builder();
            for (int s = 0; s < n; s++) { b.addState(); b.setAccept(s); }
            b.setInitial(0, 0.5);
            for (int s = 0; s < n; s++) for (int c = 0; c < 2; c++) {
                int t = random.nextInt(n); b.addTransition(s, t, c, 1 + random.nextInt(2) + p[s] - p[t]);
            }
            WeightedAutomaton a = b.build(); List<Walk> all = new ArrayList<>();
            enumerate(a, 0, 10, new BigDecimal(0.5), new ArrayList<>(), all);
            all.sort(Comparator.comparing(Walk::cost));
            // Longer walks cost at least .5 + 11 - 1, so this is an exhaustive oracle for the first 12.
            assertTrue(all.get(11).cost.compareTo(new BigDecimal("10.5")) < 0);
            List<WeightedPath> actual = a.kBestPaths(12); Set<String> seen = new HashSet<>();
            Iterator<WeightedPath> lazy = a.pathsByCost();
            for (int i = 0; i < 12; i++) {
                assertEquals(all.get(i).cost.doubleValue(), actual.get(i).cost(), 0);
                assertEquals(actual.get(i).cost(), pathCost(a, actual.get(i)).doubleValue(), 0);
                assertTrue(seen.add(Arrays.toString(actual.get(i).edges())));
                assertTrue(lazy.hasNext()); assertTrue(lazy.hasNext());
                assertEquals(actual.get(i).cost(), lazy.next().cost(), 0);
            }
        }
    }
    @Test public void signedCyclesBoundariesExactCancellationAndIrrelevantCycles() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int s = 0; s < 7; s++) b.addState();
        b.setInitial(0, -2); b.setAccept(3, 4);
        b.addTransition(0, 1, 1, 1e16); b.addTransition(1, 2, 2, 1); b.addTransition(2, 3, 3, -1e16);
        b.addTransition(3, 1, 4, 1e16); b.addTransition(0, 3, 5, 1.5);
        b.addEpsilonTransition(4, 4, -1); // unreachable
        b.addTransition(0, 5, 6, 0); b.addTransition(5, 5, 6, -1); // cannot accept
        WeightedAutomaton a = b.build(); List<WeightedPath> k = a.kBestPaths(4);
        assertEquals(3, k.get(0).cost(), 0); assertEquals(3.5, k.get(1).cost(), 0);
        assertEquals(4, k.get(2).cost(), 0); assertEquals(4.5, k.get(3).cost(), 0);
        for (WeightedPath path : k) assertEquals(pathCost(a, path).doubleValue(), path.cost(), 0);
        assertEquals(3, a.kBestWords(1).get(0).cost(), 0);
        b = WeightedAutomaton.builder(); b.addState(); b.setInitial(0); b.setAccept(0); b.addTransition(0, 0, 1, -Double.MIN_VALUE);
        WeightedAutomaton unbounded = b.build();
        assertThrows(IllegalArgumentException.class, unbounded::pathsByCost);
        assertThrows(IllegalArgumentException.class, () -> unbounded.kBestPaths(1));
        assertTrue(unbounded.kBestPaths(0).isEmpty());
    }
    @Test(timeout=5000) public void zeroCyclesAreFairAndHorizonCountsSymbols() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); b.addState(); b.setInitial(0, 2); b.setAccept(0, 3);
        b.addEpsilonTransition(0, 0, 0); b.addTransition(0, 0, 7, 0);
        WeightedAutomaton a = b.build(); Iterator<WeightedPath> paths = a.pathsByCost();
        Set<String> seen = new HashSet<>(); int previous = -1;
        for (int i = 0; i < 50; i++) {
            WeightedPath path = paths.next(); assertEquals(5, path.cost(), 0);
            assertTrue(path.edgeCount() >= previous); previous = path.edgeCount();
            assertTrue(seen.add(Arrays.toString(path.edges())));
        }
        List<WeightedPath> fixed = KBestPaths.ofLength(a, 2, 10);
        for (WeightedPath path : fixed) { assertEquals(2, path.length()); assertSame(a.graph(), path.graph()); }
        List<WeightedWord> words = KBestWords.ofLength(a, 2, 10);
        assertEquals(1, words.size()); assertArrayEquals(new int[]{7,7}, words.get(0).word());
        assertEquals(5, words.get(0).cost(), 0);
    }
    @Test(timeout=5000) public void lazinessAvoidsExponentialPrefixExpansionAndEnumeratingIntervals() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int s = 0; s <= 80; s++) b.addState();
        b.setInitial(0); b.setAccept(80); b.addEpsilonTransition(0, 0, 0);
        for (int s = 0; s < 80; s++) { b.addTransition(s, s + 1, 1, 0); b.addTransition(s, s + 1, 2, 0); }
        WeightedAutomaton a = b.build(); assertEquals(80, a.pathsByCost().next().length());
        assertEquals(80, a.wordsByCost().next().length());
        b = WeightedAutomaton.builder(); b.addState(); b.addState(); b.setInitial(0); b.setAccept(1);
        b.addTransition(0, 1, new Interval(Integer.MIN_VALUE, Integer.MAX_VALUE), 0.5);
        a = b.build(); List<WeightedWord> words = a.kBestWords(4);
        for (int i = 0; i < 4; i++) assertArrayEquals(new int[]{Integer.MIN_VALUE + i}, words.get(i).word());
        assertEquals(1, a.kBestPaths(9).size());
        b = WeightedAutomaton.builder(); b.addState(); b.addState(); b.setInitial(0); b.setAccept(1);
        IntervalSet set = IntervalSet.fromInterval(new Interval(Integer.MIN_VALUE, Integer.MIN_VALUE));
        set.add(new Interval(Integer.MAX_VALUE - 1, Integer.MAX_VALUE)); b.addTransition(0, 1, set, 1);
        Iterator<WeightedWord> it = b.build().wordsByCost();
        assertEquals(Integer.MIN_VALUE, it.next().word()[0]); assertEquals(Integer.MAX_VALUE - 1, it.next().word()[0]);
        assertEquals(Integer.MAX_VALUE, it.next().word()[0]); assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next); assertThrows(UnsupportedOperationException.class, it::remove);
    }
    @Test public void cyclicResidualDeterminizationAgainstIndependentWordOracle() {
        Random random = new Random(99121); List<int[]> words = words(3, 5);
        for (int round = 0; round < 45; round++) {
            int n = 4; double[] p = new double[n], c = {-0.5, 0.25, 0.75};
            for (int s = 0; s < n; s++) p[s] = (random.nextInt(9) - 4) / 4.0;
            WeightedAutomaton.Builder b = WeightedAutomaton.builder();
            for (int s = 0; s < n; s++) { b.addState(); b.setAccept(s, p[s] + 0.5); } b.setInitial(0, 0.25);
            for (int s = 0; s < n; s++) {
                for (int i = 0; i < 4; i++) {
                    int t = random.nextInt(n), symbol = random.nextInt(3);
                    b.addTransition(s, t, symbol, c[symbol] + p[s] - p[t]);
                }
                int t = (s + 1) % n; b.addEpsilonTransition(s, t, p[s] - p[t]);
            }
            WeightedAutomaton a = b.build(), d = a.determinize(128), m = d.minimize();
            assertTrue(d.graph().isDeterministic()); assertTrue(m.graph().isDeterministic());
            sameCosts(a, d, words); sameCosts(a, m, words);
            assertEquals(m.graph().getNbStates(), m.minimize().graph().getNbStates());
        }
    }
    @Test public void cyclicMinimizationSignedDyadicsAndIndependentOffsets() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int s = 0; s < 3; s++) b.addState();
        b.setInitial(0, 0.5); b.setAccept(1, 1.25); b.setAccept(2, -0.75);
        b.addTransition(0, 1, 0, 0.25); b.addTransition(0, 2, 1, 0.5);
        b.addTransition(1, 1, new Interval(0, 2), -0.5); b.addTransition(2, 2, new Interval(0, 2), -0.5);
        WeightedAutomaton a = b.build(), m = a.minimize(); assertEquals(2, m.graph().getNbStates());
        sameCosts(a, m, words(3, 6)); assertEquals(Double.NEGATIVE_INFINITY, m.optimalCost(), 0);
        Random random = new Random(99122);
        for (int round = 0; round < 80; round++) {
            b = WeightedAutomaton.builder(); int n = 2 + random.nextInt(5);
            for (int s = 0; s < n; s++) b.addState(); b.setInitial(0, 0.25); b.setAccept(n - 1, -0.5);
            for (int s = 0; s < n; s++) {
                if (random.nextBoolean()) b.setAccept(s, random.nextInt(9) / 4.0);
                for (int symbol = 0; symbol < 2; symbol++) b.addTransition(s, random.nextInt(n), symbol, (random.nextInt(17) - 8) / 4.0);
            }
            a = b.build(); m = a.minimize(); sameCosts(a, m, words(2, 7));
            assertEquals(minimalStateCount(a), m.graph().getNbStates());
            assertEquals(m.graph().getNbStates(), m.minimize().graph().getNbStates());
        }
    }
    @Test public void kDistinctWordsAgainstAllWordsWithAmbiguityAndBoundedNegativeCycles() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int i = 0; i < 3; i++) b.addState();
        b.setInitial(0, 1.25); b.setAccept(1, 0.5); b.setAccept(2, 0.25);
        b.addEpsilonTransition(0, 0, 0); b.addTransition(0, 1, new Interval(0, 2), 0.75);
        b.addTransition(0, 2, new Interval(0, 2), 1.5); b.addTransition(1, 1, new Interval(0, 2), 1);
        b.addTransition(2, 2, new Interval(0, 2), 1);
        WeightedAutomaton a = b.build(); List<WeightedWord> actual = a.kBestWords(30);
        List<BigDecimal> reference = new ArrayList<>();
        for (int[] word : words(3, 3)) { BigDecimal cost = wordCost(a, word); if (cost != null) reference.add(cost); }
        reference.sort(BigDecimal::compareTo); Set<String> seen = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            WeightedWord word = actual.get(i); assertTrue(seen.add(Arrays.toString(word.word())));
            assertEquals(reference.get(i).doubleValue(), word.cost(), 0);
            assertEquals(wordCost(a, word.word()).doubleValue(), word.cost(), 0);
            int[] copy = word.word(); copy[0] = 99; assertNotEquals(99, word.word()[0]);
        }
        b = WeightedAutomaton.builder(); b.addState(); b.setInitial(0); b.setAccept(0); b.addTransition(0, 0, new Interval(0, 1), -1);
        a = b.build(); assertThrows(IllegalArgumentException.class, a::wordsByCost);
        List<WeightedWord> fixed = KBestWords.ofLength(a, 3, 20); assertEquals(8, fixed.size());
        for (WeightedWord word : fixed) { assertEquals(3, word.length()); assertEquals(-3, word.cost(), 0); }
    }
    @Test public void randomizedUnboundedNfaWordsAgainstCompleteCostBound() {
        Random random = new Random(99124);
        for (int round = 0; round < 40; round++) {
            WeightedAutomaton.Builder b = WeightedAutomaton.builder();
            for (int state = 0; state < 4; state++) { b.addState(); b.setAccept(state); }
            b.setInitial(0); b.addEpsilonTransition(0,0,0);
            for (int state = 0; state < 4; state++) for (int symbol = 0; symbol < 2; symbol++) for (int j = 0; j < 2; j++)
                b.addTransition(state,random.nextInt(4),symbol,1+random.nextInt(2));
            WeightedAutomaton a = b.build(); List<BigDecimal> expected = new ArrayList<>();
            for (int[] word : words(2,6)) expected.add(wordCost(a,word));
            expected.sort(BigDecimal::compareTo);
            // All 15 words of length <= 3 cost <= 6; length >= 7 costs >= 7.
            assertTrue(expected.get(14).compareTo(new BigDecimal(7)) < 0);
            List<WeightedWord> actual = a.kBestWords(15); Set<String> seen = new HashSet<>();
            for (int i = 0; i < 15; i++) {
                assertEquals(expected.get(i).doubleValue(),actual.get(i).cost(),0);
                assertEquals(wordCost(a,actual.get(i).word()).doubleValue(),actual.get(i).cost(),0);
                assertTrue(seen.add(Arrays.toString(actual.get(i).word())));
            }
        }
    }
    @Test public void randomizedDistinctWordsAtSignedHorizons() {
        Random random = new Random(99123);
        for (int round = 0; round < 70; round++) {
            WeightedAutomaton.Builder b = WeightedAutomaton.builder();
            for (int s = 0; s < 3; s++) b.addState();
            b.setInitial(0, -0.25); b.setAccept(2, 0.5);
            for (int s = 0; s < 3; s++) {
                b.addEpsilonTransition(s, s, 0);
                for (int j = 0; j < 4; j++) b.addTransition(s, random.nextInt(3), random.nextInt(2), (random.nextInt(17) - 8) / 4.0);
            }
            WeightedAutomaton a = b.build(); List<BigDecimal> expected = new ArrayList<>();
            for (int[] word : words(2, 3)) if (word.length == 3) {
                BigDecimal cost = wordCost(a, word); if (cost != null) expected.add(cost);
            }
            expected.sort(BigDecimal::compareTo); List<WeightedWord> actual = KBestWords.ofLength(a, 3, 20);
            assertEquals(expected.size(), actual.size()); Set<String> seen = new HashSet<>();
            for (int i = 0; i < actual.size(); i++) {
                assertEquals(expected.get(i).doubleValue(), actual.get(i).cost(), 0);
                assertEquals(wordCost(a, actual.get(i).word()).doubleValue(), actual.get(i).cost(), 0);
                assertTrue(seen.add(Arrays.toString(actual.get(i).word())));
            }
        }
    }
    @Test public void budgetsNumericalRejectionEmptyInputsAndNoMutation() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int s = 0; s < 3; s++) b.addState(); b.setInitial(0);
        b.setAccept(1); b.setAccept(2); b.addEpsilonTransition(0, 1, 0); b.addEpsilonTransition(0, 2, 0);
        b.addTransition(1, 1, 1, 0); b.addTransition(2, 2, 1, 1);
        WeightedAutomaton growing = b.build();
        assertThrows(IllegalStateException.class, () -> growing.determinize(8));
        assertEquals(3, growing.kBestWords(3).size());
        assertEquals(0, growing.kBestWords(3).get(2).cost(), 0);
        assertThrows(IllegalArgumentException.class, () -> growing.determinize(0));
        assertThrows(IllegalArgumentException.class, () -> growing.kBestWords(-1));
        assertEquals(4, growing.graph().getNbTransitions());
        b = WeightedAutomaton.builder(); b.addState(); b.setInitial(0); b.setAccept(0); b.addEpsilonTransition(0, 0, -0.25);
        WeightedAutomaton negativeEps = b.build(); assertThrows(IllegalArgumentException.class, negativeEps::determinize);
        b = WeightedAutomaton.builder(); b.addState(); b.addState(); b.setInitial(0); b.setAccept(1, 0.2); b.addEpsilonTransition(0, 1, 0.1);
        WeightedAutomaton nonrepresentable = b.build(); assertThrows(ArithmeticException.class, nonrepresentable::determinize);
        assertEquals(new BigDecimal(0.1).add(new BigDecimal(0.2)).doubleValue(), nonrepresentable.kBestWords(1).get(0).cost(), 0);
        b = WeightedAutomaton.builder(); b.addState(); b.addState(); b.setInitial(0, 0.1); b.setAccept(1); b.addTransition(0, 1, 1, 0.2);
        nonrepresentable = b.build(); assertThrows(ArithmeticException.class, nonrepresentable::minimize);
        WeightedAutomaton empty = WeightedAutomaton.builder().build();
        assertFalse(empty.pathsByCost().hasNext()); assertFalse(empty.wordsByCost().hasNext());
        assertTrue(empty.kBestWords(2).isEmpty()); assertEquals(0, empty.minimize().graph().getNbStates());
        b = WeightedAutomaton.builder(); b.addState(); b.setInitial(0); b.setAccept(0, Double.MAX_VALUE); b.addTransition(0, 0, 1, Double.MAX_VALUE);
        Iterator<WeightedPath> overflow = b.build().pathsByCost(); assertEquals(Double.MAX_VALUE, overflow.next().cost(), 0);
        assertThrows(ArithmeticException.class, overflow::next);
    }
}
