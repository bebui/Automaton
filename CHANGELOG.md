# Changelog

## 4.0.0 — prepared, not yet published

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

Java 17 is still required; no new runtime dependencies. Version preparation does
not publish Maven Central artifacts or create a release tag. Weighted imports from
development snapshots must be updated; see the migration guide.
