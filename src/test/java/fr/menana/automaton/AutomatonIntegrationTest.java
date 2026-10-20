package fr.menana.automaton;

import fr.menana.automaton.weighted.*;
import java.util.*;
import java.util.function.ToDoubleFunction;
import org.junit.Test;
import static org.junit.Assert.*;

public class AutomatonIntegrationTest {
    @Test public void existingConstructionFlowsDirectlyIntoScalarAndVectorCosts() {
        Automaton source = Automaton.dfaFromString("12+");
        WeightedAutomaton scalar = source.weighted(t -> 2);
        MultiWeightedAutomaton vector = source.multiWeighted(t -> 2, t -> 5);
        assertEquals(6, scalar.cost(1, 2, 2), 0);
        assertEquals(15, vector.project(1).cost(1, 2, 2), 0);
        assertTrue(scalar.run(1, 2)); assertFalse(vector.run(1));
        assertTrue(source.sameLanguage(scalar.toAutomaton()));
        assertTrue(source.sameLanguage(vector.toAutomaton()));
        assertEquals(6, WeightedAutomaton.from(source, t -> 2).cost(1, 2, 2), 0);
        assertEquals(15, MultiWeightedAutomaton.from(source, t -> 2, t -> 5).project(1).cost(1, 2, 2), 0);
    }

    @Test public void oneSnapshotRetainsMappingsBoundariesAndSharesItsGraphAfterSourceEdits() {
        Automaton source = new Automaton();
        State end = source.addState(), start = source.addState(), middle = source.addState();
        source.setInitial(start); source.setAccept(end);
        source.addTransition(start, middle, 1); source.addTransition(middle, end, 2);
        AutomatonSnapshot snapshot = source.snapshot();
        assertEquals(1, snapshot.stateId(start)); assertSame(end, snapshot.originalState(0));
        assertSame(start.getTransitions().get(middle), snapshot.originalTransition(0));
        WeightedAutomaton cost = snapshot.weighted(t -> 2, 0.5, s -> {
            assertSame(end, s); return 1.5;
        });
        WeightedAutomaton duration = snapshot.weighted(t -> 7);
        MultiWeightedAutomaton vectors = snapshot.multiWeighted(t -> 2, t -> 7);
        assertSame(snapshot.graph(), cost.graph()); assertSame(cost.graph(), duration.graph());
        assertSame(cost.graph(), vectors.graph());
        assertSame(cost.graph(), MultiWeightedAutomaton.fromWeightings(cost, duration).graph());
        assertEquals(6, cost.cost(1, 2), 0); assertEquals(14, duration.cost(1, 2), 0);
        source.reIndex(); assertEquals(0, start.getIndex()); assertEquals(1, snapshot.stateId(start));
        source.setAccept(end, false); source.addTransition(start, end, 9);
        assertTrue(snapshot.run(1, 2)); assertFalse(snapshot.run(9)); assertFalse(source.run(1, 2));
        assertEquals(6, cost.cost(1, 2), 0);
        Automaton exported = snapshot.toAutomaton(); exported.setAccept(exported.getInitial());
        assertTrue(exported.run()); assertFalse(snapshot.run());
        assertThrows(IllegalArgumentException.class, () -> snapshot.stateId(new Automaton().addState()));
        assertThrows(IndexOutOfBoundsException.class, () -> snapshot.originalState(3));
    }

    @Test public void mixedEpsilonAndSymbolModesCanReceiveDifferentCosts() {
        Automaton source = new Automaton(); State start = source.addState(), end = source.addState();
        source.setInitial(start); source.setAccept(end);
        source.addEpsilonTransition(start, end); source.addTransition(start, end, new Interval(4, 8));
        AutomatonSnapshot snapshot = source.snapshot();
        assertEquals(2, snapshot.graph().getNbTransitions());
        assertSame(snapshot.originalTransition(0), snapshot.originalTransition(1));
        WeightedAutomaton weighted = snapshot.weighted((transition, epsilon) -> epsilon ? -3 : 7,
                                                       2, state -> 0.5);
        assertEquals(-0.5, weighted.cost(), 0); assertEquals(9.5, weighted.cost(5), 0);
        Automaton projected = weighted.toAutomaton();
        assertTrue(projected.run()); assertTrue(projected.run(5)); assertFalse(projected.run(5, 5));
        assertEquals(1, projected.getAllTransitions().size());
        assertTrue(projected.getAllTransitions().get(0).hasEpsilon());
        assertTrue(projected.getAllTransitions().get(0).hasValues());
    }

