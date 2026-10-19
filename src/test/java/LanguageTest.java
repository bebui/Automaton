import fr.menana.automaton.Automaton;
import fr.menana.automaton.CompiledDfa;
import fr.menana.automaton.Searcher;
import fr.menana.automaton.State;
import fr.menana.automaton.Transition;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Checks regular expressions, products and language functions against brute force references :
 * an interpreter of regular expressions written independently of the library, and the enumeration of short words
 */
public class LanguageTest {

    /** Symbols used by the generated expressions */
    private static final int NB_SYMBOLS = 3;

    /** Symbols of the enumerated words : the generated ones plus one used only through . and ~ */
    private static final int[] ALPHABET = {0, 1, 2, 7};

    private static final int MAX_LENGTH = 5;

    // ---------------------------------------------------------------- reference regular expressions

    /**
     * A regular expression tree with its own matcher : ends(word, i) is the set of j such that word[i..j) matches
     */
    private abstract static class Node {
        abstract String text();
        abstract BitSet ends(int[] word, int i);
        boolean usesComplementOrAny() { return false; }
        boolean matches(int[] word) { return ends(word, 0).get(word.length); }
    }

    private static Node symbol(int v) {
        return new Node() {
            String text() { return v < 10 && v >= 0 ? Integer.toString(v) : "<" + v + ">"; }
            BitSet ends(int[] w, int i) { BitSet b = new BitSet(); if (i < w.length && w[i] == v) b.set(i + 1); return b; }
        };
    }

    private static Node any() {
        return new Node() {
            String text() { return "."; }
            BitSet ends(int[] w, int i) { BitSet b = new BitSet(); if (i < w.length) b.set(i + 1); return b; }
            boolean usesComplementOrAny() { return true; }
        };
    }

    private static Node seq(Node l, Node r) {
        return new Node() {
            String text() { return "(" + l.text() + r.text() + ")"; }
            BitSet ends(int[] w, int i) {
                BitSet out = new BitSet();
                BitSet mid = l.ends(w, i);
                for (int j = mid.nextSetBit(0); j >= 0; j = mid.nextSetBit(j + 1)) out.or(r.ends(w, j));
                return out;
            }
            boolean usesComplementOrAny() { return l.usesComplementOrAny() || r.usesComplementOrAny(); }
        };
    }

    private static Node or(Node l, Node r) {
        return new Node() {
            String text() { return "(" + l.text() + "|" + r.text() + ")"; }
            BitSet ends(int[] w, int i) { BitSet b = l.ends(w, i); b.or(r.ends(w, i)); return b; }
            boolean usesComplementOrAny() { return l.usesComplementOrAny() || r.usesComplementOrAny(); }
        };
    }

    private static Node and(Node l, Node r) {
        return new Node() {
            String text() { return "(" + l.text() + "&" + r.text() + ")"; }
            BitSet ends(int[] w, int i) { BitSet b = l.ends(w, i); b.and(r.ends(w, i)); return b; }
            boolean usesComplementOrAny() { return l.usesComplementOrAny() || r.usesComplementOrAny(); }
        };
    }

    private static Node not(Node x) {
        return new Node() {
            String text() { return "(~" + x.text() + ")"; }
            BitSet ends(int[] w, int i) { BitSet b = new BitSet(); b.set(i, w.length + 1); b.andNot(x.ends(w, i)); return b; }
            boolean usesComplementOrAny() { return true; }
        };
    }

