/*
 * Automaton
 * Copyright (c) 2025, Julien Menana, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package fr.menana.automaton;

import java.math.BigInteger;
import java.util.*;

/**
 * Algorithms on the languages recognized by automata : products, emptiness, equivalence, inclusion,
 * shortest words, counting, sampling and enumeration. <br>
 * Used through the methods of {@link fr.menana.automaton.Automaton} and {@link fr.menana.automaton.Operation}
 */
final class Languages {

    private Languages() {
        // Hiding the implicit public one
    }

    /**
     * Combination of the acceptance of two automata in a product
     */
    enum Product {
        UNION, INTERSECTION, DIFFERENCE, SYMMETRIC_DIFFERENCE;

        boolean accepts(boolean left, boolean right) {
            switch (this) {
                case UNION: return left || right;
                case INTERSECTION: return left && right;
                case DIFFERENCE: return left && !right;
                default: return left != right;
            }
        }

        /**
         * Checks if a pair of states can still lead to an accepted word, <code>DEAD</code> states recognizing nothing
         */
        boolean alive(int left, int right) {
            switch (this) {
                case INTERSECTION: return left != CompiledDfa.DEAD && right != CompiledDfa.DEAD;
                case DIFFERENCE: return left != CompiledDfa.DEAD;
                default: return left != CompiledDfa.DEAD || right != CompiledDfa.DEAD;
            }
        }
    }

    /**
     * Receives the maximal intervals of symbols on which two states go to the same pair of states
     */
    private interface SegmentConsumer {
        void accept(int min, int max, int leftDest, int rightDest);
    }

    /**
     * Sweeps the transitions of a state of each automaton together, in increasing order of symbols.
     * Symbols leaving no state are skipped, a state missing a symbol goes to <code>DEAD</code>
     */
    private static void sweep(CompiledDfa a, int p, CompiledDfa b, int q, SegmentConsumer consumer) {
        int i = a.firstTransition(p), iEnd = a.endTransition(p);
        int j = b.firstTransition(q), jEnd = b.endTransition(q);
        long x = Long.MIN_VALUE;
        while (i < iEnd || j < jEnd) {
            long nextA = i < iEnd ? a.min(i) : Long.MAX_VALUE;
            long nextB = j < jEnd ? b.min(j) : Long.MAX_VALUE;
            if (x < Math.min(nextA, nextB))
                x = Math.min(nextA, nextB);
            boolean inA = nextA <= x, inB = nextB <= x;
            long end = Math.min(inA ? a.max(i) : nextA - 1, inB ? b.max(j) : nextB - 1);
            consumer.accept((int) x, (int) end, inA ? a.dest(i) : CompiledDfa.DEAD, inB ? b.dest(j) : CompiledDfa.DEAD);
            if (inA && a.max(i) == end) i++;
            if (inB && b.max(j) == end) j++;
            x = end + 1;
        }
    }

    private static long pairKey(int p, int q) {
        return ((long) (p + 1) << 32) | (q + 1);
    }

    /**
     * Builds the product of two automata, exploring only the pairs of states reachable from the initial pair
     * @param left the first automaton
     * @param right the second automaton
     * @param product how the acceptance of both automata is combined
     * @return a deterministic automaton
     */
    static Automaton product(Automaton left, Automaton right, Product product) {
        CompiledDfa a = CompiledDfa.of(left);
        CompiledDfa b = CompiledDfa.of(right);
        Automaton out = new Automaton();
        int initA = a.getInitialState(), initB = b.getInitialState();
        if (!product.alive(initA, initB)) {
            out.setInitial(out.addState());
            return out;
        }
        Map<Long, State> pairs = new HashMap<>();
        List<int[]> queue = new ArrayList<>();
        State init = out.addState();
        out.setInitial(init);
        pairs.put(pairKey(initA, initB), init);
        queue.add(new int[]{initA, initB});
        for (int v = 0; v < queue.size(); v++) {
            int p = queue.get(v)[0], q = queue.get(v)[1];
            State from = out.getStates().get(v);
            if (product.accepts(a.isAccept(p), b.isAccept(q)))
                out.setAccept(from);
            Map<State, IntervalSet> byTarget = new LinkedHashMap<>();
            sweep(a, p, b, q, (min, max, dp, dq) -> {
                if (!product.alive(dp, dq))
                    return;
                State target = pairs.get(pairKey(dp, dq));
                if (target == null) {
                    target = out.addState();
                    pairs.put(pairKey(dp, dq), target);
                    queue.add(new int[]{dp, dq});
                }
                byTarget.computeIfAbsent(target, x -> new IntervalSet()).append(min, max);
            });
            for (Map.Entry<State, IntervalSet> e : byTarget.entrySet())
                out.addDisjointTransition(from, e.getKey(), e.getValue());
        }
        return out;
    }

