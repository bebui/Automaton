import fr.menana.automaton.Automaton;
import fr.menana.automaton.Interval;
import fr.menana.automaton.IntervalSet;
import fr.menana.automaton.Operation;
import fr.menana.automaton.State;
import fr.menana.automaton.Transition;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Checks determinization and minimization against an independent simulation of the original automaton
 */
public class NfaEquivalenceTest {

    /**
     * Reference implementation : simulates the automaton on the set of reachable states
     */
    private static boolean accepts(Automaton auto, int[] word) {
        if (auto.getInitial() == null)
            return false;
        Set<State> current = closure(Collections.singleton(auto.getInitial()));
        for (int symbol : word) {
            Set<State> next = new HashSet<>();
            for (State s : current)
                for (Transition t : s.getTransitions().values())
                    if (t.accepts(symbol))
                        next.add(t.getDest());
            current = closure(next);
        }
        return current.stream().anyMatch(State::isAccept);
    }

    private static Set<State> closure(Set<State> states) {
        Set<State> out = new HashSet<>(states);
        Deque<State> todo = new ArrayDeque<>(states);
        while (!todo.isEmpty())
            for (Transition t : todo.poll().getTransitions().values())
                if (t.hasEpsilon() && out.add(t.getDest()))
                    todo.add(t.getDest());
        return out;
    }

    private static Automaton randomNfa(Random r) {
        Automaton auto = new Automaton();
        int n = 1 + r.nextInt(25);
        for (int i = 0; i < n; i++)
            auto.addState();
        List<State> states = auto.getStates();
        auto.setInitial(states.get(0));
        for (State s : states)
            auto.setAccept(s, r.nextInt(4) == 0);
        int nbTransitions = r.nextInt(3 * n + 1);
        for (int i = 0; i < nbTransitions; i++) {
            State orig = states.get(r.nextInt(n));
            State dest = states.get(r.nextInt(n));
            int kind = r.nextInt(10);
            if (kind < 2) {
                auto.addEpsilonTransition(orig, dest);
            } else if (kind < 4) {
                int min = r.nextInt(10);
                auto.addTransition(orig, dest, new Interval(min, min + r.nextInt(4)));
            } else {
                auto.addTransition(orig, dest, r.nextInt(10));
            }
        }
        return auto;
    }

    private static int[] randomWord(Random r) {
        int[] word = new int[r.nextInt(12)];
        for (int i = 0; i < word.length; i++)
            word[i] = r.nextInt(12) - 1;
        return word;
    }

    @Test
    public void randomNfaTest() {
        Random r = new Random(42);
        for (int i = 0; i < 2000; i++) {
            Automaton nfa = randomNfa(r);
            Automaton dfa = nfa.determinize();
            Automaton hop = Operation.minimizeHopcroft(nfa);
            Automaton brz = Operation.minimizeBrzozowski(nfa);
            assertTrue(dfa.isDeterministic());
            assertTrue(hop.isDeterministic());
            assertEquals("both minimal automata must have the same size", brz.getNbStates(), hop.getNbStates());
            assertTrue(hop.getNbStates() <= dfa.getNbStates());
            for (int j = 0; j < 200; j++) {
                int[] word = randomWord(r);
                boolean expected = accepts(nfa, word);
                assertEquals(expected, nfa.run(word));
                assertEquals(expected, dfa.run(word));
                assertEquals(expected, hop.run(word));
                assertEquals(expected, brz.run(word));
            }
        }
    }

    @Test
    public void acceptingStateWithOnlyEpsilonTransitions() {
        // 1+ : the accepting state only has an epsilon transition back to the initial state
        Automaton auto = new Automaton();
        State s0 = auto.addState();
        State s1 = auto.addState();
        auto.setInitial(s0);
        auto.setAccept(s1);
        auto.addTransition(s0, s1, 1);
        auto.addEpsilonTransition(s1, s0);

        Automaton dfa = auto.determinize();
        assertFalse(dfa.run());
        assertTrue(dfa.run(1));
        assertTrue(dfa.run(1, 1));
        assertFalse(dfa.run(1, 2));
        assertTrue(auto.minimize().run(1, 1, 1));
    }

