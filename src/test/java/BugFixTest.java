import fr.menana.automaton.Automaton;
import fr.menana.automaton.Interval;
import fr.menana.automaton.IntervalSet;
import fr.menana.automaton.Operation;
import fr.menana.automaton.State;
import fr.menana.automaton.regexp.RegExpParser;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Regression tests, one per fixed bug
 */
public class BugFixTest {

    @Test
    public void complementOfNonDeterministicAutomaton() {
        Automaton nfa = Automaton.nfaFromString("1|12");
        assertFalse(nfa.isDeterministic());
        Automaton comp = nfa.complement();
        assertFalse(comp.run(1));
        assertFalse(comp.run(1, 2));
        assertTrue(comp.run());
        assertTrue(comp.run(2));
        assertTrue(comp.run(1, 2, 3));
    }

    @Test
    public void complementOfEmptyAutomatonAcceptsEverything() {
        Automaton comp = new Automaton().complement();
        assertTrue(comp.run());
        assertTrue(comp.run(Integer.MIN_VALUE, 0, Integer.MAX_VALUE));
    }

    @Test
    public void unboundedRepetition() {
        Automaton atLeastTwo = Automaton.dfaFromString("1{2,}");
        assertFalse(atLeastTwo.run(1));
        assertTrue(atLeastTwo.run(1, 1));
        assertTrue(atLeastTwo.run(1, 1, 1, 1, 1, 1));
        assertFalse(atLeastTwo.run(1, 1, 2));

        Automaton atMostTwo = Automaton.dfaFromString("1{,2}");
        assertTrue(atMostTwo.run());
        assertTrue(atMostTwo.run(1, 1));
        assertFalse(atMostTwo.run(1, 1, 1));
    }

    @Test
    public void invalidRepetitionsAreRejected() {
        for (String regexp : new String[]{"1{3,1}", "1{-1}", "1{a}", "1{,}", "1{1,2,3}", "1{2"})
            assertInvalid(regexp);
    }

    @Test
    public void singleDigitBetweenAngleBrackets() {
        Automaton auto = Automaton.dfaFromString("<5><12><-3>");
        assertTrue(auto.run(5, 12, -3));
        assertFalse(auto.run(5, 1, 2, -3));
    }

    @Test
    public void malformedExpressionsGiveClearErrors() {
        for (String regexp : new String[]{"(1", "1)", "1)2", "<99999999999>", "a", "<12", "<>", "<1a>", "*1", "1|("})
            assertInvalid(regexp);
    }

    private static void assertInvalid(String regexp) {
        try {
            Automaton.dfaFromString(regexp);
            fail("Should reject " + regexp);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("position"));
        }
    }

    @Test
    public void hashCodeIsConsistentWithEquals() {
        Automaton a = Automaton.dfaFromString("12|34");
        Automaton b = Automaton.dfaFromString("12|34");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void equalsComparesAcceptingStates() {
        // Same structure : a single state without transition, accepting or not
        Automaton epsilon = Automaton.dfaFromString("");
        Automaton empty = new Automaton();
        empty.setInitial(empty.addState());
        assertTrue(epsilon.run());
        assertFalse(empty.run());
        assertNotEquals(epsilon, empty);
    }

    @Test
    public void sizeDoesNotOverflow() {
        assertEquals(1L << 32, IntervalSet.ALL.size());
        assertEquals(0L, IntervalSet.EMPTY.size());
    }

    @Test
    public void emptyAutomataDoNotCrash() {
        Automaton empty = new Automaton();
        assertEquals(0, empty.revert().getNbStates());
        assertEquals(0, empty.determinize().getNbStates());
        assertFalse(Automaton.dfaFromString("1").concatenate(empty).run(1));
        assertFalse(empty.concatenate(Automaton.dfaFromString("1")).run(1));
        assertFalse(empty.minimize().run());
    }

    @Test
    public void revertKeepsEpsilonTransitions() {
        Automaton nfa = Automaton.nfaFromString("12*3");
        Automaton reverted = nfa.revert();
        assertTrue(reverted.run(3, 2, 2, 1));
        assertTrue(reverted.run(3, 1));
        assertFalse(reverted.run(1, 2, 3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyIntervalIsRejected() {
        new Interval(5, 1);
    }

    @Test
    public void regexpToStringCanBeParsedAgain() {
        for (String regexp : new String[]{"1+", "(12)+3", "(12){2,3}", "1(2|3)*", "<12>{2,}", "(1|2){,2}", ".?4"}) {
            String printed = RegExpParser.toRegExp(regexp).toString();
            assertEquals(regexp + " printed as " + printed,
                    Automaton.dfaFromString(regexp), Automaton.dfaFromString(printed));
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void sharedConstantsCannotBeModified() {
        IntervalSet.ALL.remove(IntervalSet.fromIntArray(1));
    }

    @Test
    public void clonesOfConstantsCanBeModified() {
        IntervalSet set = IntervalSet.EMPTY.clone();
        set.add(3);
        assertTrue(set.contains(3));
        assertTrue(IntervalSet.EMPTY.isEmpty());
    }

    @Test
    public void automatonDoesNotShareTheGivenValues() {
        IntervalSet values = IntervalSet.fromIntArray(1, 2);
        Automaton auto = new Automaton();
        State s0 = auto.addState();
        State s1 = auto.addState();
        auto.setInitial(s0);
        auto.setAccept(s1);
        auto.addTransition(s0, s1, values);
        values.add(7);
        assertFalse(auto.run(7));
        auto.getInitial().getTransitions().get(s1).getValues().add(8);
        assertFalse(auto.run(8));
    }

    @Test
    public void defaultMinimizationCanBeChanged() {
        Operation.MINIMIZATION_ALGO previous = Operation.getDefaultMinimization();
        try {
            Operation.setDefaultMinimization(Operation.MINIMIZATION_ALGO.Brzozowski);
            assertEquals(Operation.MINIMIZATION_ALGO.Brzozowski, Operation.getDefaultMinimization());
            assertTrue(Automaton.dfaFromString("(1|2)*3").run(1, 2, 3));
        } finally {
            Operation.setDefaultMinimization(previous);
        }
    }
}
