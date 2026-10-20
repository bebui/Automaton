/*
 * Automaton
 * Copyright (c) 2026, Julien Menana, All rights reserved.
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
package fr.menana.automaton.weighted;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.internal.WeightMath;
import fr.menana.automaton.weighted.compiled.*;

import java.math.BigDecimal;

/**
 * Immutable accepting path and its additive cost vector; no componentwise artificial optimum is formed.
 */
public final class MultiWeightedPath {
    private final WeightedPath path;
    private final double[] costs;
    private final BigDecimal[] exact;
    MultiWeightedPath(WeightedPath path, double[] costs) { this(path, costs, null); }
    MultiWeightedPath(WeightedPath path, double[] costs, BigDecimal[] exact) { this.path = path; this.costs = costs.clone(); this.exact = exact == null ? null : exact.clone(); }
    BigDecimal[] exactVector() { return exact; }
    /**
     * @return shared graph
     */
    public AutomatonGraph graph() { return path.graph(); }
    /**
     * @return initial state of witness
     */
    public int start() { return path.start(); }
    /**
     * @return final state of witness
     */
    public int end() { return path.end(); }
    /**
     * @return copied logical edges
     */
    public int[] edges() { return path.edges(); }
    /**
     * @return copied concrete word
     */
    public int[] word() { return path.word(); }
    /**
     * @return number of consumed symbols
     */
    public int length() { return path.length(); }
    /**
     * @return copied cost vector including boundaries
     */
    public double[] costs() { return costs.clone(); }
    /**
     * @param dimension zero-based dimension
     * @return additive cost
     */
    public double cost(int dimension) { return costs[dimension]; }
    /**
     * @param dimension zero-based component
     * @return exact sum of the binary-double inputs used by the multiobjective solver
     */
    public BigDecimal exactCost(int dimension) { return exact == null ? new BigDecimal(costs[dimension]) : exact[dimension]; }
}
