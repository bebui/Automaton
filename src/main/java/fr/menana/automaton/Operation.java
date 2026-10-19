/**
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

import java.util.*;

/**
 * This class consists of static methods to manipulate a finite {@link fr.menana.automaton.Automaton}. <br>
 * The operation that can be performed are minimization, determinization, union, concatenation, reversion and complement <br>
 * Created by Julien Menana on 01/05/2015.
 */
public class Operation {

    private Operation() {
        // Hiding the implicit public one
    }

    /**
     * Enum of the available minimization algorithms <br>
     * Brzozowski performs in O(2^n) where n is the number of states in the automaton, but has a good average case-complexity <br>
     * Hopcroft performs in O(m log(n)) where m is the number of transitions, using Valmari and Lehtinen's refinement
     */
    public enum MINIMIZATION_ALGO {
        /**
         * Brzozowski's algorithm.
         */
        Brzozowski,
        /**
         * Hopcroft's algorithm.
         */
        Hopcroft,
    }

    /**
     * The minimization algorithm to use when {@link fr.menana.automaton.Operation#minimize(Automaton)} is called
     */
    private static volatile MINIMIZATION_ALGO defaultMinimization = MINIMIZATION_ALGO.Hopcroft;

    /**
     * Returns the minimization algorithm used by {@link fr.menana.automaton.Operation#minimize(Automaton)}
     * @return the default minimization algorithm
     */
    public static MINIMIZATION_ALGO getDefaultMinimization() {
        return defaultMinimization;
    }

    /**
     * Sets the minimization algorithm used by {@link fr.menana.automaton.Operation#minimize(Automaton)} for the whole application. <br>
     * Prefer {@link fr.menana.automaton.Operation#minimize(Automaton, MINIMIZATION_ALGO)} to choose it for a single call
     * @param method the default minimization algorithm
     */
    public static void setDefaultMinimization(MINIMIZATION_ALGO method) {
        defaultMinimization = Objects.requireNonNull(method);
    }

    /**
     * Returns a new minimal {@link fr.menana.automaton.Automaton} that recognizes the same language as the given {@link fr.menana.automaton.Automaton}<br>
     * The algorithm used is set by {@link fr.menana.automaton.Operation#setDefaultMinimization(MINIMIZATION_ALGO)}
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @return an equivalent minimal  automaton
     */
    public static Automaton minimize(Automaton base) {
        return minimize(base, defaultMinimization);
    }

    /**
     * Returns a new minimal {@link fr.menana.automaton.Automaton} that recognizes the same language as the given {@link fr.menana.automaton.Automaton}<br>
     * The algorithm used is set by the element in the given enum {@link fr.menana.automaton.Operation.MINIMIZATION_ALGO}
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @param method the minimization algorithm to use
     * @return an equivalent minimal  automaton
     */
    public static Automaton minimize(Automaton base, MINIMIZATION_ALGO method) {
        switch(method){
            case Brzozowski : return minimizeBrzozowski(base);
            case Hopcroft   : return minimizeHopcroft(base);
            default         : return minimizeHopcroft(base);
        }
    }

