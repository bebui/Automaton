# Changelog

## 4.1.0 — release preparation

- Connect `Automaton` directly to scalar/vector costs with `weighted`,
  `multiWeighted` and a reusable `AutomatonSnapshot` preserving original identity
  mappings and sharing one immutable graph across weightings.
- Support boundary callbacks, mode-aware epsilon costs, Boolean membership
  independent of costs and an explicit mutable `toAutomaton` projection.
- Share the primitive reachability kernel between legacy coaccessibility and
  immutable graph analysis/epsilon closure, preserving the distinction between
  coaccessible and useful states.
- Add independent matrix-oracle tests, executable integration documentation and
  before/after JMH comparisons. Preserve the published 4.0 public API.

See [Automaton and weights](docs/AUTOMATON_AND_WEIGHTS.md) and the
[integration plan](AUTOMATON_INTEGRATION_PLAN.md). The 4.1.0 release is being signed and validated for Maven Central.

## 4.0.0 — published 2026-10-20

### Weighted and multi-weighted automata

- Immutable shared `AutomatonGraph` with stable transition IDs, symbolic integer
  intervals, epsilon edges and separate scalar/vector weights and boundary costs.
- Scalar word evaluation and path reconstruction; DAG shortest/longest paths,
  Dijkstra, Bellman–Ford and relevant negative-cycle detection.
- Reusable finite-horizon graphs, position-dependent costs and conditional optima.
- Scalar/vector union, concatenation, synchronized intersection, reversal,
  repetition and cost-preserving simplifications with explicit semantics.
- Graph-sharing scalarization and dimension projection, correlated scalar
  weightings, independent synchronized scalar products, DAG lexicographic
  optimization, exact Pareto fronts and path reconstruction.
- Exact residual weighted determinization with a construction budget, deterministic
  weighted minimization, and lazy cyclic K-best paths/distinct-word enumeration
  under the documented numeric and cycle preconditions.
- Compiled scalar/vector DFA evaluation with reusable interval indexes and
  allocation-free existing-array/caller-buffer recognition.

### Organization and documentation

- Move the new weighted API into `fr.menana.automaton.weighted`, its compiled
  evaluators into `weighted.compiled`, and numeric helpers into `weighted.internal`.
- Extract weight-independent `WeightedDfaIndex`, with explicit sharing factories
  for independently constructed scalar and vector evaluators.
- Expose the existing immutable `Alphabet` partitioner and add primitive range
  reads and a defensive sorted-range factory to `IntervalSet`.
- Preserve public Boolean 3.0 class names, packages, signatures and algorithms.
- Provide a complete English manual, executable examples, generated public API
  index, [migration guide](docs/MIGRATION_4.md), independent correctness tests and
  isolated JMH benchmarks. See [the technical plan](RELEASE_4_PLAN.md).

Java 17 is required; no new runtime dependencies. Weighted imports from pre-4.0
development snapshots must be updated; see the migration guide.