    /** x{min,max}, max = -1 meaning unbounded */
    private static Node repeat(Node x, int min, int max, String suffix) {
        return new Node() {
            String text() { return "(" + x.text() + ")" + suffix; }
            BitSet ends(int[] w, int i) {
                BitSet current = new BitSet();
                current.set(i);
                BitSet out = new BitSet();
                if (min == 0) out.set(i);
                BitSet seen = (BitSet) current.clone();
                for (int k = 1; max < 0 || k <= max; k++) {
                    BitSet next = new BitSet();
                    for (int j = current.nextSetBit(0); j >= 0; j = current.nextSetBit(j + 1)) next.or(x.ends(w, j));
                    if (max < 0) {
                        // Positions reached for the first time, enough to reach a fixpoint
                        BitSet fresh = (BitSet) next.clone();
                        fresh.andNot(seen);
                        if (k >= min) out.or(next);
                        if (fresh.isEmpty() && k >= min) break;
                        seen.or(next);
                        if (k > w.length + min + 1) break;
                    } else if (k >= min) {
                        out.or(next);
                    }
                    current = next;
                    if (current.isEmpty()) break;
                }
                return out;
            }
            boolean usesComplementOrAny() { return x.usesComplementOrAny(); }
        };
    }

    private static Node randomNode(Random r, int depth, boolean allowComplement) {
        int kind = depth <= 0 ? r.nextInt(3) : r.nextInt(allowComplement ? 13 : 10);
        switch (kind) {
            case 0: case 1: return symbol(r.nextInt(NB_SYMBOLS));
            case 2: return allowComplement && r.nextInt(3) == 0 ? any() : symbol(r.nextInt(NB_SYMBOLS));
            case 3: case 4: return seq(randomNode(r, depth - 1, allowComplement), randomNode(r, depth - 1, allowComplement));
            case 5: return or(randomNode(r, depth - 1, allowComplement), randomNode(r, depth - 1, allowComplement));
            case 6: return repeat(randomNode(r, depth - 1, allowComplement), 0, -1, "*");
            case 7: return repeat(randomNode(r, depth - 1, allowComplement), 1, -1, "+");
            case 8: return repeat(randomNode(r, depth - 1, allowComplement), 0, 1, "?");
            case 9: {
                int min = r.nextInt(3);
                int choice = r.nextInt(3);
                if (choice == 0) return repeat(randomNode(r, depth - 1, allowComplement), min, -1, "{" + min + ",}");
                int max = min + r.nextInt(3);
                if (choice == 1) return repeat(randomNode(r, depth - 1, allowComplement), 0, max, "{," + max + "}");
                return repeat(randomNode(r, depth - 1, allowComplement), min, max, "{" + min + "," + max + "}");
            }
            case 10: case 11: return and(randomNode(r, depth - 1, true), randomNode(r, depth - 1, true));
            default: return not(randomNode(r, depth - 1, true));
        }
    }

    private static List<int[]> allWords(int maxLength) {
        List<int[]> words = new ArrayList<>();
        words.add(new int[0]);
        int from = 0;
        for (int l = 1; l <= maxLength; l++) {
            int to = words.size();
            for (int i = from; i < to; i++)
                for (int v : ALPHABET) {
                    int[] w = Arrays.copyOf(words.get(i), l);
                    w[l - 1] = v;
                    words.add(w);
                }
            from = to;
        }
        return words;
    }

    private static final List<int[]> WORDS = allWords(MAX_LENGTH);

    // ---------------------------------------------------------------- tests

    @Test
    public void regularExpressionsMatchTheReferenceInterpreter() {
        Random r = new Random(11);
        for (int i = 0; i < 400; i++) {
            Node node = randomNode(r, 4, i % 2 == 0);
            String text = node.text();
            Automaton nfa = Automaton.nfaFromString(text);
            Automaton dfa = Automaton.dfaFromString(text);
            CompiledDfa compiled = dfa.compile();
            for (int[] w : WORDS) {
                boolean expected = node.matches(w);
                assertEquals(text + " " + Arrays.toString(w), expected, nfa.run(w));
                assertEquals(text + " " + Arrays.toString(w), expected, dfa.run(w));
                assertEquals(text + " " + Arrays.toString(w), expected, compiled.run(w));
            }
        }
    }

