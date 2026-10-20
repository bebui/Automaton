package fr.menana.automaton.weighted;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.compiled.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class LayeredAutomatonTest {
    private static double bounded(WeightedAutomaton a, int s, int left, double prefix) {
        double best = left == 0 && a.graph().isAccept(s) ? prefix + a.finalWeight(s) : Double.POSITIVE_INFINITY;
        if (left == 0) return best;
        for (int e : a.graph().outgoingEdges(s)) best = Math.min(best, bounded(a, a.graph().target(e), left - 1, prefix + a.weight(e)));
        return best;
    }
    @Test public void fixedLengthAgainstIndependentWalkEnumeration() {
        Random random = new Random(804);
        for (int round = 0; round < 100; round++) {
            WeightedAutomaton.Builder b = WeightedAutomaton.builder(); int n = 2 + random.nextInt(5);
            for (int s = 0; s < n; s++) { b.addState(); if (random.nextBoolean()) b.setAccept(s, random.nextInt(5) - 2); }
            b.setInitial(0, 2);
            for (int k = 0; k < 2 * n; k++) b.addTransition(random.nextInt(n), random.nextInt(n), new Interval(-2, 3), random.nextInt(9) - 4);
            WeightedAutomaton a = b.build(); double atMost = Double.POSITIVE_INFINITY;
            for (int length = 0; length < 5; length++) {
                double expected = bounded(a, 0, length, 2); atMost = Math.min(atMost, expected);
                assertEquals(expected, a.costOfLength(length), 0); assertEquals(atMost, a.costOfLengthAtMost(length), 0);
                LayeredAutomaton layers = LayeredAutomaton.exactly(a.graph(), length);
                assertEquals(expected, layers.evaluate(a).optimalCost(), 0);
                WeightedPath p = layers.bestPath(a);
                if (expected == Double.POSITIVE_INFINITY) assertNull(p);
                else { assertEquals(length, p.length()); assertEquals(expected, p.cost(), 0); assertEquals(expected, a.cost(p.word()), 0); }
                assertEquals(atMost, LayeredAutomaton.atMost(a.graph(), length).evaluate(a).optimalCost(), 0);
            }
        }
    }
    @Test public void structureReusedUnderReweightingAndPositionCosts() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int i = 0; i < 3; i++) b.addState();
        b.setInitial(0, 1); b.setAccept(1, 2); b.addTransition(0, 1, 3, 4); b.addTransition(1, 1, 5, 7);
        // State 2 is intentionally useless and must never appear in the deployed graph.
        WeightedAutomaton a = b.build(); LayeredAutomaton layers = LayeredAutomaton.exactly(a.graph(), 3);
        assertEquals(4, layers.graph().getNbStates()); assertEquals(3, layers.graph().getNbTransitions());
        WeightedAutomaton changed = a.withWeights(new double[]{-1, 2});
        assertSame(layers.graph(), layers.evaluate(a).graph()); assertSame(layers.graph(), layers.evaluate(changed).graph());
        assertEquals(21, layers.analyze(a).optimalCost(), 0); assertEquals(6, layers.analyze(changed).optimalCost(), 0);
        LayeredAutomaton.Analysis analysis = layers.analyze(a, (position, edge) -> position + edge);
        assertEquals(8, analysis.optimalCost(), 0); assertEquals(8, analysis.bestPath().cost(), 0);
        assertEquals(8, analysis.costAt(0, 3), 0); assertEquals(Double.POSITIVE_INFINITY, analysis.costAt(0, 5), 0);
        assertEquals(8, analysis.costAt(2, 5), 0);
        assertEquals(4, analysis.forwardDistances().length); assertEquals(4, analysis.backwardDistances().length);
        assertSame(a.graph(), analysis.bestPath().graph());
    }
    @Test public void epsilonDoesNotConsumePositionAndMayRemainCyclic() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); for (int i = 0; i < 3; i++) b.addState();
        b.setInitial(0); b.setAccept(2); b.addEpsilonTransition(0, 1, -2); b.addTransition(1, 2, 9, 5); b.addEpsilonTransition(1, 1, 0);
        WeightedAutomaton a = b.build(); LayeredAutomaton layers = LayeredAutomaton.exactly(a.graph(), 1);
        assertFalse(layers.graph().isAcyclic()); assertEquals(3, a.costOfLength(1), 0); assertEquals(1, layers.bestPath(a).length());
        assertEquals(3, layers.analyze(a).costAt(0, 9), 0); assertEquals(Double.POSITIVE_INFINITY, a.costOfLength(0), 0);
        WeightedAutomaton negative = a.withWeights(new double[]{-2, 5, -1});
        assertEquals(Double.NEGATIVE_INFINITY, negative.costOfLength(1), 0); assertEquals(Double.NEGATIVE_INFINITY, layers.analyze(negative).optimalCost(), 0);
        assertThrows(IllegalStateException.class, () -> layers.bestPath(negative));
        assertThrows(IllegalArgumentException.class, () -> LayeredAutomaton.exactly(a.graph(), -1));
        assertThrows(ArithmeticException.class, () -> LayeredAutomaton.exactly(a.graph(), Integer.MAX_VALUE));
    }
}
