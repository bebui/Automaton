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

import java.util.List;

/**
 * Exact nondominated accepting vectors and immutable path witnesses; size may be exponential.
 */
public final class ParetoResult {
    private final List<MultiWeightedPath> paths;
    private final boolean allPaths;
    ParetoResult(List<MultiWeightedPath> paths, boolean allPaths) { this.paths = List.copyOf(paths); this.allPaths = allPaths; }
    /**
     * @return immutable representatives, or all paths if explicitly requested
     */
    public List<MultiWeightedPath> paths() { return paths; }
    /**
     * @return number of returned path representatives
     */
    public int size() { return paths.size(); }
    /**
     * @return whether equality preserves every path rather than one representative
     */
    public boolean includesAllPaths() { return allPaths; }
    /**
     * @return true; this implementation never silently truncates or approximates
     */
    public boolean isComplete() { return true; }
}
