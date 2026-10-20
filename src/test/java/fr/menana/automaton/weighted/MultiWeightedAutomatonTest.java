package fr.menana.automaton.weighted;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.compiled.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MultiWeightedAutomatonTest {
    private static MultiWeightedAutomaton choice() {
        MultiWeightedAutomaton.Builder b = MultiWeightedAutomaton.builder(2);
        b.addState(); b.addState(); b.setInitial(0, 1, 2); b.setAccept(1, 3, 4);
        b.addTransition(0, 1, 7, 0, 10); b.addTransition(0, 1, 7, 5, 5);
        b.addTransition(0, 1, 7, 10, 0); b.addTransition(0, 1, 7, 6, 6); b.addTransition(0, 1, 7, 5, 5);
        return b.build();
    }
    private static Set<String> vectors(List<MultiWeightedPath> paths) {
        Set<String> result = new HashSet<>(); for (MultiWeightedPath p : paths) result.add(Arrays.toString(p.costs())); return result;
    }
    @Test public void scalarViewsLexicographyParetoAndEquality() {
        MultiWeightedAutomaton a = choice();
        assertSame(a.graph(), a.scalarize(1, 2).graph()); assertSame(a.graph(), a.project(0).graph());
        assertEquals(4, a.project(0).cost(7), 0); assertEquals(6, a.project(1).cost(7), 0);
        assertEquals(26, a.scalarize(1, 2).cost(7), 0); assertEquals(26, a.scalarizeMaterialized(1, 2).cost(7), 0);
        assertEquals(-8, a.scalarize(-1, 1).cost(7), 0);
        double[] coefficients = {1, 2}; WeightedAutomaton scalar = a.scalarize(coefficients); coefficients[0] = 999;
        assertEquals(26, scalar.cost(7), 0);
        assertArrayEquals(new double[]{4, 16}, a.lexicographicBestPath().costs(), 0);
        assertArrayEquals(new double[]{14, 6}, a.lexicographicBestPath(1, 0).costs(), 0);
        assertArrayEquals(new double[]{16, 4}, a.selectDimensions(1, 0).lexicographicBestPath(1, 0).costs(), 0);
        assertEquals(3, a.paretoFront().size()); assertEquals(4, a.paretoFront(true).size());
        assertTrue(a.paretoFront().isComplete()); assertFalse(a.paretoFront().includesAllPaths());
        assertEquals(vectors(a.paretoFront().paths()), vectors(a.paretoForWord(7).paths())); assertEquals(0, a.paretoForWord(8).size());
        for (MultiWeightedPath p : a.paretoFront().paths()) {
            double[] expected = {a.initialWeight(0), a.initialWeight(1)};
            int state = p.start();
            for (int e : p.edges()) {
                assertEquals(state, a.graph().source(e)); state = a.graph().target(e);
                for (int d = 0; d < expected.length; d++) expected[d] += a.weight(e, d);
            }
            for (int d = 0; d < expected.length; d++) expected[d] += a.finalWeight(state, d);
            assertArrayEquals(expected, p.costs(), 0); assertEquals(20, expected[0] + expected[1], 0);
            assertArrayEquals(new int[]{7}, p.word());
        }
        assertThrows(UnsupportedOperationException.class, () -> a.paretoFront().paths().clear());
        assertTrue(MultiWeightedPaths.dominates(new double[]{1, 2}, new double[]{1, 3}));
        assertFalse(MultiWeightedPaths.dominates(new double[]{1, 2}, new double[]{1, 2}));
        assertFalse(MultiWeightedPaths.dominates(new double[]{1, 4}, new double[]{2, 3}));
        assertThrows(IllegalArgumentException.class, () -> a.scalarize(1));
        assertThrows(IllegalArgumentException.class, () -> a.lexicographicBestPath(0, 0));
    }

    private record Edge(int from, int to, int symbol, double[] vector) { }
    private static void enumerate(int s, int finalState, List<Edge> edges, double[] prefix, List<double[]> result) {
        if (s == finalState) result.add(prefix.clone());
        for (Edge e : edges) if (e.from == s) {
            double[] v = prefix.clone(); for (int d = 0; d < v.length; d++) v[d] += e.vector[d];
            enumerate(e.to, finalState, edges, v, result);
        }
    }
    private static Set<String> independentFront(List<double[]> all) {
        Set<String> result = new HashSet<>();
        for (double[] x : all) {
            boolean dominated = false;
            for (double[] y : all) {
                boolean weak = true, strict = false;
                for (int d = 0; d < x.length; d++) { weak &= y[d] <= x[d]; strict |= y[d] < x[d]; }
                if (weak && strict) { dominated = true; break; }
            }
            if (!dominated) result.add(Arrays.toString(x));
        }
        return result;
    }
    @Test public void paretoAgainstIndependentExhaustiveEnumeration() {
        Random random = new Random(902);
        for (int round = 0; round < 180; round++) {
            int n = 2 + random.nextInt(6), dims = 2 + random.nextInt(2);
            MultiWeightedAutomaton.Builder b = MultiWeightedAutomaton.builder(dims);
            for (int s = 0; s < n; s++) b.addState(); b.setInitial(0); b.setAccept(n - 1);
            List<Edge> edges = new ArrayList<>();
            for (int k = 0; k < 16; k++) {
                int s = random.nextInt(n - 1), t = s + 1 + random.nextInt(n - s - 1); double[] w = new double[dims];
                for (int d = 0; d < dims; d++) w[d] = random.nextInt(9) - 4;
                b.addTransition(s, t, 1, w); edges.add(new Edge(s, t, 1, w));
            }
            MultiWeightedAutomaton a = b.build(); List<double[]> all = new ArrayList<>(); enumerate(0, n - 1, edges, new double[dims], all);
            assertEquals(independentFront(all), vectors(a.paretoFront().paths()));
            if (!all.isEmpty()) {
                all.sort(Arrays::compare); assertArrayEquals(all.get(0), a.lexicographicBestPath().costs(), 0);
                double[] coefficients = new double[dims]; Arrays.fill(coefficients, 1); coefficients[0] = -2;
                double min = Double.POSITIVE_INFINITY;
                for (double[] v : all) { double sum = 0; for (int d = 0; d < dims; d++) sum += coefficients[d] * v[d]; min = Math.min(min, sum); }
                assertEquals(min, a.scalarize(coefficients).optimalCost(), 0);
            } else assertNull(a.lexicographicBestPath());
        }
    }

    private static List<double[]> allWordVectors(MultiWeightedAutomaton a, int[] word) {
        List<double[]> result = new ArrayList<>();
        if (a.graph().getInitialState() >= 0) {
            double[] initial = new double[a.dimensions()]; for (int d = 0; d < initial.length; d++) initial[d] = a.initialWeight(d);
            allWordVectors(a, a.graph().getInitialState(), word, 0, initial, result);
        }
        return result;
    }
    private static void allWordVectors(MultiWeightedAutomaton a, int state, int[] word, int pos, double[] costs, List<double[]> result) {
        if (pos == word.length && a.graph().isAccept(state)) {
            double[] v = costs.clone(); for (int d = 0; d < v.length; d++) v[d] += a.finalWeight(state, d); result.add(v);
        }
        for (int e : a.graph().outgoingEdges(state)) {
            boolean eps = a.graph().isEpsilon(e);
            if (!eps && (pos == word.length || !a.graph().accepts(e, word[pos]))) continue;
            double[] v = costs.clone(); for (int d = 0; d < v.length; d++) v[d] += a.weight(e, d);
            allWordVectors(a, a.graph().target(e), word, pos + (eps ? 0 : 1), v, result);
        }
    }
    private static Set<String> vectorSet(List<double[]> list) { Set<String> set = new HashSet<>(); for (double[] v : list) set.add(Arrays.toString(v)); return set; }
    @Test public void operationsPreserveAllVectorsIncludingDominatedAndIndependentResources() {
        MultiWeightedAutomaton a = choice();
        MultiWeightedAutomaton union = a.union(a), concatenated = a.concatenate(a), product = a.intersection(a);
        Set<String> original = vectorSet(allWordVectors(a, new int[]{7}));
        assertEquals(4, original.size()); assertEquals(original, vectorSet(allWordVectors(union, new int[]{7})));
        Set<String> sums = new HashSet<>();
        for (double[] x : allWordVectors(a, new int[]{7})) for (double[] y : allWordVectors(a, new int[]{7})) sums.add(Arrays.toString(new double[]{x[0] + y[0], x[1] + y[1]}));
        assertEquals(sums, vectorSet(allWordVectors(concatenated, new int[]{7, 7})));
        assertEquals(sums, vectorSet(allWordVectors(product, new int[]{7})));
        assertEquals(original, vectorSet(allWordVectors(a.revert(), new int[]{7})));
        assertEquals(original, vectorSet(allWordVectors(MultiWeightedOperations.trim(a), new int[]{7})));
        assertEquals(original, vectorSet(allWordVectors(a.simplify(), new int[]{7})));
        MultiWeightedAutomaton independent = MultiWeightedOperations.productIndependent(a, a);
        assertEquals(4, independent.dimensions()); assertEquals(16, vectorSet(allWordVectors(independent, new int[]{7})).size());
        MultiWeightedAutomaton independentConcat = MultiWeightedOperations.concatenateIndependent(a, a);
        assertEquals(16, vectorSet(allWordVectors(independentConcat, new int[]{7, 7})).size());
        assertEquals(original, vectorSet(allWordVectors(a, new int[]{7})));
    }
    @Test public void filteredEpsilonProductDoesNotDuplicateOperandPathPairs() {
        MultiWeightedAutomaton.Builder b = MultiWeightedAutomaton.builder(1); for (int i = 0; i < 4; i++) b.addState();
        b.setInitial(0); b.setAccept(3); b.addEpsilonTransition(0, 1, 2); b.addTransition(1, 2, 7, 3); b.addEpsilonTransition(2, 3, 4);
        MultiWeightedAutomaton a = b.build(), p = a.intersection(a);
        List<double[]> vectors = allWordVectors(p, new int[]{7}); assertEquals(1, vectors.size()); assertArrayEquals(new double[]{18}, vectors.get(0), 0);
    }
    @Test public void dimensionsCopiesCyclesAndExtremes() {
        assertThrows(IllegalArgumentException.class, () -> MultiWeightedAutomaton.builder(0));
        MultiWeightedAutomaton.Builder b = MultiWeightedAutomaton.builder(2); b.addState(); b.addState(); b.setInitial(0); b.setAccept(1);
        double[] costs = {1, 2}; b.addTransition(0, 1, new Interval(Integer.MIN_VALUE, Integer.MAX_VALUE), costs); costs[0] = 999;
        MultiWeightedAutomaton a = b.build(); assertEquals(1, a.project(0).cost(Integer.MIN_VALUE), 0);
        a.weights(0)[0] = 900; assertEquals(1, a.weight(0, 0), 0);
        assertThrows(IllegalArgumentException.class, () -> b.addEpsilonTransition(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> a.scalarize(Double.NaN, 1));
        MultiWeightedAutomaton.Builder loop = MultiWeightedAutomaton.builder(1); loop.addState(); loop.setInitial(0); loop.setAccept(0); loop.addTransition(0, 0, 1, -1);
        MultiWeightedAutomaton cyclic = loop.build(); assertThrows(IllegalArgumentException.class, cyclic::paretoFront);
        assertEquals(1, cyclic.paretoForWord(1, 1).size()); assertEquals(-2, cyclic.paretoForWord(1, 1).paths().get(0).cost(0), 0);
    }
    @Test public void exactFallbackPreventsFalseDominanceAndLexicographicTies() {
        MultiWeightedAutomaton.Builder b = MultiWeightedAutomaton.builder(2); for (int i = 0; i < 4; i++) b.addState();
        b.setInitial(0); b.setAccept(3);
        b.addTransition(0, 1, 7, 1e16, 0); b.addTransition(1, 2, 7, 1, 1); b.addTransition(2, 3, 7, -1e16, 0);
        b.addTransition(0, 3, 7, 0, 2); MultiWeightedAutomaton a = b.build();
        assertEquals(Set.of("[1.0, 1.0]", "[0.0, 2.0]"), vectors(a.paretoFront().paths()));
        assertArrayEquals(new double[]{0, 2}, a.lexicographicBestPath().costs(), 0);
        assertArrayEquals(new double[]{1, 1}, a.pathCosts(a.project(0).bestPath(7, 7, 7)), 0);
        for (MultiWeightedPath path : a.paretoFront().paths()) assertEquals(0, path.exactCost(0).compareTo(new java.math.BigDecimal(path.cost(0))));
    }
}
