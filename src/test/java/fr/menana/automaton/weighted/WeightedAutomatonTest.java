package fr.menana.automaton.weighted;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.compiled.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Independent path enumeration on DAGs and Floyd-Warshall on small cyclic graphs. */
public class WeightedAutomatonTest {
    private record Arc(int from, int to, int low, int high, double cost, boolean eps) { }
    private record Fixture(WeightedAutomaton a, List<Arc> arcs, boolean[] finals, double[] terminal, double initial) { }

    private static double exhaustive(Fixture f, int s, int[] word, int pos, double cost) {
        double best = f.finals[s] && (word == null || pos == word.length) ? cost + f.terminal[s] : Double.POSITIVE_INFINITY;
        for (Arc e : f.arcs) if (e.from == s) {
            if (e.eps) best = Math.min(best, exhaustive(f, e.to, word, pos, cost + e.cost));
            else if (word == null || (pos < word.length && word[pos] >= e.low && word[pos] <= e.high))
                best = Math.min(best, exhaustive(f, e.to, word, pos + 1, cost + e.cost));
        }
        return best;
    }
    private static List<int[]> words(int max) {
        List<int[]> result = new ArrayList<>(); result.add(new int[0]); int start = 0;
        for (int l = 1; l <= max; l++) {
            int end = result.size();
            for (int i = start; i < end; i++) for (int v : new int[]{-1, 0, 2}) {
                int[] w = Arrays.copyOf(result.get(i), l); w[l - 1] = v; result.add(w);
            }
            start = end;
        }
        return result;
    }
    private static Fixture dag(Random random) {
        int n = 2 + random.nextInt(6); WeightedAutomaton.Builder b = WeightedAutomaton.builder();
        boolean[] finals = new boolean[n]; double[] terminal = new double[n];
        double initial = random.nextInt(7) - 3;
        for (int s = 0; s < n; s++) {
            b.addState(); finals[s] = random.nextBoolean(); terminal[s] = random.nextInt(7) - 3;
            if (finals[s]) b.setAccept(s, terminal[s]);
        }
        b.setInitial(0, initial); List<Arc> arcs = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            int from = random.nextInt(n - 1), to = from + 1 + random.nextInt(n - from - 1);
            int lo = random.nextInt(4) - 1, hi = lo + random.nextInt(3); double cost = random.nextInt(11) - 5;
            boolean eps = random.nextInt(5) == 0;
            if (eps) b.addEpsilonTransition(from, to, cost); else b.addTransition(from, to, new Interval(lo, hi), cost);
            arcs.add(new Arc(from, to, lo, hi, cost, eps));
        }
        return new Fixture(b.build(), arcs, finals, terminal, initial);
    }
    private static void validatePath(WeightedAutomaton a, WeightedPath p, double expected, boolean terminal) {
        if (expected == Double.POSITIVE_INFINITY) { assertNull(p); return; }
        assertNotNull(p); int s = p.start(), pos = 0; double sum = a.initialWeight();
        int[] word = p.word();
        for (int e : p.edges()) {
            assertEquals(s, a.graph().source(e));
            if (!a.graph().isEpsilon(e)) assertTrue(a.graph().accepts(e, word[pos++]));
            sum += a.weight(e); s = a.graph().target(e);
        }
        assertEquals(word.length, pos); assertEquals(s, p.end());
        if (terminal) { assertTrue(a.graph().isAccept(s)); sum += a.finalWeight(s); }
        assertEquals(expected, sum, 0); assertEquals(expected, p.cost(), 0);
    }

    @Test public void stableParallelEdgesDefensiveCopiesAndImport() {
        AutomatonGraph.Builder b = AutomatonGraph.builder(); int p = b.addState(), q = b.addState();
        b.setInitial(p); b.setAccept(q);
        IntervalSet labels = IntervalSet.ALL.clone();
        assertEquals(0, b.addTransition(p, q, labels)); assertEquals(1, b.addTransition(p, q, 4));
        AutomatonGraph g = b.build(); labels.remove(IntervalSet.ALL); b.addState();
        assertEquals(2, g.getNbStates()); assertEquals(2, g.getNbTransitions()); assertFalse(g.isDeterministic());
        double[] weights = {3.5, -2}; WeightedAutomaton a = WeightedAutomaton.of(g, weights); weights[1] = 999;
        assertEquals(-2, a.cost(4), 0); assertEquals(3.5, a.cost(Integer.MIN_VALUE), 0); assertEquals(3.5, a.cost(Integer.MAX_VALUE), 0);
        g.labels(0).remove(IntervalSet.ALL); assertEquals(3.5, a.cost(Integer.MAX_VALUE), 0);
        assertSame(g, a.withWeights(new double[]{1, 2}).graph());
        assertEquals(-2, a.cost(4), 0);
        Automaton source = new Automaton(); State s = source.addState(), t = source.addState(); source.setInitial(s); source.setAccept(t);
        source.addTransition(s, t, 4); source.addEpsilonTransition(s, t);
        AutomatonGraph.ImportResult imported = AutomatonGraph.importAutomaton(source);
        assertEquals(2, imported.graph().getNbTransitions()); assertSame(imported.originalTransition(0), imported.originalTransition(1));
        WeightedAutomaton snapshot = WeightedAutomaton.from(source, edge -> 2);
        source.getInitial().getTransitions().clear(); assertEquals(2, snapshot.cost(4), 0); assertEquals(2, snapshot.cost(), 0);
        assertEquals(2, g.topologicalOrder().length); int[] order = g.topologicalOrder(); order[0] = 99; assertEquals(0, g.topologicalOrder()[0]);
    }

    @Test public void exhaustiveDagWordsDistancesAndWitnesses() {
        Random random = new Random(731);
        for (int iteration = 0; iteration < 180; iteration++) {
            Fixture f = dag(random); WeightedAutomaton a = f.a;
            double expected = exhaustive(f, 0, null, 0, f.initial);
            assertEquals(expected, a.optimalCost(), 0); validatePath(a, a.bestPath(), expected, true);
            WeightedAutomaton.Evaluator evaluator = a.evaluator();
            for (int[] word : words(3)) {
                double cost = exhaustive(f, 0, word, 0, f.initial);
                assertEquals(Arrays.toString(word), cost, a.cost(word), 0); assertEquals(cost, evaluator.cost(word), 0);
                if (iteration < 30) { WeightedPath path = a.bestPath(word); validatePath(a, path, cost, true); if (path != null) assertArrayEquals(word, path.word()); }
            }
            double[] backward = WeightedPaths.backwardDistances(a);
            for (int s = 0; s < f.finals.length; s++) assertEquals(exhaustive(f, s, null, 0, 0), backward[s], 0);
        }
    }

    @Test public void cyclicDistancesAgainstFloydWarshall() {
        Random random = new Random(732);
        for (int iteration = 0; iteration < 240; iteration++) {
            int n = 2 + random.nextInt(6); double inf = Double.POSITIVE_INFINITY;
            double[][] distance = new double[n][n];
            WeightedAutomaton.Builder b = WeightedAutomaton.builder();
            for (int s = 0; s < n; s++) { b.addState(); Arrays.fill(distance[s], inf); distance[s][s] = 0; }
            b.setInitial(0, 2); b.setAccept(n - 1, -3);
            for (int i = 0; i < 3 * n; i++) {
                int s = random.nextInt(n), t = random.nextInt(n); double w = random.nextInt(11) - (iteration % 2 == 0 ? 0 : 5);
                b.addTransition(s, t, i, w); distance[s][t] = Math.min(distance[s][t], w);
            }
            for (int k = 0; k < n; k++) for (int s = 0; s < n; s++) for (int t = 0; t < n; t++)
                distance[s][t] = Math.min(distance[s][t], distance[s][k] + distance[k][t]);
            WeightedAutomaton a = b.build(); ShortestPathResult actual = WeightedPaths.bellmanFord(a);
            for (int s = 0; s < n; s++) {
                double expected = distance[0][s] == inf ? inf : 2 + distance[0][s];
                for (int k = 0; k < n; k++) if (distance[0][k] != inf && distance[k][k] < 0 && distance[k][s] != inf) expected = Double.NEGATIVE_INFINITY;
                assertEquals(expected, actual.distanceTo(s), 0);
                assertEquals(expected, WeightedPaths.shortestPaths(a).distanceTo(s), 0);
                if (Double.isFinite(expected)) validatePath(a, actual.pathTo(s), expected, false);
            }
            if (iteration % 2 == 0) assertArrayEquals(actual.distances(), WeightedPaths.dijkstra(a).distances(), 0);
        }
    }

    @Test public void negativeEpsilonCyclesAreWordSpecificAndExact() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int i = 0; i < 4; i++) b.addState();
        b.setInitial(0); b.setAccept(3); b.addEpsilonTransition(0, 1, 0); b.addEpsilonTransition(1, 1, -1);
        b.addTransition(1, 3, 1, 0); b.addTransition(0, 3, 2, 5);
        WeightedAutomaton a = b.build();
        assertEquals(Double.NEGATIVE_INFINITY, a.cost(1), 0); assertEquals(5, a.cost(2), 0);
        assertEquals(Double.POSITIVE_INFINITY, a.cost(3), 0);
        assertThrows(IllegalStateException.class, () -> a.bestPath(1)); assertEquals(5, a.bestPath(2).cost(), 0);
        assertEquals(ShortestPathResult.Status.UNBOUNDED, WeightedPaths.shortestPaths(a).statusTo(3));
        assertThrows(IllegalArgumentException.class, () -> WeightedPaths.dijkstra(a));
        assertThrows(IllegalArgumentException.class, () -> a.graph().topologicalOrder());

        WeightedAutomaton.Builder zero = WeightedAutomaton.builder(); for (int i = 0; i < 5; i++) zero.addState();
        zero.setInitial(0); zero.setAccept(4);
        zero.addEpsilonTransition(0, 1, 1e16); zero.addEpsilonTransition(1, 2, 1);
        zero.addEpsilonTransition(2, 3, -1e16); zero.addEpsilonTransition(3, 0, -1);
        zero.addTransition(3, 4, 7, 0);
        assertEquals(1, zero.build().cost(7), 0); assertEquals(1, zero.build().optimalCost(), 0); assertEquals(1, zero.build().bestPath(7).cost(), 0);
        WeightedAutomaton.Builder tiny = WeightedAutomaton.builder(); tiny.addState(); tiny.setInitial(0); tiny.setAccept(0);
        tiny.addEpsilonTransition(0, 0, -Double.MIN_VALUE);
        assertEquals(Double.NEGATIVE_INFINITY, tiny.build().cost(), 0);
    }

    @Test public void boundariesEmptyGraphsLongestAndThroughCosts() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int i = 0; i < 4; i++) b.addState();
        b.setInitial(0, 2); b.setAccept(2, 3); b.addTransition(0, 1, 4, -5); b.addTransition(1, 2, 5, 1); b.addTransition(0, 2, 9, 4);
        WeightedAutomaton a = b.build();
        assertEquals(1, a.cost(4, 5), 0); assertEquals(9, WeightedPaths.dagLongestPaths(a).optimalCost(), 0);
        assertArrayEquals(new int[]{9}, WeightedPaths.dagLongestPaths(a).bestPath().word());
        assertEquals(1, WeightedPaths.costThroughState(a, 1), 0); assertEquals(9, WeightedPaths.costThroughTransition(a, 2), 0);
        assertEquals(1, WeightedPaths.analyze(a).costThroughState(1), 0);
        assertEquals(9, WeightedPaths.analyze(a).costThroughTransition(2), 0);
        assertEquals(1, a.optimalCostWithPrefix(4), 0); assertEquals(9, a.optimalCostWithPrefix(9), 0);
        assertEquals(Double.POSITIVE_INFINITY, a.optimalCostWithPrefix(8), 0);
        assertEquals(Double.POSITIVE_INFINITY, WeightedPaths.costThroughState(a, 3), 0);
        assertArrayEquals(new boolean[]{true, true, true, false}, a.graph().usefulStates());
        assertEquals(-4, WeightedPaths.distancesTo(a, 2)[0], 0);
        WeightedAutomaton empty = WeightedAutomaton.builder().build();
        assertEquals(Double.POSITIVE_INFINITY, empty.cost(), 0); assertEquals(Double.POSITIVE_INFINITY, empty.optimalCost(), 0); assertNull(empty.bestPath());
        assertThrows(IllegalStateException.class, () -> WeightedPaths.shortestPaths(a, false).bestPath());
    }

    @Test public void operationsPreserveWordCostsAndInputs() {
        Random random = new Random(733);
        for (int iteration = 0; iteration < 50; iteration++) {
            Fixture fa = dag(random), fb = dag(random); WeightedAutomaton a = fa.a, b = fb.a;
            WeightedAutomaton union = a.union(b), concat = a.concatenate(b), product = a.intersection(b), reverse = a.revert(), simple = WeightedOperations.simplify(a);
            for (int[] word : words(3)) {
                double ca = exhaustive(fa, 0, word, 0, fa.initial), cb = exhaustive(fb, 0, word, 0, fb.initial);
                assertEquals(Math.min(ca, cb), union.cost(word), 0);
                assertEquals(ca == Double.POSITIVE_INFINITY || cb == Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : ca + cb, product.cost(word), 0);
                double expectedConcat = Double.POSITIVE_INFINITY;
                for (int i = 0; i <= word.length; i++) {
                    double left = exhaustive(fa, 0, Arrays.copyOfRange(word, 0, i), 0, fa.initial);
                    double right = exhaustive(fb, 0, Arrays.copyOfRange(word, i, word.length), 0, fb.initial);
                    expectedConcat = Math.min(expectedConcat, left + right);
                }
                assertEquals(expectedConcat, concat.cost(word), 0);
                int[] reversed = word.clone(); for (int i = 0; i < word.length; i++) reversed[i] = word[word.length - i - 1];
                assertEquals(ca, reverse.cost(reversed), 0); assertEquals(ca, simple.cost(word), 0); assertEquals(ca, a.cost(word), 0);
            }
        }
    }

    @Test public void repetitionAndImprovingEmptyWord() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); b.addState(); b.addState(); b.setInitial(0, 1); b.setAccept(1, 2); b.addTransition(0, 1, 4, -1);
        WeightedAutomaton a = b.build();
        assertEquals(0, a.star().cost(), 0); assertEquals(6, a.star().cost(4, 4, 4), 0);
        WeightedAutomaton range = WeightedOperations.repeat(a, 2, 3);
        assertEquals(Double.POSITIVE_INFINITY, range.cost(4), 0); assertEquals(4, range.cost(4, 4), 0); assertEquals(6, range.cost(4, 4, 4), 0);
        b = WeightedAutomaton.builder(); b.addState(); b.setInitial(0); b.setAccept(0, -1);
        assertEquals(Double.NEGATIVE_INFINITY, b.build().star().cost(), 0);
    }

    @Test public void nonfiniteValuesAndOverflowAreRejected() {
        AutomatonGraph.Builder g = AutomatonGraph.builder(); g.addState(); g.setInitial(0); g.setAccept(0); g.addTransition(0, 0, 1);
        for (double bad : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> WeightedAutomaton.of(g.build(), new double[]{bad}));
        assertThrows(IllegalArgumentException.class, () -> WeightedAutomaton.of(g.build(), new double[0]));
        WeightedAutomaton a = WeightedAutomaton.of(g.build(), new double[]{Double.MAX_VALUE});
        assertThrows(ArithmeticException.class, () -> a.cost(1, 1));
        assertEquals(Double.doubleToRawLongBits(0.0), Double.doubleToRawLongBits(WeightedAutomaton.of(g.build(), new double[]{-0.0}).weight(0)));
    }
    @Test public void csrKeepsInsertionIdsAndQuasiEqualWeightsDistinct() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int i = 0; i < 4; i++) b.addState();
        b.setInitial(0); b.setAccept(3);
        int first = b.addTransition(2, 3, 5, 7);
        int second = b.addTransition(0, 2, 4, 1);
        int third = b.addTransition(0, 2, 4, Math.nextUp(1.0));
        WeightedAutomaton a = b.build(); assertEquals(0, first); assertEquals(1, second); assertEquals(2, third);
        assertArrayEquals(new int[]{1, 2}, a.graph().outgoingEdges(0)); assertArrayEquals(new int[]{0}, a.graph().outgoingEdges(2));
        assertTrue(a.weight(second) < a.weight(third)); assertEquals(3, WeightedOperations.simplify(a).graph().getNbTransitions());
        assertEquals(8, a.cost(4, 5), 0);
    }
}