    @Test
    public void productsMatchBooleanCombinations() {
        Random r = new Random(12);
        for (int i = 0; i < 200; i++) {
            Node x = randomNode(r, 3, i % 3 == 0), y = randomNode(r, 3, i % 3 == 1);
            Automaton a = Automaton.nfaFromString(x.text()), b = Automaton.nfaFromString(y.text());
            Automaton da = a.minimize(), db = b.minimize();
            Automaton inter = a.intersection(b), diff = a.difference(b), union = da.union(db);
            for (int[] w : WORDS) {
                boolean ia = x.matches(w), ib = y.matches(w);
                String msg = x.text() + " / " + y.text() + " " + Arrays.toString(w);
                assertEquals(msg, ia && ib, inter.run(w));
                assertEquals(msg, ia && !ib, diff.run(w));
                assertEquals(msg, ia || ib, union.run(w));
            }
            assertTrue(union.isDeterministic());
        }
    }

    @Test
    public void decisionProceduresAgree() {
        Random r = new Random(13);
        for (int i = 0; i < 300; i++) {
            Node x = randomNode(r, 3, i % 4 == 0);
            Node y = r.nextInt(4) == 0 ? x : randomNode(r, 3, i % 4 == 1);
            Automaton a = Automaton.nfaFromString(x.text());
            Automaton b = r.nextBoolean() ? Automaton.nfaFromString(y.text()) : Automaton.dfaFromString(y.text());
            String msg = x.text() + " / " + y.text();

            boolean same = a.sameLanguage(b);
            int[] counter = a.counterExample(b);
            assertEquals(msg, same, counter == null);
            assertEquals(msg, same, a.difference(b).isEmpty() && b.difference(a).isEmpty());
            if (counter != null)
                assertNotEquals(msg, a.run(counter), b.run(counter));

            assertEquals(msg, a.difference(b).isEmpty(), a.isSubsetOf(b));
            assertEquals(msg, b.difference(a).isEmpty(), b.isSubsetOf(a));

            boolean bruteSame = true, bruteSubset = true;
            for (int[] w : WORDS) {
                bruteSame &= x.matches(w) == y.matches(w);
                bruteSubset &= !x.matches(w) || y.matches(w);
            }
            if (same) assertTrue(msg, bruteSame);
            if (a.isSubsetOf(b)) assertTrue(msg, bruteSubset);
            if (x == y) assertTrue(msg, same);
        }
    }

    @Test
    public void emptinessShortestWordAndFiniteness() {
        Random r = new Random(14);
        for (int i = 0; i < 300; i++) {
            Node x = randomNode(r, 3, i % 3 == 0);
            Automaton a = Automaton.nfaFromString(x.text());
            String msg = x.text();
            int[] shortest = a.shortestWord();
            assertEquals(msg, shortest == null, a.isEmpty());
            int bruteShortest = -1;
            for (int[] w : WORDS)
                if (x.matches(w)) { bruteShortest = w.length; break; }
            if (shortest != null) {
                assertTrue(msg, a.run(shortest));
                if (bruteShortest >= 0) assertEquals(msg, bruteShortest, shortest.length);
                else assertTrue(msg, shortest.length > MAX_LENGTH);
            } else {
                assertEquals(msg, -1, bruteShortest);
            }
            // A language is infinite iff it has a word whose length is between n and 2n-1, n states of its minimal DFA
            Automaton min = a.minimize();
            int n = Math.max(min.getNbStates(), 1);
            boolean infinite = false;
            for (int l = n; l < 2 * n && !infinite; l++)
                infinite = min.countWords(l).signum() > 0;
            assertEquals(msg, !infinite, a.isFinite());
            assertEquals(msg, a.complement().isEmpty(), a.isUniversal());
        }
        assertTrue(Automaton.dfaFromString(".*").isUniversal());
        assertTrue(Automaton.dfaFromString("~(1&2)").isUniversal());
        assertFalse(Automaton.dfaFromString("~1").isUniversal());
        assertTrue(Automaton.dfaFromString("1&2").isEmpty());
    }