    /**
     * Checks if two automata recognize the same language with the algorithm of Hopcroft and Karp :
     * pairs of states that must be equivalent are merged with a union-find structure, in almost linear time
     */
    static boolean sameLanguage(Automaton left, Automaton right) {
        CompiledDfa a = CompiledDfa.of(left);
        CompiledDfa b = CompiledDfa.of(right);
        int na = a.getNbStates(), nb = b.getNbStates();
        // Nodes : states of a, dead state of a, states of b, dead state of b
        int deadA = na, offsetB = na + 1, deadB = na + 1 + nb;
        int[] parent = new int[na + nb + 2];
        for (int i = 0; i < parent.length; i++)
            parent[i] = i;
        int startA = a.getInitialState() == CompiledDfa.DEAD ? deadA : a.getInitialState();
        int startB = b.getInitialState() == CompiledDfa.DEAD ? deadB : offsetB + b.getInitialState();
        union(parent, deadA, deadB);
        Deque<int[]> todo = new ArrayDeque<>();
        if (union(parent, startA, startB))
            todo.push(new int[]{a.getInitialState(), b.getInitialState()});
        while (!todo.isEmpty()) {
            int[] pair = todo.pop();
            if (a.isAccept(pair[0]) != b.isAccept(pair[1]))
                return false;
            sweep(a, pair[0], b, pair[1], (min, max, dp, dq) -> {
                int x = dp == CompiledDfa.DEAD ? deadA : dp;
                int y = dq == CompiledDfa.DEAD ? deadB : offsetB + dq;
                if (union(parent, x, y))
                    todo.push(new int[]{dp, dq});
            });
        }
        return true;
    }