    /**
     * Minimizes the given {@link fr.menana.automaton.Automaton} by partition refinement. <br>
     * Uses the algorithm of Valmari and Lehtinen, a variant of Hopcroft's algorithm that refines the
     * partition of the states and the partition of the transitions together. It runs in O(m log n),
     * where n is the number of states and m the number of transitions, and works directly on partial automata
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @return a minimal {@link fr.menana.automaton.Automaton}
     */
    public static Automaton minimizeHopcroft(Automaton base) {
        if (!base.isDeterministic())
            base = base.determinize();
        if (base.getInitial() == null)
            return new Automaton();

        // Live states : reachable from the initial state and leading to an accepting state, numbered from 0 to n-1
        List<State> all = base.getStates();
        int[] live = liveStates(base);
        List<State> states = new ArrayList<>();
        for (State s : all)
            if (live[s.index] >= 0)
                states.add(s);
        int n = states.size();
        if (live[base.getInitial().index] < 0) {
            Automaton out = new Automaton();
            out.setInitial(out.addState());
            return out;
        }

        // Transitions (tail, label, head) between live states, one per symbol class
        List<IntervalSet> sets = new ArrayList<>();
        for (State s : states)
            for (Transition tr : s.transitions.values())
                if (live[tr.dest.index] >= 0)
                    sets.add(tr.values);
        Alphabet alphabet = Alphabet.of(sets);
        int k = alphabet.size();
        int[][] classes = new int[sets.size()][];
        int m = 0;
        for (int i = 0; i < classes.length; i++) {
            classes[i] = alphabet.classesOf(sets.get(i));
            m += classes[i].length;
        }
        int[] tail = new int[m];
        int[] label = new int[m];
        int[] head = new int[m];
        int t = 0, cl = 0;
        for (State s : states) {
            for (Transition tr : s.transitions.values()) {
                if (live[tr.dest.index] < 0)
                    continue;
                for (int c : classes[cl]) {
                    tail[t] = live[s.index];
                    label[t] = c;
                    head[t++] = live[tr.dest.index];
                }
                cl++;
            }
        }

        // Blocks : partition of the states, initially accepting / non accepting
        Partition blocks = new Partition(n);
        for (State s : states)
            if (s.accept)
                blocks.mark(live[s.index]);
        blocks.split();

        // Cords : partition of the transitions by label
        int[] byLabel = new int[k + 1];
        for (int i = 0; i < m; i++)
            byLabel[label[i] + 1]++;
        for (int c = 0; c < k; c++)
            byLabel[c + 1] += byLabel[c];
        int[] order = new int[m];
        int[] fill = Arrays.copyOf(byLabel, k);
        for (int i = 0; i < m; i++)
            order[fill[label[i]]++] = i;
        Partition cords = new Partition(order, byLabel);

        // Transitions grouped by head : those going into q are into[intoStart[q] .. intoStart[q+1]-1]
        int[] intoStart = new int[n + 1];
        for (int i = 0; i < m; i++)
            intoStart[head[i] + 1]++;
        for (int q = 0; q < n; q++)
            intoStart[q + 1] += intoStart[q];
        int[] into = new int[m];
        fill = Arrays.copyOf(intoStart, n);
        for (int i = 0; i < m; i++)
            into[fill[head[i]]++] = i;

        // Every cord splits the blocks, every block except the first one splits the cords
        int b = 1, c = 0;
        while (c < cords.z) {
            for (int i = cords.first[c]; i < cords.past[c]; i++)
                blocks.mark(tail[cords.elems[i]]);
            blocks.split();
            c++;
            while (b < blocks.z) {
                for (int i = blocks.first[b]; i < blocks.past[b]; i++) {
                    int q = blocks.elems[i];
                    for (int j = intoStart[q]; j < intoStart[q + 1]; j++)
                        cords.mark(into[j]);
                }
                cords.split();
                b++;
            }
        }

        // Builds the quotient automaton, numbering the blocks in breadth first order
        Automaton out = new Automaton();
        State[] blockState = new State[blocks.z];
        IntervalSet[] toBlock = new IntervalSet[blocks.z];
        int[] targets = new int[blocks.z];
        int initBlock = blocks.setOf[live[base.getInitial().index]];
        int[] queue = new int[blocks.z];
        int queued = 0;
        blockState[initBlock] = out.addState();
        queue[queued++] = initBlock;
        for (int q = 0; q < queued; q++) {
            int block = queue[q];
            State representative = states.get(blocks.elems[blocks.first[block]]);
            State from = blockState[block];
            if (representative.accept)
                out.setAccept(from);
            int nbTargets = 0;
            for (Transition tr : representative.transitions.values()) {
                if (tr.values == null || live[tr.dest.index] < 0)
                    continue;
                int target = blocks.setOf[live[tr.dest.index]];
                if (blockState[target] == null) {
                    blockState[target] = out.addState();
                    queue[queued++] = target;
                }
                if (toBlock[target] == null) {
                    toBlock[target] = tr.values.clone();
                    targets[nbTargets++] = target;
                } else {
                    toBlock[target].add(tr.values);
                }
            }
            for (int i = 0; i < nbTargets; i++) {
                out.addDisjointTransition(from, blockState[targets[i]], toBlock[targets[i]]);
                toBlock[targets[i]] = null;
            }
        }
        out.setInitial(blockState[initBlock]);
        return out;
    }

