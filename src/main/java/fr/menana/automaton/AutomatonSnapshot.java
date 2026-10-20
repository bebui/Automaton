/*
 * Automaton
 * Copyright (c) 2026, Julien Menana, All rights reserved.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package fr.menana.automaton;

import fr.menana.automaton.weighted.AutomatonGraph;
import fr.menana.automaton.weighted.MultiWeightedAutomaton;
import fr.menana.automaton.weighted.WeightedAutomaton;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/**
 * Reusable structural snapshot connecting a mutable {@link Automaton} to scalar
 * and vector weightings. Every weighting created here shares the identical graph.
 * Structure is immutable; original State/Transition references are identity
 * metadata, not frozen copies. Callbacks must not mutate the source or race edits.
 * Capture weights before editing metadata that those callbacks inspect.
 * @since 4.1.0
 */
public final class AutomatonSnapshot {
    private final AutomatonGraph.ImportResult imported;

    private AutomatonSnapshot(AutomatonGraph.ImportResult imported) { this.imported = imported; }

    /**
     * Copies structure, preserving state/transition identity mappings and epsilon modes.
     * @param automaton mutable source; concurrent modification is unsupported
     * @return independent snapshot without determinizing or minimizing
     */
    public static AutomatonSnapshot of(Automaton automaton) {
        return new AutomatonSnapshot(AutomatonGraph.importAutomaton(automaton));
    }

    /** @return immutable graph shared by all weightings of this snapshot */
    public AutomatonGraph graph() { return imported.graph(); }

    /**
     * @param state original source state, even if it has since been reindexed
     * @return its captured graph-local ID
     * @throws IllegalArgumentException if the state did not belong to the snapshot
     */
    public int stateId(State state) { return imported.stateId(state); }

    /**
     * @param state graph-local state ID
     * @return original mutable state reference, used only as metadata
     */
    public State originalState(int state) { return imported.originalState(state); }

    /**
     * @param edge graph-local logical edge ID
     * @return original transition; consuming/epsilon modes may share this reference
     */
    public Transition originalTransition(int edge) { return imported.originalTransition(edge); }

    /** Mode-aware scalar callback, allowing different costs for a mixed transition. */
    @FunctionalInterface
    public interface TransitionWeight {
        /**
         * @param transition original transition identity
         * @param epsilon true for the nonconsuming mode
         * @return finite scalar cost
         */
        double weight(Transition transition, boolean epsilon);
    }

    /**
     * @param weights scalar callback, evaluated once per logical edge
     * @return weighting with zero initial/final costs, sharing this graph
     */
    public WeightedAutomaton weighted(ToDoubleFunction<Transition> weights) {
        Objects.requireNonNull(weights);
        return weighted((transition, epsilon) -> weights.applyAsDouble(transition));
    }

    /**
     * @param weights scalar callback distinguishing consuming and epsilon modes
     * @return weighting with zero initial/final costs, sharing this graph
     */
    public WeightedAutomaton weighted(TransitionWeight weights) {
        return weighted(weights, 0, state -> 0);
    }

    /**
     * @param weights finite costs, evaluated once per logical edge
     * @param initialWeight finite initial boundary cost
     * @param finalWeights finite costs, evaluated once per captured accepting state
     * @return weighting sharing this graph, with captured boundary costs
     */
    public WeightedAutomaton weighted(ToDoubleFunction<Transition> weights, double initialWeight,
                                      ToDoubleFunction<State> finalWeights) {
        Objects.requireNonNull(weights);
        return weighted((transition, epsilon) -> weights.applyAsDouble(transition), initialWeight, finalWeights);
    }

    /**
     * @param weights finite costs distinguishing consuming and epsilon modes
     * @param initialWeight finite initial boundary cost
     * @param finalWeights finite costs, evaluated once per captured accepting state
     * @return weighting sharing this graph, with captured boundary costs
     */
    public WeightedAutomaton weighted(TransitionWeight weights, double initialWeight,
                                      ToDoubleFunction<State> finalWeights) {
        Objects.requireNonNull(weights); Objects.requireNonNull(finalWeights);
        AutomatonGraph graph = graph();
        double[] edges = new double[graph.getNbTransitions()], finals = new double[graph.getNbStates()];
        for (int e = 0; e < edges.length; e++) edges[e] = weights.weight(originalTransition(e), graph.isEpsilon(e));
        for (int s = 0; s < finals.length; s++) if (graph.isAccept(s)) finals[s] = finalWeights.applyAsDouble(originalState(s));
        return WeightedAutomaton.of(graph, edges, initialWeight, finals);
    }

    /**
     * Creates correlated vector costs for each common path. Each callback is a
     * dimension; no independently chosen paths or componentwise minima are mixed.
     * @param dimensions one or more finite scalar callbacks, in dimension order
     * @return vector weighting with zero boundaries sharing this graph
     */
    @SafeVarargs
    public final MultiWeightedAutomaton multiWeighted(ToDoubleFunction<Transition>... dimensions) {
        Objects.requireNonNull(dimensions);
        if (dimensions.length == 0) throw new IllegalArgumentException("At least one dimension required");
        ToDoubleFunction<Transition>[] callbacks = dimensions.clone();
        for (ToDoubleFunction<Transition> callback : callbacks) Objects.requireNonNull(callback);
        AutomatonGraph graph = graph();
        double[] costs = new double[Math.multiplyExact(graph.getNbTransitions(), callbacks.length)];
        for (int e = 0; e < graph.getNbTransitions(); e++)
            for (int d = 0; d < callbacks.length; d++) costs[e * callbacks.length + d] = callbacks[d].applyAsDouble(originalTransition(e));
        return MultiWeightedAutomaton.of(graph, callbacks.length, costs);
    }

    /**
     * @param word consumed symbols
     * @return Boolean membership, independent of any weighting or numeric overflow
     */
    public boolean run(int... word) { return graph().run(word); }

    /**
     * @return independent mutable Boolean automaton; snapshot structure stays unchanged
     */
    public Automaton toAutomaton() { return graph().toAutomaton(); }
}