    private static int find(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    private static boolean union(int[] parent, int x, int y) {
        int rx = find(parent, x), ry = find(parent, y);
        if (rx == ry)
            return false;
        parent[rx] = ry;
        return true;
    }

    /**
     * Returns a shortest word accepted by exactly one of two automata, found by a breadth first search of their product
     */
    static int[] counterExample(Automaton left, Automaton right) {
        CompiledDfa a = CompiledDfa.of(left);
        CompiledDfa b = CompiledDfa.of(right);
        Map<Long, Integer> seen = new HashMap<>();
        List<int[]> nodes = new ArrayList<>();   // {p, q, parent node, symbol}
        nodes.add(new int[]{a.getInitialState(), b.getInitialState(), -1, 0});
        seen.put(pairKey(a.getInitialState(), b.getInitialState()), 0);
        for (int v = 0; v < nodes.size(); v++) {
            int p = nodes.get(v)[0], q = nodes.get(v)[1];
            if (a.isAccept(p) != b.isAccept(q))
                return wordTo(nodes, v);
            int from = v;
            sweep(a, p, b, q, (min, max, dp, dq) -> {
                if (seen.putIfAbsent(pairKey(dp, dq), nodes.size()) == null)
                    nodes.add(new int[]{dp, dq, from, min});
            });
        }
        return null;
    }

    private static int[] wordTo(List<int[]> nodes, int node) {
        int length = 0;
        for (int v = node; nodes.get(v)[2] >= 0; v = nodes.get(v)[2])
            length++;
        int[] word = new int[length];
        for (int v = node; nodes.get(v)[2] >= 0; v = nodes.get(v)[2])
            word[--length] = nodes.get(v)[3];
        return word;
    }

    /**
     * Checks if the language of an automaton is included in the language of another one,
     * without determinizing them, using antichains (De Wulf, Doyen, Henzinger and Raskin). <br>
     * Explores the pairs (state of left, set of states of right) reachable by the same word, and fails on a pair whose
     * state accepts while the set does not. A pair is skipped when a pair with the same state and a smaller set was already seen
     */
    static boolean isSubsetOf(Automaton left, Automaton right) {
        Automaton a = Operation.removeEpsilon(left);
        Automaton b = Operation.removeEpsilon(right);
        if (a.getInitial() == null)
            return true;
        List<IntervalSet> sets = new ArrayList<>();
        for (Automaton auto : new Automaton[]{a, b})
            for (State s : auto.getStates())
                for (Transition tr : s.transitions.values())
                    sets.add(tr.values);
        Alphabet alphabet = Alphabet.of(sets);
        int k = alphabet.size();
        int[][][] succA = successors(a, alphabet);
        int[][][] succB = successors(b, alphabet);
        BitSet acceptB = new BitSet();
        for (State s : b.getStates())
            if (s.accept)
                acceptB.set(s.index);

        List<List<BitSet>> antichains = new ArrayList<>();
        for (int i = 0; i < a.getNbStates(); i++)
            antichains.add(new ArrayList<>());
        BitSet start = new BitSet();
        if (b.getInitial() != null)
            start.set(b.getInitial().index);
        Deque<Object[]> todo = new ArrayDeque<>();
        antichains.get(a.getInitial().index).add(start);
        todo.push(new Object[]{a.getInitial().index, start});
        while (!todo.isEmpty()) {
            Object[] pair = todo.pop();
            int p = (Integer) pair[0];
            BitSet set = (BitSet) pair[1];
            if (a.getStates().get(p).accept && !set.intersects(acceptB))
                return false;
            for (int c = 0; c < k; c++) {
                int[] nextA = succA[p][c];
                if (nextA == null)
                    continue;
                BitSet next = new BitSet();
                for (int s = set.nextSetBit(0); s >= 0; s = set.nextSetBit(s + 1))
                    if (succB[s][c] != null)
                        for (int d : succB[s][c])
                            next.set(d);
                for (int pNext : nextA) {
                    if (addToAntichain(antichains.get(pNext), next))
                        todo.push(new Object[]{pNext, next});
                }
            }
        }
        return true;
    }

    /**
     * Adds a set to an antichain of minimal sets, unless it contains one of them
     * @return <code>true</code> if and only if the set was added
     */
    private static boolean addToAntichain(List<BitSet> antichain, BitSet set) {
        for (BitSet other : antichain) {
            BitSet diff = (BitSet) other.clone();
            diff.andNot(set);
            if (diff.isEmpty())
                return false;
        }
        antichain.removeIf(other -> {
            BitSet diff = (BitSet) set.clone();
            diff.andNot(other);
            return diff.isEmpty();
        });
        antichain.add(set);
        return true;
    }

    /**
     * Returns for each state and each symbol class the destinations of an automaton without epsilon transitions
     */
    private static int[][][] successors(Automaton auto, Alphabet alphabet) {
        int n = auto.getNbStates(), k = alphabet.size();
        int[][][] succ = new int[n][k][];
        for (State s : auto.getStates()) {
            for (Transition tr : s.transitions.values()) {
                if (tr.values == null)
                    continue;
                for (int c : alphabet.classesOf(tr.values)) {
                    int[] old = succ[s.index][c];
                    int[] grown = old == null ? new int[1] : Arrays.copyOf(old, old.length + 1);
                    grown[grown.length - 1] = tr.dest.index;
                    succ[s.index][c] = grown;
                }
            }
        }
        return succ;
    }

    /**
     * Returns a shortest accepted word, epsilon transitions costing nothing
     */
    static int[] shortestWord(Automaton auto) {
        State init = auto.getInitial();
        if (init == null)
            return null;
        int n = auto.getNbStates();
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        int[] parent = new int[n];
        int[] symbol = new int[n];
        boolean[] done = new boolean[n];
        Deque<Integer> todo = new ArrayDeque<>();
        dist[init.index] = 0;
        parent[init.index] = -1;
        todo.add(init.index);
        List<State> states = auto.getStates();
        while (!todo.isEmpty()) {
            int s = todo.pollFirst();
            if (done[s])
                continue;
            done[s] = true;
            State state = states.get(s);
            if (state.accept) {
                int[] word = new int[dist[s]];
                for (int v = s, i = dist[s]; parent[v] >= 0; v = parent[v])
                    if (dist[parent[v]] < dist[v])
                        word[--i] = symbol[v];
                return word;
            }
            for (Transition tr : state.transitions.values()) {
                int d = tr.dest.index;
                if (tr.epsilon && dist[s] < dist[d]) {
                    dist[d] = dist[s];
                    parent[d] = s;
                    todo.addFirst(d);
                }
                if (tr.values != null && !tr.values.isEmpty() && dist[s] + 1 < dist[d]) {
                    dist[d] = dist[s] + 1;
                    parent[d] = s;
                    symbol[d] = tr.values.getMin();
                    todo.addLast(d);
                }
            }
        }
        return null;
    }

    /**
     * Checks if an automaton recognizes a finite language : no cycle reading a symbol goes through a useful state
     */
    static boolean isFinite(Automaton auto) {
        Automaton a = Operation.removeEpsilon(auto);
        a.removeDeadStates();
        if (a.getInitial() == null)
            return true;
        // Iterative depth first search looking for a back edge among live states
        int n = a.getNbStates();
        int[] color = new int[n];  // 0 unvisited, 1 on the stack, 2 done
        List<State> states = a.getStates();
        Deque<Iterator<Transition>> iterators = new ArrayDeque<>();
        Deque<Integer> path = new ArrayDeque<>();
        Set<State> useful = a.getUseFulStates();
        color[a.getInitial().index] = 1;
        path.push(a.getInitial().index);
        iterators.push(a.getInitial().transitions.values().iterator());
        while (!path.isEmpty()) {
            Iterator<Transition> it = iterators.peek();
            if (!it.hasNext()) {
                color[path.pop()] = 2;
                iterators.pop();
                continue;
            }
            State dest = it.next().dest;
            if (!useful.contains(dest))
                continue;
            if (color[dest.index] == 1)
                return false;
            if (color[dest.index] == 0) {
                color[dest.index] = 1;
                path.push(dest.index);
                iterators.push(states.get(dest.index).transitions.values().iterator());
            }
        }
        return true;
    }

    /**
     * Counts the words of a given length, as a dynamic programming on the deterministic automaton
     */
    static BigInteger countWords(Automaton auto, int length) {
        CompiledDfa dfa = CompiledDfa.of(auto);
        BigInteger[][] counts = countTable(dfa, length);
        return dfa.getInitialState() == CompiledDfa.DEAD ? BigInteger.ZERO : counts[length][dfa.getInitialState()];
    }

    /**
     * Returns counts[l][s] : the number of words of length l accepted from state s
     */
    private static BigInteger[][] countTable(CompiledDfa dfa, int length) {
        if (length < 0)
            throw new IllegalArgumentException("Negative length: " + length);
        int n = dfa.getNbStates();
        BigInteger[][] counts = new BigInteger[length + 1][n];
        for (int s = 0; s < n; s++)
            counts[0][s] = dfa.isAccept(s) ? BigInteger.ONE : BigInteger.ZERO;
        for (int l = 1; l <= length; l++) {
            for (int s = 0; s < n; s++) {
                BigInteger total = BigInteger.ZERO;
                for (int t = dfa.firstTransition(s); t < dfa.endTransition(s); t++) {
                    BigInteger next = counts[l - 1][dfa.dest(t)];
                    if (next.signum() != 0)
                        total = total.add(next.multiply(BigInteger.valueOf((long) dfa.max(t) - dfa.min(t) + 1)));
                }
                counts[l][s] = total;
            }
        }
        return counts;
    }

    /**
     * Draws a word of a given length uniformly among the accepted words of that length
     */
    static int[] randomWord(Automaton auto, int length, Random random) {
        CompiledDfa dfa = CompiledDfa.of(auto);
        if (dfa.getInitialState() == CompiledDfa.DEAD)
            return null;
        BigInteger[][] counts = countTable(dfa, length);
        if (counts[length][dfa.getInitialState()].signum() == 0)
            return null;
        int[] word = new int[length];
        int s = dfa.getInitialState();
        for (int i = 0; i < length; i++) {
            int remaining = length - i - 1;
            BigInteger r = uniform(counts[length - i][s], random);
            for (int t = dfa.firstTransition(s); t < dfa.endTransition(s); t++) {
                BigInteger each = counts[remaining][dfa.dest(t)];
                BigInteger weight = each.multiply(BigInteger.valueOf((long) dfa.max(t) - dfa.min(t) + 1));
                if (r.compareTo(weight) < 0) {
                    word[i] = (int) (dfa.min(t) + r.divide(each).longValue());
                    s = dfa.dest(t);
                    break;
                }
                r = r.subtract(weight);
            }
        }
        return word;
    }

    /**
     * Returns a uniform random integer in [0, bound)
     */
    private static BigInteger uniform(BigInteger bound, Random random) {
        BigInteger r;
        do {
            r = new BigInteger(bound.bitLength(), random);
        } while (r.compareTo(bound) >= 0);
        return r;
    }

    /**
     * Enumerates the accepted words of length at most maxLength, by increasing length then in lexicographic order
     */
    static Iterable<int[]> words(Automaton auto, int maxLength) {
        if (maxLength < 0)
            throw new IllegalArgumentException("Negative length: " + maxLength);
        CompiledDfa dfa = CompiledDfa.of(auto);
        int n = dfa.getNbStates();
        // canAccept[l] : states from which a word of length exactly l is accepted
        BitSet[] canAccept = new BitSet[maxLength + 1];
        canAccept[0] = new BitSet();
        for (int s = 0; s < n; s++)
            if (dfa.isAccept(s))
                canAccept[0].set(s);
        for (int l = 1; l <= maxLength; l++) {
            canAccept[l] = new BitSet();
            for (int s = 0; s < n; s++)
                for (int t = dfa.firstTransition(s); t < dfa.endTransition(s); t++)
                    if (canAccept[l - 1].get(dfa.dest(t))) {
                        canAccept[l].set(s);
                        break;
                    }
        }
        return () -> new WordIterator(dfa, canAccept, maxLength);
    }

    /**
     * Depth first enumeration of the words of each length, keeping one frame (state, transition, symbol) per letter
     */
    private static final class WordIterator implements Iterator<int[]> {
        private final CompiledDfa dfa;
        private final BitSet[] canAccept;
        private final int maxLength;
        private int length = -1;
        private int[] states;
        private int[] transitions;
        private long[] symbols;
        private int depth;
        private int[] next;

        WordIterator(CompiledDfa dfa, BitSet[] canAccept, int maxLength) {
            this.dfa = dfa;
            this.canAccept = canAccept;
            this.maxLength = maxLength;
            nextLength();
            advance(false);
        }

        /**
         * Starts the enumeration of the next length from which the initial state accepts something
         */
        private boolean nextLength() {
            int init = dfa.getInitialState();
            do {
                length++;
            } while (length <= maxLength && (init == CompiledDfa.DEAD || !canAccept[length].get(init)));
            if (length > maxLength)
                return false;
            states = new int[length + 1];
            transitions = new int[length];
            symbols = new long[length];
            states[0] = init;
            depth = 0;
            return true;
        }

        /**
         * Computes the next word, resuming after the current one if <code>resume</code> is set
         */
        private void advance(boolean resume) {
            next = null;
            if (length > maxLength)
                return;
            boolean descending = !resume;
            while (true) {
                if (descending) {
                    if (depth == length) {
                        next = new int[length];
                        for (int i = 0; i < length; i++)
                            next[i] = (int) symbols[i];
                        return;
                    }
                    // Opens the first useful transition of the current state
                    transitions[depth] = dfa.firstTransition(states[depth]) - 1;
                    symbols[depth] = Long.MAX_VALUE;
                }
                // Moves to the next symbol at the current depth, or backtracks
                if (depth == length || !nextSymbol()) {
                    if (depth == 0 || length == 0) {
                        if (!nextLength())
                            return;
                        descending = true;
                        continue;
                    }
                    depth--;
                    descending = false;
                    continue;
                }
                states[depth + 1] = dfa.dest(transitions[depth]);
                depth++;
                descending = true;
            }
        }

        private boolean nextSymbol() {
            int t = transitions[depth];
            if (t >= dfa.firstTransition(states[depth]) && symbols[depth] < dfa.max(t)) {
                symbols[depth]++;
                return true;
            }
            int remaining = length - depth - 1;
            for (t = t + 1; t < dfa.endTransition(states[depth]); t++) {
                if (canAccept[remaining].get(dfa.dest(t))) {
                    transitions[depth] = t;
                    symbols[depth] = dfa.min(t);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public int[] next() {
            if (next == null)
                throw new NoSuchElementException();
            int[] word = next;
            advance(true);
            return word;
        }
    }
}