    /**
     * Numbers the states of an automaton without epsilon transitions that are reachable from the initial state
     * and from which an accepting state can be reached
     * @param auto the automaton, whose state indexes are their positions
     * @return for each state index, its number among live states, or -1 if the state is not live
     */
    private static int[] liveStates(Automaton auto) {
        List<State> states = auto.getStates();
        int n = states.size();
        // Forward and backward adjacency as index arrays
        int[] outStart = new int[n + 1];
        int[] inStart = new int[n + 1];
        for (State s : states) {
            for (Transition tr : s.transitions.values()) {
                outStart[s.index + 1]++;
                inStart[tr.dest.index + 1]++;
            }
        }
        for (int i = 0; i < n; i++) {
            outStart[i + 1] += outStart[i];
            inStart[i + 1] += inStart[i];
        }
        int[] outDest = new int[outStart[n]];
        int[] inOrig = new int[inStart[n]];
        int[] inFill = Arrays.copyOf(inStart, n);
        for (State s : states) {
            int o = outStart[s.index];
            for (Transition tr : s.transitions.values()) {
                outDest[o++] = tr.dest.index;
                inOrig[inFill[tr.dest.index]++] = s.index;
            }
        }
        boolean[] reached = new boolean[n];
        int[] stack = new int[n];
        int top = 0;
        reached[auto.getInitial().index] = true;
        stack[top++] = auto.getInitial().index;
        while (top > 0) {
            int s = stack[--top];
            for (int i = outStart[s]; i < outStart[s + 1]; i++) {
                if (!reached[outDest[i]]) {
                    reached[outDest[i]] = true;
                    stack[top++] = outDest[i];
                }
            }
        }
        boolean[] useful = new boolean[n];
        for (State s : states) {
            if (s.accept && reached[s.index]) {
                useful[s.index] = true;
                stack[top++] = s.index;
            }
        }
        while (top > 0) {
            int s = stack[--top];
            for (int i = inStart[s]; i < inStart[s + 1]; i++) {
                int o = inOrig[i];
                if (reached[o] && !useful[o]) {
                    useful[o] = true;
                    stack[top++] = o;
                }
            }
        }
        int[] live = new int[n];
        int nb = 0;
        for (int i = 0; i < n; i++)
            live[i] = useful[i] ? nb++ : -1;
        return live;
    }

    /**
     * Refinable partition of the integers 0 .. n-1, as described by Valmari and Lehtinen. <br>
     * The elements of set s are elems[first[s] .. past[s]-1]. Marked elements of a set are moved to its beginning
     */
    private static final class Partition {
        int z;
        final int[] elems;
        final int[] loc;
        final int[] setOf;
        final int[] first;
        final int[] past;
        private final int[] marked;
        private final int[] touched;
        private int nbTouched;

        /**
         * Creates a partition with a single set containing all the elements
         * @param n the number of elements
         */
        Partition(int n) {
            elems = new int[n];
            loc = new int[n];
            setOf = new int[n];
            first = new int[n + 1];
            past = new int[n + 1];
            marked = new int[n + 1];
            touched = new int[n + 1];
            for (int i = 0; i < n; i++)
                elems[i] = loc[i] = i;
            z = n > 0 ? 1 : 0;
            past[0] = n;
        }

        /**
         * Creates a partition from elements grouped in consecutive ranges, empty ranges being ignored
         * @param order the elements, grouped
         * @param bounds group g is order[bounds[g] .. bounds[g+1]-1]
         */
        Partition(int[] order, int[] bounds) {
            this(order.length);
            System.arraycopy(order, 0, elems, 0, order.length);
            z = 0;
            for (int g = 0; g + 1 < bounds.length; g++) {
                if (bounds[g] == bounds[g + 1])
                    continue;
                first[z] = bounds[g];
                past[z] = bounds[g + 1];
                for (int i = bounds[g]; i < bounds[g + 1]; i++) {
                    loc[elems[i]] = i;
                    setOf[elems[i]] = z;
                }
                z++;
            }
        }

