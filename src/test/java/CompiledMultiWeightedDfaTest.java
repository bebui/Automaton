import fr.menana.automaton.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CompiledMultiWeightedDfaTest {
    @Test public void compiledVectorsAgainstIndependentTransitionTables() {
        Random random = new Random(99200); int[] low = {Integer.MIN_VALUE, -1, 0, 4}, high = {-2, -1, 3, Integer.MAX_VALUE};
        int[] alphabet = {Integer.MIN_VALUE, -2, -1, 0, 3, 4, Integer.MAX_VALUE};
        for (int round = 0; round < 65; round++) {
            int n = 6, dimensions = 3; int[][] target = new int[n][4]; double[][][] weights = new double[n][4][dimensions];
            boolean[] accepting = new boolean[n]; double[][] finals = new double[n][dimensions]; double[] initial = {0.25,-0.5,1.75};
            MultiWeightedAutomaton.Builder b = MultiWeightedAutomaton.builder(dimensions);
            for (int s = 0; s < n; s++) b.addState(); b.setInitial(0, initial);
            for (int s = 0; s < n; s++) {
                accepting[s] = random.nextBoolean();
                for (int d = 0; d < dimensions; d++) finals[s][d] = (random.nextInt(17)-8)/4.0;
                if (accepting[s]) b.setAccept(s, finals[s]);
                for (int c = 3; c >= 0; c--) {
                    target[s][c] = random.nextInt(5) == 0 ? -1 : random.nextInt(n);
                    for (int d = 0; d < dimensions; d++) weights[s][c][d] = (random.nextInt(17)-8)/4.0;
                    if (target[s][c] >= 0) b.addTransition(s, target[s][c], new Interval(low[c], high[c]), weights[s][c]);
                }
            }
            MultiWeightedAutomaton a = b.build(); CompiledMultiWeightedDfa compiled = a.compile(); double[] buffer = new double[dimensions];
            assertSame(a.graph(), compiled.graph()); assertEquals(dimensions, compiled.dimensions());
            for (int query = 0; query < 100; query++) {
                int[] word = new int[random.nextInt(10)]; int state = 0; double[] expected = initial.clone();
                for (int i = 0; i < word.length; i++) {
                    word[i] = alphabet[random.nextInt(alphabet.length)];
                    if (state >= 0) {
                        int c = word[i] <= -2 ? 0 : word[i] == -1 ? 1 : word[i] <= 3 ? 2 : 3;
                        if (target[state][c] >= 0) {
                            for (int d = 0; d < dimensions; d++) expected[d] += weights[state][c][d];
                            state = target[state][c];
                        } else state = -1;
                    }
                }
                boolean accepted = state >= 0 && accepting[state];
                if (accepted) for (int d = 0; d < dimensions; d++) expected[d] += finals[state][d];
                assertEquals(accepted, compiled.costs(word, buffer));
                if (accepted) {
                    assertArrayEquals(expected, buffer, 0); assertArrayEquals(expected, compiled.costs(word), 0);
                    for (int d = 0; d < dimensions; d++) assertEquals(expected[d], compiled.project(d).cost(word), 0);
                } else {
                    assertNull(compiled.costs(word));
                    assertArrayEquals(new double[]{Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY}, buffer, 0);
                }
            }
        }
    }
    @Test public void sharedWeightingsAreLightweightCorrelatedViewsWithCopiedOperandArray() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); b.addState(); b.addState(); b.setInitial(0, 2); b.setAccept(1, 3);
        b.addTransition(0, 1, new Interval(-5, 7), 1); WeightedAutomaton a = b.build();
        WeightedAutomaton other = WeightedAutomaton.of(a.graph(), new double[]{9}, -2, new double[]{0,4});
        WeightedAutomaton[] inputs = {a,other}; MultiWeightedAutomaton multi = MultiWeightedAutomaton.fromWeightings(inputs); inputs[0] = other;
        assertSame(a.graph(), multi.graph()); assertEquals(1, multi.weight(0,0), 0); assertEquals(9, multi.weight(0,1), 0);
        assertArrayEquals(new double[]{6,11}, multi.compile().costs(0), 0);
        assertEquals(17, multi.compile().scalarize(1,1).cost(0), 0);
        assertThrows(IllegalArgumentException.class, () -> MultiWeightedAutomaton.fromWeightings(a,b.build()));
        assertThrows(IllegalArgumentException.class, () -> MultiWeightedAutomaton.fromWeightings());
        assertThrows(NullPointerException.class, () -> MultiWeightedAutomaton.fromWeightings(a,null));
    }
    private static void scalarPaths(WeightedAutomaton a, int state, int[] word, int at, double cost, List<Double> result) {
        if (at == word.length && a.graph().isAccept(state)) result.add(cost + a.finalWeight(state));
        for (int e : a.graph().outgoingEdges(state)) {
            boolean eps = a.graph().isEpsilon(e);
            if (eps || (at < word.length && a.graph().accepts(e, word[at])))
                scalarPaths(a, a.graph().target(e), word, at + (eps ? 0 : 1), cost + a.weight(e), result);
        }
    }
    private static void vectorPaths(MultiWeightedAutomaton a, int state, int[] word, int at, double[] cost, List<String> result) {
        if (at == word.length && a.graph().isAccept(state)) {
            double[] v = cost.clone(); for (int d = 0; d < v.length; d++) v[d] += a.finalWeight(state,d); result.add(Arrays.toString(v));
        }
        for (int e : a.graph().outgoingEdges(state)) {
            boolean eps = a.graph().isEpsilon(e);
            if (eps || (at < word.length && a.graph().accepts(e,word[at]))) {
                double[] v = cost.clone(); for (int d = 0; d < v.length; d++) v[d] += a.weight(e,d);
                vectorPaths(a, a.graph().target(e), word, at + (eps ? 0 : 1), v, result);
            }
        }
    }
    private static List<String> vectors(MultiWeightedAutomaton a, int... word) {
        List<String> result = new ArrayList<>();
        if (a.graph().getInitialState() >= 0) {
            double[] initial = new double[a.dimensions()]; for (int d = 0; d < initial.length; d++) initial[d] = a.initialWeight(d);
            vectorPaths(a, a.graph().getInitialState(), word, 0, initial, result);
        }
        Collections.sort(result); return result;
    }
    @Test public void independentProductDiffersFromCorrelatedSharedNfaPaths() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); b.addState(); b.addState(); b.setInitial(0); b.setAccept(1);
        b.addTransition(0,1,1,0); b.addTransition(0,1,1,10); WeightedAutomaton a = b.build(), c = a.withWeights(new double[]{10,0});
        MultiWeightedAutomaton correlated = MultiWeightedAutomaton.fromWeightings(a,c), independent = MultiWeightedAutomaton.synchronizedProduct(a,c);
        assertEquals(List.of("[0.0, 10.0]", "[10.0, 0.0]"), vectors(correlated,1));
        assertEquals(List.of("[0.0, 0.0]", "[0.0, 10.0]", "[10.0, 0.0]", "[10.0, 10.0]"), vectors(independent,1));
        assertEquals(10, correlated.scalarize(1,1).cost(1), 0); assertEquals(0, independent.scalarize(1,1).cost(1), 0);
        assertThrows(IllegalArgumentException.class, correlated::compile);
    }
    @Test public void independentProductsPreserveFullCartesianPathRelationIncludingEpsilon() {
        Random random = new Random(99201);
        for (int round = 0; round < 25; round++) {
            WeightedAutomaton[] operands = new WeightedAutomaton[3];
            for (int k = 0; k < 3; k++) {
                WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int s = 0; s < 4; s++) b.addState();
                b.setInitial(0,k/4.0); b.setAccept(3, -k/4.0);
                for (int s = 0; s < 3; s++) {
                    b.addEpsilonTransition(s, s+1, random.nextInt(5)/4.0);
                    for (int j = 0; j < 2; j++) b.addTransition(s,s+1,new Interval(0,1), (random.nextInt(9)-4)/4.0);
                }
                operands[k] = b.build();
            }
            MultiWeightedAutomaton product = MultiWeightedOperations.synchronizedProduct(operands);
            for (int[] word : new int[][]{new int[0], {0}, {1}, {0,1}, {1,0}, {0,1,0}}) {
                List<List<Double>> costs = new ArrayList<>();
                for (WeightedAutomaton a : operands) { List<Double> paths = new ArrayList<>(); scalarPaths(a,0,word,0,a.initialWeight(),paths); costs.add(paths); }
                List<String> expected = new ArrayList<>();
                for (double a : costs.get(0)) for (double b : costs.get(1)) for (double c : costs.get(2)) expected.add(Arrays.toString(new double[]{a,b,c}));
                Collections.sort(expected); assertEquals(expected,vectors(product,word));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> MultiWeightedOperations.synchronizedProduct());
        assertThrows(NullPointerException.class, () -> MultiWeightedOperations.synchronizedProduct((WeightedAutomaton)null));
    }
    @Test public void compiledReweightSlicesProjectionAndScalarization() {
        MultiWeightedAutomaton.Builder b = MultiWeightedAutomaton.builder(2); b.addState(); b.addState(); b.setInitial(0,1,2); b.setAccept(1,3,4);
        b.addTransition(0,1,new Interval(Integer.MIN_VALUE,Integer.MAX_VALUE),0.5,-0.25);
        MultiWeightedAutomaton a = b.build(); CompiledMultiWeightedDfa compiled = a.compile();
        double[] out = new double[2]; int[] text = {99,Integer.MAX_VALUE,99};
        assertTrue(compiled.costs(text,1,2,out)); assertArrayEquals(new double[]{4.5,5.75},out,0);
        assertArrayEquals(out, compiled.costs(text,1,2),0);
        assertEquals(4.5-2*5.75,compiled.scalarize(1,-2).cost(Integer.MAX_VALUE),0);
        MultiWeightedAutomaton changed = MultiWeightedAutomaton.of(a.graph(),3,new double[]{7,8,9});
        CompiledMultiWeightedDfa reweighted = compiled.reweight(changed);
        assertEquals(3,reweighted.dimensions()); assertArrayEquals(new double[]{7,8,9},reweighted.costs(1),0);
        assertArrayEquals(new double[]{4.5,5.75},compiled.costs(1),0);
        assertThrows(IllegalArgumentException.class, () -> compiled.reweight(b.build()));
        assertThrows(IndexOutOfBoundsException.class, () -> compiled.costs(text,2,1,out));
        assertThrows(IllegalArgumentException.class, () -> compiled.costs(text,new double[1]));
        assertThrows(IndexOutOfBoundsException.class, () -> compiled.project(2));
        assertThrows(IllegalArgumentException.class, () -> compiled.scalarize(1));
        assertEquals(-1,compiled.transition(-1,0)); assertEquals(0,compiled.transition(0,Integer.MIN_VALUE));
        assertNull(compiled.costs()); assertNull(compiled.costs(1,2));
    }
    @Test public void epsilonRejectionEmptyGraphsAndCheckedOverflow() {
        MultiWeightedAutomaton.Builder b = MultiWeightedAutomaton.builder(1); b.addState(); b.setInitial(0); b.setAccept(0); b.addEpsilonTransition(0,0,0);
        MultiWeightedAutomaton epsilon = b.build(); assertThrows(IllegalArgumentException.class, epsilon::compile);
        MultiWeightedAutomaton empty = MultiWeightedAutomaton.builder(2).build(); double[] out = {1,2};
        assertFalse(empty.compile().costs(new int[0],out)); assertNull(empty.compile().costs());
        assertArrayEquals(new double[]{Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},out,0);
        b = MultiWeightedAutomaton.builder(1); b.addState(); b.setInitial(0,Double.MAX_VALUE); b.setAccept(0); b.addTransition(0,0,1,Double.MAX_VALUE);
        CompiledMultiWeightedDfa overflow = b.build().compile(); assertThrows(ArithmeticException.class, () -> overflow.costs(1));
    }
}
