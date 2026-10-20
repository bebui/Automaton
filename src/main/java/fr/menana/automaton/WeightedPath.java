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
package fr.menana.automaton;

/**
 * Immutable witness: logical edges, concrete consumed symbols and additive cost.
 */
public final class WeightedPath {
    private final AutomatonGraph graph;
    private final int start;
    private final int[] edges, word;
    private final double cost;

    WeightedPath(AutomatonGraph graph, int start, int[] edges, int[] word, double cost) {
        this.graph = graph; this.start = start; this.edges = edges.clone(); this.word = word.clone(); this.cost = cost;
        graph.checkState(start);
        int s = start, position = 0;
        for (int e : edges) {
            if (graph.source[e] != s) throw new IllegalArgumentException("Disconnected path");
            s = graph.target[e];
            if (!graph.epsilon[e] && (position == word.length || !graph.accepts(e, word[position++])))
                throw new IllegalArgumentException("Path does not spell word");
        }
        if (position != word.length) throw new IllegalArgumentException("Wrong word length");
    }
    /**
     * @return shared graph
     */
    public AutomatonGraph graph() { return graph; }
    /**
     * @return initial vertex of this path (not necessarily the graph initial)
     */
    public int start() { return start; }
    /**
     * @return final vertex
     */
    public int end() { return edges.length == 0 ? start : graph.target[edges[edges.length - 1]]; }
    /**
     * @return number of edges, including epsilon edges
     */
    public int edgeCount() { return edges.length; }
    /**
     * @return number of consumed symbols
     */
    public int length() { return word.length; }
    /**
     * @param index position
     * @return logical edge ID
     */
    public int edge(int index) { return edges[index]; }
    /**
     * @return defensive copy of edge IDs
     */
    public int[] edges() { return edges.clone(); }
    /**
     * @return defensive copy of the concrete word
     */
    public int[] word() { return word.clone(); }
    /**
     * @return cost, including the boundaries specified by the originating query
     */
    public double cost() { return cost; }
}