        void mark(int e) {
            int s = setOf[e], i = loc[e], j = first[s] + marked[s];
            if (i < j)
                return;
            elems[i] = elems[j];
            loc[elems[i]] = i;
            elems[j] = e;
            loc[e] = j;
            if (marked[s]++ == 0)
                touched[nbTouched++] = s;
        }

        /**
         * Splits every touched set into its marked and unmarked elements, the smallest part becoming a new set
         */
        void split() {
            while (nbTouched > 0) {
                int s = touched[--nbTouched], j = first[s] + marked[s];
                if (j == past[s]) {
                    marked[s] = 0;
                    continue;
                }
                if (marked[s] <= past[s] - j) {
                    first[z] = first[s];
                    past[z] = first[s] = j;
                } else {
                    past[z] = past[s];
                    first[z] = past[s] = j;
                }
                for (int i = first[z]; i < past[z]; i++)
                    setOf[elems[i]] = z;
                marked[s] = marked[z++] = 0;
            }
        }
    }

    /**
     * Minimizes the given {@link fr.menana.automaton.Automaton} using Brzozowski algorithm
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @return a minimal {@link fr.menana.automaton.Automaton}
     */
    public static Automaton minimizeBrzozowski(Automaton base) {
        Automaton a = base.isDeterministic() ? base : base.determinize();
        if (a.getNbStates() == 0)
            return new Automaton();
        a = a.revert();//
        a = a.determinize(); // .determinize();
        a = a.revert().determinize();
        a.removeDeadStates();
        return a;
    }


    /**
     * Determinizes a given {@link fr.menana.automaton.Automaton} using the subset construction algorithm.<br>
     * If the {@link fr.menana.automaton.Automaton} is already deterministic, return a clone.
     * If it has no initial state, returns an empty automaton.
     * @param nfa a non-deterministic {@link fr.menana.automaton.Automaton}
     * @return  a deterministic {@link fr.menana.automaton.Automaton}
     */
    public static Automaton determinize(Automaton nfa) {
        if (nfa.isDeterministic())
            return nfa.clone();

        if (nfa.getInitial() == null)
            return new Automaton();

        List<State> states = nfa.getStates();
        int n = states.size();
        Map<State, Integer> ids = new HashMap<>(2 * n);
        for (int i = 0; i < n; i++)
            ids.put(states.get(i), i);

        List<IntervalSet> sets = new ArrayList<>();
        for (State s : states)
            for (Transition tr : s.transitions.values())
                sets.add(tr.values);
        Alphabet alphabet = Alphabet.of(sets);
        int k = alphabet.size();

        // Symbol edges (class, destination) and epsilon edges of each state, stored contiguously
        int[] edgeStart = new int[n + 1];
        int[] epsStart = new int[n + 1];
        List<int[]> classes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int nbEdges = 0, nbEps = 0;
            for (Transition tr : states.get(i).transitions.values()) {
                if (tr.values != null) {
                    int[] cl = alphabet.classesOf(tr.values);
                    classes.add(cl);
                    nbEdges += cl.length;
                }
                if (tr.epsilon)
                    nbEps++;
            }
            edgeStart[i + 1] = edgeStart[i] + nbEdges;
            epsStart[i + 1] = epsStart[i] + nbEps;
        }
        int[] edgeClass = new int[edgeStart[n]];
        int[] edgeDest = new int[edgeStart[n]];
        int[] epsDest = new int[epsStart[n]];
        int e = 0, eps = 0, cl = 0;
        for (int i = 0; i < n; i++) {
            for (Transition tr : states.get(i).transitions.values()) {
                int dest = ids.get(tr.dest);
                if (tr.values != null) {
                    for (int c : classes.get(cl++)) {
                        edgeClass[e] = c;
                        edgeDest[e++] = dest;
                    }
                }
                if (tr.epsilon)
                    epsDest[eps++] = dest;
            }
        }

        // Only states having symbol transitions or accepting matter in a subset
        boolean[] relevant = new boolean[n];
        for (int i = 0; i < n; i++)
            relevant[i] = states.get(i).accept || edgeStart[i + 1] > edgeStart[i];

        Closure closure = new Closure(n, epsStart, epsDest, relevant);

        Automaton dfa = new Automaton();
        Map<Subset, State> association = new HashMap<>();
        List<int[]> toVisit = new ArrayList<>();

