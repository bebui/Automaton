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
/**
 * Scalar and vector additive weights on shared immutable interval multigraphs.
 * Logical edges have stable graph-local IDs; parallel and epsilon paths retain
 * their own costs. Includes path algorithms, weighted algebra, finite horizons,
 * exact Pareto/lexicographic optimization and conditional advanced transforms.
 * Primitive CSR data remains internal to this cohesive engine package.
 * Compiled evaluators live in {@link fr.menana.automaton.weighted.compiled}.
 */
package fr.menana.automaton.weighted;