    @Test public void vectorCallbacksRemainCorrelatedAlongEachNondeterministicPath() {
        Automaton source = new Automaton();
        State s = source.addState(), left = source.addState(), right = source.addState(), end = source.addState();
        source.setInitial(s); source.setAccept(end);
        source.addTransition(s, left, 7); source.addTransition(s, right, 7);
        source.addTransition(left, end, 8); source.addTransition(right, end, 8);
        MultiWeightedAutomaton multi = source.multiWeighted(
                t -> t.getOrig() == s ? (t.getDest() == left ? 1 : 9) : 0,
                t -> t.getOrig() == s ? (t.getDest() == left ? 9 : 1) : 0);
        assertEquals(10, multi.scalarize(1, 1).cost(7, 8), 0);
        assertEquals(2, multi.paretoForWord(7, 8).size());
        assertTrue(multi.run(7, 8));
    }

    @Test public void booleanMembershipIsIndependentOfOverflowNegativeCyclesAndParallelCosts() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); b.addState();
        b.setInitial(0); b.setAccept(0); b.addTransition(0, 0, IntervalSet.ALL.clone(), Double.MAX_VALUE);
        WeightedAutomaton huge = b.build();
        assertThrows(ArithmeticException.class, () -> huge.cost(1, 2));
        assertTrue(huge.run(Integer.MIN_VALUE, Integer.MAX_VALUE));
        b.addEpsilonTransition(0, 0, -1); WeightedAutomaton unbounded = b.build();
        assertEquals(Double.NEGATIVE_INFINITY, unbounded.cost(1), 0); assertTrue(unbounded.run(1));
        assertTrue(unbounded.toAutomaton().run(1));
        MultiWeightedAutomaton multi = MultiWeightedAutomaton.fromWeightings(unbounded, unbounded);
        assertTrue(multi.run(1, 2, 3));
        WeightedAutomaton.Builder parallel = WeightedAutomaton.builder(); parallel.addState(); parallel.addState();
        parallel.setInitial(0); parallel.setAccept(1);
        parallel.addTransition(0, 1, 7, 2); parallel.addTransition(0, 1, 7, 9);
        WeightedAutomaton a = parallel.build(); Automaton booleanCopy = a.toAutomaton();
        assertEquals(1, booleanCopy.getAllTransitions().size()); assertEquals(2, a.graph().getNbTransitions());
        assertEquals(2, a.cost(7), 0); booleanCopy.getStates().clear(); assertTrue(a.run(7));
    }

    private record Arc(int from, int to, int low, int high, boolean epsilon) { }
    private static boolean accepts(int n, int initial, boolean[] finals, List<Arc> arcs, int[] word) {
        if (initial < 0) return false;
        boolean[][] closure = new boolean[n][n];
        for (int s = 0; s < n; s++) closure[s][s] = true;
        for (Arc edge : arcs) if (edge.epsilon) closure[edge.from][edge.to] = true;
        close(closure);
        boolean[] active = closure[initial].clone();
        for (int symbol : word) {
            boolean[] next = new boolean[n];
            for (Arc edge : arcs) if (!edge.epsilon && active[edge.from] && symbol >= edge.low && symbol <= edge.high)
                for (int t = 0; t < n; t++) next[t] |= closure[edge.to][t];
            active = next;
        }
        for (int s = 0; s < n; s++) if (active[s] && finals[s]) return true;
        return false;
    }
    private static void close(boolean[][] relation) {
        for (int k = 0; k < relation.length; k++)
            for (int i = 0; i < relation.length; i++)
                for (int j = 0; j < relation.length; j++) relation[i][j] |= relation[i][k] && relation[k][j];
    }
    private static List<int[]> words() {
        List<int[]> result = new ArrayList<>(); result.add(new int[0]); int start = 0;
        for (int length = 1; length <= 3; length++) {
            int end = result.size();
            for (int i = start; i < end; i++) for (int symbol : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
                int[] word = Arrays.copyOf(result.get(i), length); word[length - 1] = symbol; result.add(word);
            }
            start = end;
        }
        return result;
    }

    @Test public void generatedGraphsMatchIndependentEpsilonAndReachabilityMatrixOracles() {
        Random random = new Random(817593); List<int[]> words = words();
        for (int round = 0; round < 70; round++) {
            int n = 1 + random.nextInt(7), initial = random.nextInt(5) == 0 ? -1 : random.nextInt(n);
            boolean[] finals = new boolean[n]; AutomatonGraph.Builder builder = AutomatonGraph.builder();
            for (int s = 0; s < n; s++) { builder.addState(); if (finals[s] = random.nextBoolean()) builder.setAccept(s); }
            if (initial >= 0) builder.setInitial(initial);
            List<Arc> arcs = new ArrayList<>();
            for (int e = 0; e < 18; e++) {
                int from = random.nextInt(n), to = random.nextInt(n), low = random.nextInt(5) - 2;
                int high = low + random.nextInt(3); boolean epsilon = random.nextInt(4) == 0;
                arcs.add(new Arc(from, to, low, high, epsilon));
                if (epsilon) builder.addEpsilonTransition(from, to); else builder.addTransition(from, to, new Interval(low, high));
            }
            AutomatonGraph graph = builder.build(); Automaton projected = graph.toAutomaton();
            AutomatonSnapshot snapshot = projected.snapshot();
            WeightedAutomaton scalar = WeightedAutomaton.of(graph, new double[arcs.size()]);
            MultiWeightedAutomaton multi = MultiWeightedAutomaton.fromWeightings(scalar);
            for (int[] word : words) {
                boolean expected = accepts(n, initial, finals, arcs, word);
                assertEquals(expected, graph.run(word)); assertEquals(expected, projected.run(word));
                assertEquals(expected, snapshot.run(word)); assertEquals(expected, scalar.run(word));
                assertEquals(expected, multi.run(word));
            }
            boolean[][] reach = new boolean[n][n]; for (int s = 0; s < n; s++) reach[s][s] = true;
            for (Arc arc : arcs) reach[arc.from][arc.to] = true; close(reach);
            boolean[] coaccessible = graph.coaccessibleStates(), useful = graph.usefulStates();
            Set<State> legacy = projected.getUseFulStates();
            for (int s = 0; s < n; s++) {
                boolean expected = false; for (int t = 0; t < n; t++) expected |= finals[t] && reach[s][t];
                assertEquals(expected, coaccessible[s]); assertEquals(expected, legacy.contains(projected.getState(s)));
                assertEquals(initial >= 0 && reach[initial][s] && expected, useful[s]);
            }
            Collections.shuffle(projected.getStates(), random);
            assertEquals(legacy, projected.getUseFulStates());
        }
    }

    @Test public void emptyAndInvalidInputsDoNotInventPathsOrWeights() {
        Automaton empty = new Automaton(); AutomatonSnapshot snapshot = empty.snapshot();
        assertFalse(snapshot.run()); assertFalse(empty.weighted(t -> 0).run());
        assertFalse(empty.multiWeighted(t -> 0).run()); assertTrue(snapshot.toAutomaton().isEmpty());
        assertArrayEquals(new boolean[0], snapshot.graph().coaccessibleStates());
        assertThrows(NullPointerException.class, () -> empty.weighted(null));
        assertThrows(IllegalArgumentException.class, () -> snapshot.multiWeighted());
        assertThrows(NullPointerException.class, () -> snapshot.multiWeighted((ToDoubleFunction<Transition>) null));
        assertThrows(IllegalArgumentException.class, () -> snapshot.weighted(t -> 0, Double.NaN, s -> 0));
        Automaton source = Automaton.dfaFromString("1");
        assertThrows(IllegalArgumentException.class, () -> source.weighted(t -> Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> source.multiWeighted(t -> 0, t -> Double.NaN));
    }

    @Test public void legacyEmptyLabelRecordsKeepTheirHistoricalStructuralQueryBehavior() {
        Automaton source = new Automaton(); State start = source.addState(), end = source.addState();
        source.setInitial(start); source.setAccept(end); source.addTransition(start, end, new IntervalSet());
        assertFalse(source.run()); assertFalse(source.run(0));
        assertEquals(Set.of(start, end), source.getUseFulStates());
        AutomatonSnapshot snapshot = source.snapshot();
        assertEquals(0, snapshot.graph().getNbTransitions());
        assertArrayEquals(new boolean[]{false, true}, snapshot.graph().coaccessibleStates());
        assertFalse(snapshot.weighted(t -> { throw new AssertionError("Empty label is not a logical edge"); }).run(0));
        Collections.swap(source.getStates(), 0, 1);
        assertEquals(Set.of(start, end), source.getUseFulStates());
    }
}