        Subset start = closure.of(new int[]{ids.get(nfa.getInitial())}, 1);
        State dfaInit = dfa.addState();
        dfa.setInitial(dfaInit);
        association.put(start, dfaInit);
        toVisit.add(start.states);

        // Destinations reached by each class from the current subset
        int[][] buckets = new int[k][];
        int[] bucketSize = new int[k];
        int[] touched = new int[k];

        for (int v = 0; v < toVisit.size(); v++) {
            int[] current = toVisit.get(v);
            State from = dfa.getStates().get(v);
            int nbTouched = 0;
            for (int s : current) {
                if (states.get(s).accept)
                    from.accept = true;
                for (int i = edgeStart[s]; i < edgeStart[s + 1]; i++) {
                    int c = edgeClass[i];
                    if (bucketSize[c] == 0) {
                        touched[nbTouched++] = c;
                        if (buckets[c] == null)
                            buckets[c] = new int[4];
                    } else if (bucketSize[c] == buckets[c].length) {
                        buckets[c] = Arrays.copyOf(buckets[c], 2 * bucketSize[c]);
                    }
                    buckets[c][bucketSize[c]++] = edgeDest[i];
                }
            }
            Arrays.sort(touched, 0, nbTouched);
            Map<State, IntervalSet> byTarget = new LinkedHashMap<>();
            for (int t = 0; t < nbTouched; t++) {
                int c = touched[t];
                Subset next = closure.of(buckets[c], bucketSize[c]);
                bucketSize[c] = 0;
                if (next.states.length == 0)
                    continue;
                State dfaState = association.get(next);
                if (dfaState == null) {
                    dfaState = dfa.addState();
                    association.put(next, dfaState);
                    toVisit.add(next.states);
                }
                alphabet.appendTo(byTarget.computeIfAbsent(dfaState, x -> new IntervalSet()), c);
            }
            for (Map.Entry<State, IntervalSet> entry : byTarget.entrySet())
                dfa.addDisjointTransition(from, entry.getKey(), entry.getValue());
        }

