/*
 * Automaton
 * Copyright (c) 2026, Julien Menana, All rights reserved.
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package fr.menana.automaton.internal;

/**
 * Shared primitive reachability kernel for mutable and immutable representations.
 * All arrays are borrowed only for the duration of a call.
 * @hidden
 */
public final class GraphTraversal {
    private GraphTraversal() { }

    public static void propagate(int[] starts, int[] edges, int[] destinations,
                                 boolean[] reached, int[] queue, int tail,
                                 boolean[] allowedStates, boolean[] allowedEdges) {
        int head = 0;
        while (head < tail) {
            int state = queue[head++];
            for (int i = starts[state]; i < starts[state + 1]; i++) {
                int edge = edges == null ? i : edges[i];
                if (allowedEdges != null && !allowedEdges[edge]) continue;
                int next = destinations[edge];
                if (!reached[next] && (allowedStates == null || allowedStates[next])) {
                    reached[next] = true;
                    queue[tail++] = next;
                }
            }
        }
    }
}
