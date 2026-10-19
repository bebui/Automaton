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
     * Hopcroft performs in O(n s log(n))
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
    public static MINIMIZATION_ALGO minimization_method = MINIMIZATION_ALGO.Hopcroft;

    /**
     * Returns a new minimal {@link fr.menana.automaton.Automaton} that recognizes the same language as the given {@link fr.menana.automaton.Automaton}<br>
     * The algorithm used is set by the variable {@link fr.menana.automaton.Operation#minimization_method}
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @return an equivalent minimal  automaton
     */
    public static Automaton minimize(Automaton base) {
        return minimize(base, minimization_method);
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
     * Minimizes the given {@link fr.menana.automaton.Automaton} using Hopcroft algorithm. <br>
     * Runs in O(k n log n) where n is the number of states and k the number of symbol classes
     * @param base the {@link fr.menana.automaton.Automaton} to minimize
     * @return a minimal {@link fr.menana.automaton.Automaton}
     */
    public static Automaton minimizeHopcroft(Automaton base) {
        base = base.isDeterministic() ? base.clone() : base.determinize();
        if (base == null || base.getNbStates() == 0 || base.getInitial() == null)
            return new Automaton();
        base.removeDeadStates();
        List<State> states = base.getStates();
        int n = states.size();
        if (states.stream().noneMatch(State::isAccept)) {
            Automaton out = new Automaton();
            out.setInitial(out.addState());
            return out;
        }

        List<IntervalSet> sets = new ArrayList<>();
        for (State s : states)
            for (Transition tr : s.transitions.values())
                sets.add(tr.values);
        Alphabet alphabet = Alphabet.of(sets);
        int k = alphabet.size();

        // Complete transition table, state n being a non accepting sink
        int nbStates = n + 1;
        int sink = n;
        int[] delta = new int[nbStates * k];
        Arrays.fill(delta, sink);
        for (State s : states) {
            for (Transition tr : s.transitions.values()) {
                if (tr.values == null)
                    continue;
                for (int c : alphabet.classesOf(tr.values))
                    delta[s.index * k + c] = tr.dest.index;
            }
        }

        // Inverse transitions : sources of (t, c) are invSrc[invStart[t*k+c] .. invStart[t*k+c+1]-1]
        int[] invStart = new int[nbStates * k + 1];
        for (int i = 0; i < delta.length; i++)
            invStart[delta[i] * k + i % k + 1]++;
        for (int i = 0; i < nbStates * k; i++)
            invStart[i + 1] += invStart[i];
        int[] invSrc = new int[delta.length];
        int[] fill = Arrays.copyOf(invStart, nbStates * k);
        for (int i = 0; i < delta.length; i++)
            invSrc[fill[delta[i] * k + i % k]++] = i / k;

        // Partition : the states of block b are elems[first[b] .. end[b]-1]
        int[] elems = new int[nbStates];
        int[] loc = new int[nbStates];
        int[] blockOf = new int[nbStates];
        int[] first = new int[nbStates];
        int[] end = new int[nbStates];
        int[] marked = new int[nbStates];
        int pos = 0;
        for (State s : states) {
            if (s.accept) {
                elems[pos] = s.index;
                loc[s.index] = pos++;
            }
        }
        int nbAccept = pos;
        for (int s = 0; s < nbStates; s++) {
            if (s == sink || !states.get(s).accept) {
                elems[pos] = s;
                loc[s] = pos++;
                blockOf[s] = 1;
            }
        }
        first[0] = 0;
        end[0] = nbAccept;
        first[1] = nbAccept;
        end[1] = nbStates;
        int nbBlocks = 2;

        // Waiting splitters (block, class)
        boolean[] waiting = new boolean[nbStates * k];
        int[] stack = new int[nbStates * k];
        int top = 0;
        int smallest = nbAccept <= nbStates - nbAccept ? 0 : 1;
        for (int c = 0; c < k; c++) {
            waiting[smallest * k + c] = true;
            stack[top++] = smallest * k + c;
        }

        int[] splitter = new int[nbStates];
        int[] touched = new int[nbStates];
        while (top > 0) {
            int pair = stack[--top];
            waiting[pair] = false;
            int b = pair / k;
            int c = pair % k;
            int size = end[b] - first[b];
            System.arraycopy(elems, first[b], splitter, 0, size);

            // Moves the predecessors of the splitter at the beginning of their block
            int nbTouched = 0;
            for (int i = 0; i < size; i++) {
                int key = splitter[i] * k + c;
                for (int j = invStart[key]; j < invStart[key + 1]; j++) {
                    int s = invSrc[j];
                    int bs = blockOf[s];
                    int m = first[bs] + marked[bs];
                    if (loc[s] >= m) {
                        int other = elems[m];
                        elems[m] = s;
                        elems[loc[s]] = other;
                        loc[other] = loc[s];
                        loc[s] = m;
                        if (marked[bs]++ == 0)
                            touched[nbTouched++] = bs;
                    }
                }
            }

            for (int t = 0; t < nbTouched; t++) {
                int bs = touched[t];
                int nbMarked = marked[bs];
                marked[bs] = 0;
                int bsSize = end[bs] - first[bs];
                if (nbMarked == bsSize)
                    continue;
                // The smallest part becomes the new block
                int nb = nbBlocks++;
                if (nbMarked <= bsSize - nbMarked) {
                    first[nb] = first[bs];
                    end[nb] = first[bs] + nbMarked;
                    first[bs] = end[nb];
                } else {
                    first[nb] = first[bs] + nbMarked;
                    end[nb] = end[bs];
                    end[bs] = first[nb];
                }
                for (int p = first[nb]; p < end[nb]; p++)
                    blockOf[elems[p]] = nb;
                for (int a = 0; a < k; a++) {
                    if (!waiting[nb * k + a]) {
                        waiting[nb * k + a] = true;
                        stack[top++] = nb * k + a;
                    }
                }
            }
        }

        // Builds the quotient automaton, numbering the blocks in breadth first order
        Automaton out = new Automaton();
        State[] blockState = new State[nbBlocks];
        int sinkBlock = blockOf[sink];
        int initBlock = blockOf[base.getInitial().index];
        List<Integer> queue = new ArrayList<>();
        blockState[initBlock] = out.addState();
        queue.add(initBlock);
        for (int q = 0; q < queue.size(); q++) {
            int b = queue.get(q);
            int r = elems[first[b]];
            State from = blockState[b];
            if (states.get(r).accept)
                out.setAccept(from);
            Map<Integer, IntervalSet> byTarget = new LinkedHashMap<>();
            for (int c = 0; c < k; c++) {
                int tb = blockOf[delta[r * k + c]];
                if (tb == sinkBlock)
                    continue;
                if (blockState[tb] == null) {
                    blockState[tb] = out.addState();
                    queue.add(tb);
                }
                alphabet.appendTo(byTarget.computeIfAbsent(tb, x -> new IntervalSet()), c);
            }
            for (Map.Entry<Integer, IntervalSet> e : byTarget.entrySet())
                out.addDisjointTransition(from, blockState[e.getKey()], e.getValue());
        }
        out.setInitial(blockState[initBlock]);
        return out;
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
     * @param nfa a non-deterministic {@link fr.menana.automaton.Automaton}
     * @return  a deterministic {@link fr.menana.automaton.Automaton}
     */
    public static Automaton determinize(Automaton nfa) {
        if (nfa.isDeterministic())
            return nfa.clone();

        if (nfa.getInitial() == null)
            return null;

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
        for (State s : automaton.getStates()) {
            map.put(s, out.addState());
        }
        for (State s : automaton.getStates()) {
            for (Transition t : s.transitions.values())
            {
                State d = t.dest;
                out.addTransition(map.get(d),map.get(s),t.values.clone());
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
                    out.addTransition(map.get(tr.orig),map.get(tr.dest),tr.values.clone());
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
        Automaton out = first.clone();
        Map<State, State> map = new HashMap<>();
        for (State s : second.getStates()) {
            map.put(s, out.addState());
        }
        for (State s : second.getStates()) {
            for (Transition tr : s.getTransitions().values()) {
                if (tr.values != null) {
                    out.addTransition(map.get(tr.orig),map.get(tr.dest),tr.values.clone());
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
     * Returns a new automaton that recognizes the complementary language of the given {@link fr.menana.automaton.Automaton}
     * @param automaton the automaton to complement
     * @return a new automaton recognizing the complementary language of the given  {@link fr.menana.automaton.Automaton}
     */
    public static Automaton complement(Automaton automaton) {
        Automaton out = automaton.clone();
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