        return dfa;
    }

    /**
     * A set of NFA states identified by their sorted indexes, used as a key of the subset construction
     */
    private static final class Subset {
        final int[] states;
        final int hash;

        Subset(int[] states) {
            this.states = states;
            this.hash = Arrays.hashCode(states);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Subset && ((Subset) other).hash == hash && Arrays.equals(((Subset) other).states, states);
        }
    }

    /**
     * Computes epsilon-closures, reusing its buffers from one call to another
     */
    private static final class Closure {
        private final int[] epsStart;
        private final int[] epsDest;
        private final boolean[] relevant;
        private final int[] seen;
        private final int[] stack;
        private final int[] result;
        private int generation;

        Closure(int n, int[] epsStart, int[] epsDest, boolean[] relevant) {
            this.epsStart = epsStart;
            this.epsDest = epsDest;
            this.relevant = relevant;
            this.seen = new int[n];
            this.stack = new int[n];
            this.result = new int[n];
        }

        /**
         * Returns the relevant states of the epsilon-closure of the given states
         * @param from the start states
         * @param size the number of start states to consider in <code>from</code>
         * @return the closure as a subset
         */
        Subset of(int[] from, int size) {
            generation++;
            int top = 0, nb = 0;
            for (int i = 0; i < size; i++) {
                if (seen[from[i]] != generation) {
                    seen[from[i]] = generation;
                    stack[top++] = from[i];
                }
            }
            while (top > 0) {
                int s = stack[--top];
                if (relevant[s])
                    result[nb++] = s;
                for (int i = epsStart[s]; i < epsStart[s + 1]; i++) {
                    int d = epsDest[i];
                    if (seen[d] != generation) {
                        seen[d] = generation;
                        stack[top++] = d;
                    }
                }
            }
            int[] out = Arrays.copyOf(result, nb);
            Arrays.sort(out);
            return new Subset(out);
        }
    }

    /**
     * Returns a new {@link fr.menana.automaton.Automaton} that recognizes the mirror of the language defined by the given {@link fr.menana.automaton.Automaton}
     * @param automaton the {@link fr.menana.automaton.Automaton} to revert
     * @return a reverted {@link fr.menana.automaton.Automaton}
     */
    public static Automaton revert(Automaton automaton) {
        Map<State, State> map = new HashMap<>();
        Automaton out = new Automaton();
        if (automaton.getInitial() == null)
            return out;
        for (State s : automaton.getStates()) {
            map.put(s, out.addState());
        }
        for (State s : automaton.getStates()) {
            for (Transition t : s.transitions.values())
            {
                State d = t.dest;
                if (t.values != null)
                    out.addTransition(map.get(d),map.get(s),t.values);
                if (t.epsilon)
                    out.addEpsilonTransition(map.get(d),map.get(s));
            }
        }
        out.setAccept(map.get(automaton.getInitial()));
        State init = out.addState();
        out.setInitial(init);
        for (State s : automaton.getAcceptList())
        {
            out.addEpsilonTransition(init,map.get(s));
        }
        return out;
    }

    /**
     * Returns a new automaton recognizing the concatenation of the languages defined by the {@link fr.menana.automaton.Automaton} given as a parameter
     * The returned automaton is deterministic if and only if both given automatons already are
     * @param first the first {@link fr.menana.automaton.Automaton} used for the concatenation
     * @param second the second {@link fr.menana.automaton.Automaton} used for the concatenation
     * @return a new automaton recognizing the concatenation of the languages defined by the {@link fr.menana.automaton.Automaton} parameters
     */
    public static Automaton concatenate(Automaton first, Automaton second) {
        return concatenate(first, second, true);
    }

    /**
     * Returns a new automaton recognizing the concatenation of the languages defined by the {@link fr.menana.automaton.Automaton} given as a parameter. <br>
     * Unless minimization is requested, the states of <code>first</code> keep their index in the returned automaton,
     * which is non-deterministic as soon as <code>first</code> has an accepting state
     * @param first the first {@link fr.menana.automaton.Automaton} used for the concatenation
     * @param second the second {@link fr.menana.automaton.Automaton} used for the concatenation
     * @param minimizeIfDeterministic <code>true</code> to minimize the result when both given automatons are deterministic
     * @return a new automaton recognizing the concatenation of the languages defined by the {@link fr.menana.automaton.Automaton} parameters
     */
    public static Automaton concatenate(Automaton first, Automaton second, boolean minimizeIfDeterministic) {
        // An automaton without initial state recognizes the empty language, and so does the concatenation
        if (first.getInitial() == null || second.getInitial() == null)
            return new Automaton();
        Automaton out = first.clone();
        Map<State, State> map = new HashMap<>();
        for (State s : second.getStates()) {
            map.put(s, out.addState());
        }
        for (State s : out.getAcceptList()) {
            out.addEpsilonTransition(s,map.get(second.getInitial()));
            out.setAccept(s, false);
        }
        for (State s : second.getAcceptList()) {
            out.setAccept(map.get(s));
        }
        for (State s : second.getStates()) {
            for (Transition tr : s.getTransitions().values()) {
                if (tr.values != null) {
                    out.addTransition(map.get(tr.orig),map.get(tr.dest),tr.values);
                }
                if (tr.hasEpsilon())
                    out.addEpsilonTransition(map.get(tr.orig),map.get(tr.dest));
            }
        }
        return (minimizeIfDeterministic && first.isDeterministic() && second.isDeterministic()?out.minimize():out) ;
    }

    /**
     * Returns a new automaton recognizing the union of the languages defined by the {@link fr.menana.automaton.Automaton} given as a parameter
     * The returned automaton is deterministic if and only if both given automatons already are
     * @param first the first {@link fr.menana.automaton.Automaton} used for the union
     * @param second the second {@link fr.menana.automaton.Automaton} used for the union
     * @return a new automaton recognizing the union of the languages defined by the {@link fr.menana.automaton.Automaton} parameters
     */
    public static Automaton union(Automaton first, Automaton second) {
        if (first.getInitial() == null)
            return second.clone();
        else if (second.getInitial() == null)
            return first.clone();
        // Two deterministic automata : the product stays deterministic and avoids a determinization
        if (first.isDeterministic() && second.isDeterministic())
            return Languages.product(first, second, Languages.Product.UNION).minimize();
        Automaton out = first.clone();
        Map<State, State> map = new HashMap<>();
        for (State s : second.getStates()) {
            map.put(s, out.addState());
        }
        for (State s : second.getStates()) {
            for (Transition tr : s.getTransitions().values()) {
                if (tr.values != null) {
                    out.addTransition(map.get(tr.orig),map.get(tr.dest),tr.values);
                }
                if (tr.hasEpsilon())
                    out.addEpsilonTransition(map.get(tr.orig),map.get(tr.dest));
            }
        }
        for (State s : second.getAcceptList()) {
            out.setAccept(map.get(s));
        }
        State init = out.addState();
        out.addEpsilonTransition(init,out.getInitial());
        out.addEpsilonTransition(init, map.get(second.getInitial()));
        out.setInitial(init);
        return (first.isDeterministic() && second.isDeterministic()?out.minimize():out) ;
    }

    /**
     * Returns a new automaton recognizing the intersection of the languages of the given automata. <br>
     * Built as the product of their deterministic automata, exploring only reachable pairs of states, then minimized
     * @param first the first {@link fr.menana.automaton.Automaton}
     * @param second the second {@link fr.menana.automaton.Automaton}
     * @return a minimal deterministic automaton recognizing the words accepted by both automata
     */
    public static Automaton intersection(Automaton first, Automaton second) {
        return Languages.product(first, second, Languages.Product.INTERSECTION).minimize();
    }

    /**
     * Returns a new automaton recognizing the words of a language that are not in another one. <br>
     * Built as the product of their deterministic automata, then minimized
     * @param first the {@link fr.menana.automaton.Automaton} whose words are kept
     * @param second the {@link fr.menana.automaton.Automaton} whose words are removed
     * @return a minimal deterministic automaton recognizing the words accepted by <code>first</code> and not by <code>second</code>
     */
    public static Automaton difference(Automaton first, Automaton second) {
        return Languages.product(first, second, Languages.Product.DIFFERENCE).minimize();
    }

    /**
     * Returns a new automaton without epsilon transitions recognizing the same language. <br>
     * Its states are the initial state and the targets of the transitions reading a symbol : applied to the automaton
     * of a regular expression, it gives the position (Glushkov) automaton, with one state per symbol of the expression plus one.
     * The number of transitions can grow quadratically, for instance for <code>(1|2|...|n)*</code>
     * @param automaton the {@link fr.menana.automaton.Automaton}
     * @return an equivalent automaton without epsilon transitions
     */
    public static Automaton removeEpsilon(Automaton automaton) {
        Automaton out = new Automaton();
        State init = automaton.getInitial();
        if (init == null)
            return out;
        List<State> states = automaton.getStates();
        int n = states.size();
        // New state for the initial state and for every target of a symbol transition
        State[] copy = new State[n];
        copy[init.index] = out.addState();
        out.setInitial(copy[init.index]);
        for (State s : states)
            for (Transition tr : s.transitions.values())
                if (tr.values != null && copy[tr.dest.index] == null)
                    copy[tr.dest.index] = out.addState();
        int[] seen = new int[n];
        int[] stack = new int[n];
        int generation = 0;
        for (int i = 0; i < n; i++) {
            if (copy[i] == null)
                continue;
            State from = copy[i];
            // Every symbol transition of the epsilon-closure of the state leaves its copy
            generation++;
            int top = 0;
            seen[i] = generation;
            stack[top++] = i;
            while (top > 0) {
                State s = states.get(stack[--top]);
                if (s.accept)
                    out.setAccept(from);
                for (Transition tr : s.transitions.values()) {
                    if (tr.values != null)
                        out.addTransition(from, copy[tr.dest.index], tr.values);
                    if (tr.epsilon && seen[tr.dest.index] != generation) {
                        seen[tr.dest.index] = generation;
                        stack[top++] = tr.dest.index;
                    }
                }
            }
        }
        out.removeDeadStates();
        return out;
    }

    /**
     * Returns a new automaton recognizing the same language with fewer states, without determinizing it. <br>
     * Removes the epsilon transitions, then merges the states that are bisimilar : states with the same acceptance
     * whose transitions reach the same groups of states on the same symbols. The result may still be non-deterministic.
     * Reducing an automaton before determinizing it can make determinization much cheaper
     * @param automaton the {@link fr.menana.automaton.Automaton}
     * @return a smaller or equal equivalent automaton without epsilon transitions
     */
    public static Automaton reduce(Automaton automaton) {
        Automaton a = removeEpsilon(automaton);
        List<State> states = a.getStates();
        int n = states.size();
        if (n == 0)
            return a;
        List<IntervalSet> sets = new ArrayList<>();
        for (State s : states)
            for (Transition tr : s.transitions.values())
                sets.add(tr.values);
        Alphabet alphabet = Alphabet.of(sets);
        // Transitions (source, class, destination)
        int m = 0;
        int[][] classes = new int[sets.size()][];
        for (int i = 0; i < classes.length; i++) {
            classes[i] = alphabet.classesOf(sets.get(i));
            m += classes[i].length;
        }
        int[] src = new int[m], lab = new int[m], dst = new int[m];
        int t = 0, cl = 0;
        for (State s : states)
            for (Transition tr : s.transitions.values())
                for (int c : classes[cl++]) {
                    src[t] = s.index;
                    lab[t] = c;
                    dst[t++] = tr.dest.index;
                }
        int[] block = Bisimulation.coarsest(n, src, lab, dst, alphabet.size(), states);
        int nbBlocks = 0;
        for (int b : block)
            nbBlocks = Math.max(nbBlocks, b + 1);
        if (nbBlocks == n)
            return a;
        // Quotient automaton
        Automaton out = new Automaton();
        State[] blockState = new State[nbBlocks];
        for (int b = 0; b < nbBlocks; b++)
            blockState[b] = out.addState();
        boolean[] done = new boolean[nbBlocks];
        for (State s : states) {
            int b = block[s.index];
            if (done[b])
                continue;
            done[b] = true;
            if (s.accept)
                out.setAccept(blockState[b]);
            for (Transition tr : s.transitions.values())
                out.addTransition(blockState[b], blockState[block[tr.dest.index]], tr.values);
        }
        out.setInitial(blockState[block[a.getInitial().index]]);
        out.reIndex();
        return out;
    }

    /**
     * Returns a new automaton that recognizes the complementary language of the given {@link fr.menana.automaton.Automaton}
     * @param automaton the automaton to complement
     * @return a new automaton recognizing the complementary language of the given  {@link fr.menana.automaton.Automaton}
     */
    public static Automaton complement(Automaton automaton) {
        // Swapping accepting states is only valid on a deterministic automaton
        Automaton out;
        if (automaton.getInitial() == null)
            out = new Automaton();
        else
            out = automaton.isDeterministic() ? automaton.clone() : automaton.determinize();
        if (out.getInitial() == null)
            out.setInitial(out.addState());
        State poubelle = out.addState();
        Map<State,IntervalSet> toAdd = new HashMap<>();
        for (State s : out.getStates()) {
            IntervalSet set = IntervalSet.ALL.clone();
            for (Transition tr : s.transitions.values()) {
                if (tr.values != null) {
                    set = set.intersection(tr.values.complement());
                }
            }
            out.setAccept(s,!s.accept);
            toAdd.put(s,set);
        }
        for (Map.Entry<State,IntervalSet> couple : toAdd.entrySet()) {
            if (!couple.getValue().isEmpty())
                out.addTransition(couple.getKey(),poubelle,couple.getValue());
        }
        out.setAccept(poubelle);
        return out;
    }


  /*  public static void main(String[] args) {
        Automaton a = Automaton.nfaFromString("((72345)|((7|8)+))");
       // System.out.println(a);
        a = a.determinize();
        System.out.println(a);
        a.toDotty("det.dot");
        //System.out.println(a.minimize());
        Automaton b = a.minimize();
        //State out = b.addState();
        //b.addTransition(out,b.getInitial(),13,14);
       // b = b.minimize();
        b.toDotty("hop.dot");
        System.out.println(b);
        Operation.minimization_method = MINIMIZATION_ALGO.Brzozowski;
        a.minimize().toDotty("brz.dot");
       // System.out.println(a.minimize());



    }*/
}