    @Test
    public void countingAndEnumerationMatchBruteForce() {
        Random r = new Random(15);
        for (int i = 0; i < 200; i++) {
            Node x = randomNode(r, 3, false);
            Automaton a = Automaton.nfaFromString(x.text());
            List<int[]> expected = new ArrayList<>();
            int[] perLength = new int[MAX_LENGTH + 1];
            for (int[] w : WORDS) {
                if (x.matches(w)) {
                    expected.add(w);
                    perLength[w.length]++;
                }
            }
            for (int l = 0; l <= MAX_LENGTH; l++)
                assertEquals(x.text() + " length " + l, BigInteger.valueOf(perLength[l]), a.countWords(l));
            // Shortlex order : by length, then lexicographic
            expected.sort((u, v) -> u.length != v.length ? Integer.compare(u.length, v.length) : Arrays.compare(u, v));
            List<int[]> enumerated = new ArrayList<>();
            for (int[] w : a.words(MAX_LENGTH))
                enumerated.add(w);
            assertEquals(x.text(), expected.size(), enumerated.size());
            for (int k = 0; k < expected.size(); k++)
                assertArrayEquals(x.text(), expected.get(k), enumerated.get(k));
        }
    }

    @Test
    public void countingLargeIntervals() {
        assertEquals(BigInteger.ONE.shiftLeft(64), Automaton.dfaFromString("..").countWords(2));
        assertEquals(BigInteger.valueOf(3), Automaton.dfaFromString("1|2|<40>").countWords(1));
        assertEquals(BigInteger.ZERO, Automaton.dfaFromString("1|2|<40>").countWords(2));
        Iterator<int[]> words = Automaton.dfaFromString(".").words(1).iterator();
        assertArrayEquals(new int[]{Integer.MIN_VALUE}, words.next());
        assertArrayEquals(new int[]{Integer.MIN_VALUE + 1}, words.next());
    }

    @Test
    public void randomWordsAreUniform() {
        Automaton a = Automaton.dfaFromString("(1|2)(1|2|3)");   // 6 words of length 2
        Random r = new Random(16);
        Map<String, Integer> seen = new HashMap<>();
        for (int i = 0; i < 6000; i++) {
            int[] w = a.randomWord(2, r);
            assertTrue(a.run(w));
            seen.merge(Arrays.toString(w), 1, Integer::sum);
        }
        assertEquals(6, seen.size());
        for (int count : seen.values())
            assertTrue("count " + count, count > 850 && count < 1150);
        assertNull(a.randomWord(3, r));
        int[] big = Automaton.dfaFromString(".<5>.").randomWord(3, r);
        assertEquals(5, big[1]);
    }

    @Test
    public void searchMatchesNaiveLeftmostLongest() {
        Random r = new Random(17);
        for (int i = 0; i < 200; i++) {
            Node x = randomNode(r, 3, i % 3 == 0);
            Automaton a = Automaton.dfaFromString(x.text());
            Searcher searcher = a.searcher();
            for (int t = 0; t < 30; t++) {
                int[] text = new int[r.nextInt(9)];
                for (int k = 0; k < text.length; k++)
                    text[k] = ALPHABET[r.nextInt(ALPHABET.length)];
                String msg = x.text() + " in " + Arrays.toString(text);
                assertEquals(msg, naiveFind(x, text, 0), searcher.find(text));
                assertEquals(msg, naiveFind(x, text, 0) != null, searcher.contains(text));
                List<Searcher.Match> all = new ArrayList<>();
                int from = 0;
                while (from <= text.length) {
                    Searcher.Match m = naiveFind(x, text, from);
                    if (m == null) break;
                    all.add(m);
                    from = m.end() > m.start() ? m.end() : m.start() + 1;
                }
                assertEquals(msg, all, searcher.findAll(text));
            }
        }
    }

    private static Searcher.Match naiveFind(Node x, int[] text, int from) {
        for (int s = from; s <= text.length; s++)
            for (int e = text.length; e >= s; e--)
                if (x.matches(Arrays.copyOfRange(text, s, e)))
                    return new Searcher.Match(s, e);
        return null;
    }