    @Test
    public void epsilonCycleDoesNotOverflow() {
        Automaton auto = new Automaton();
        State s0 = auto.addState();
        State s1 = auto.addState();
        State s2 = auto.addState();
        auto.setInitial(s0);
        auto.addEpsilonTransition(s0, s1);
        auto.addEpsilonTransition(s1, s0);
        auto.addTransition(s1, s2, 3);
        auto.setAccept(s2);
        assertTrue(auto.run(3));
        assertFalse(auto.run(3, 3));
    }

    @Test
    public void largeIntervalsAndExtremeValues() {
        Automaton auto = new Automaton();
        State s0 = auto.addState();
        State s1 = auto.addState();
        auto.setInitial(s0);
        auto.setAccept(s1);
        auto.addTransition(s0, s1, new Interval(Integer.MIN_VALUE, -1));
        auto.addTransition(s0, s1, new Interval(1000, Integer.MAX_VALUE));
        auto.addEpsilonTransition(s1, s0);
        Automaton min = auto.minimize();
        assertEquals(2, min.getNbStates());
        assertTrue(min.run(Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 1000));
        assertFalse(min.run(0));
        assertFalse(min.run(999));
        assertFalse(min.run());
    }

    @Test
    public void emptyLanguage() {
        Automaton auto = new Automaton();
        State s0 = auto.addState();
        State s1 = auto.addState();
        auto.setInitial(s0);
        auto.addTransition(s0, s1, 1, 2, 3);
        Automaton min = auto.minimize();
        assertEquals(1, min.getNbStates());
        assertFalse(min.run(1));
    }

    @Test
    public void boundedRepetition() {
        Automaton auto = Automaton.dfaFromString("1{2,4}");
        assertFalse(auto.run(1));
        assertTrue(auto.run(1, 1));
        assertTrue(auto.run(1, 1, 1, 1));
        assertFalse(auto.run(1, 1, 1, 1, 1));
        assertEquals(5, auto.getNbStates());
    }

    @Test
    public void intervalSetAgainstBitSet() {
        Random r = new Random(7);
        for (int i = 0; i < 5000; i++) {
            BitSet bitsA = new BitSet(), bitsB = new BitSet();
            IntervalSet a = randomSet(r, bitsA), b = randomSet(r, bitsB);
            for (int v = 0; v < 64; v++)
                assertEquals(bitsA.get(v), a.contains(v));

            BitSet union = (BitSet) bitsA.clone();
            union.or(bitsB);
            IntervalSet u = a.clone();
            u.add(b);
            assertEquals(fromBits(union), u);
            assertEquals(fromBits(union), a.union(b));

            BitSet inter = (BitSet) bitsA.clone();
            inter.and(bitsB);
            assertEquals(fromBits(inter), a.intersection(b));
            assertEquals(!inter.isEmpty(), a.intersects(b));

            BitSet diff = (BitSet) bitsA.clone();
            diff.andNot(bitsB);
            assertEquals(fromBits(diff), a.minus(b));
            assertEquals(a, a.complement().complement());
            assertEquals(bitsA.cardinality(), a.size());
        }
    }

    private static IntervalSet randomSet(Random r, BitSet bits) {
        IntervalSet set = new IntervalSet();
        int nb = r.nextInt(6);
        for (int i = 0; i < nb; i++) {
            int min = r.nextInt(60);
            int max = min + r.nextInt(5);
            set.add(new Interval(min, max));
            bits.set(min, max + 1);
        }
        return set;
    }

    private static IntervalSet fromBits(BitSet bits) {
        return IntervalSet.fromIntArray(bits.stream().toArray());
    }
}
