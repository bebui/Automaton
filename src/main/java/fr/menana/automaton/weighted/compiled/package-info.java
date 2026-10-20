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
 * Immutable scalar/vector deterministic evaluators using a shared
 * {@link fr.menana.automaton.weighted.WeightedDfaIndex}. Compilation requires an
 * epsilon-free deterministic graph. Reweighting, projection and scalarization
 * reuse lookup arrays without retaining earlier weights. Caller-owned output
 * buffers make direct vector recognition allocation-free after setup.
 */
package fr.menana.automaton.weighted.compiled;