    @Test
    public void epsilonRemovalAndReduction() {
        Random r = new Random(18);
        for (int i = 0; i < 200; i++) {
            Node x = randomNode(r, 4, false);
            Automaton a = Automaton.nfaFromString(x.text());
            Automaton noEps = a.removeEpsilon();
            Automaton reduced = a.reduce();
            for (Automaton auto : new Automaton[]{noEps, reduced})
                for (State s : auto.getStates())
                    for (Transition tr : s.getTransitions().values())
                        assertFalse(x.text(), tr.hasEpsilon());
            assertTrue(reduced.getNbStates() <= noEps.getNbStates());
            assertTrue(x.text(), a.sameLanguage(noEps));
            assertTrue(x.text(), a.sameLanguage(reduced));
            for (int[] w : WORDS) {
                assertEquals(x.text(), x.matches(w), noEps.run(w));
                assertEquals(x.text(), x.matches(w), reduced.run(w));
            }
        }
        // Position automaton : one state per symbol of the expression, plus the initial one
        assertEquals(6, Automaton.nfaFromString("(1|2)*1(1|2)").removeEpsilon().getNbStates());
    }

    /**
     * Reference bisimulation : refines blocks by signature (block, set of (symbol, block of destination)) until stable
     */
    private static int naiveBisimulationSize(Automaton a, int[] symbols) {
        List<State> states = a.getStates();
        int n = states.size();
        int[] block = new int[n];
        for (State s : states)
            block[s.getIndex()] = s.isAccept() ? 1 : 0;
        int nb = -1;
        while (true) {
            Map<List<Object>, Integer> ids = new HashMap<>();
            int[] next = new int[n];
            for (State s : states) {
                TreeSet<Long> signature = new TreeSet<>();
                for (Transition tr : s.getTransitions().values())
                    for (int v : symbols)
                        if (tr.accepts(v))
                            signature.add(((long) v << 32) | block[tr.getDest().getIndex()]);
                List<Object> key = Arrays.asList(block[s.getIndex()], signature);
                next[s.getIndex()] = ids.computeIfAbsent(key, k -> ids.size());
            }
            block = next;
            if (ids.size() == nb)
                return nb;
            nb = ids.size();
        }
    }

    @Test
    public void reductionIsTheCoarsestBisimulation() {
        Random r = new Random(19);
        int[] symbols = {0, 1, 2, 3};
        for (int i = 0; i < 1000; i++) {
            Automaton a = new Automaton();
            int n = 1 + r.nextInt(30);
            for (int k = 0; k < n; k++)
                a.addState();
            List<State> st = a.getStates();
            a.setInitial(st.get(0));
            for (State s : st)
                a.setAccept(s, r.nextInt(3) == 0);
            for (int k = 0; k < 2 * n + r.nextInt(2 * n + 1); k++) {
                State o = st.get(r.nextInt(n)), d = st.get(r.nextInt(n));
                if (r.nextInt(8) == 0) a.addEpsilonTransition(o, d);
                else if (r.nextInt(4) == 0) a.addTransition(o, d, new fr.menana.automaton.Interval(0, 1 + r.nextInt(3)));
                else a.addTransition(o, d, r.nextInt(4));
            }
            Automaton noEps = a.removeEpsilon();
            Automaton reduced = a.reduce();
            assertEquals("automaton " + i, naiveBisimulationSize(noEps, symbols), reduced.getNbStates());
            assertTrue("automaton " + i, a.sameLanguage(reduced));
        }
    }

    @Test
    public void complementAndIntersectionSyntax() {
        Automaton evenOnes = Automaton.dfaFromString("(11)*");
        Automaton notEven = Automaton.dfaFromString("~((11)*)");
        assertTrue(notEven.run(1));
        assertFalse(notEven.run(1, 1));
        assertTrue(notEven.run(2));
        assertTrue(evenOnes.complement().sameLanguage(notEven));
        Automaton both = Automaton.dfaFromString("(1|2)*1(1|2)*&(1|2)*2(1|2)*");
        assertTrue(both.run(1, 2));
        assertFalse(both.run(1, 1));
        // ~ applies to the next expression only, with lower priority for &
        assertTrue(Automaton.dfaFromString("~12").run(3, 2));
        assertTrue(Automaton.dfaFromString("1&1|2").run(2));
    }
}
