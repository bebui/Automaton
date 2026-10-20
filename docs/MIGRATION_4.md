# Migrating to Automaton 4.0

## Version and availability

The published `fr.menana:automaton:4.0.0` release brings
scalar/vector weighted automata, path algorithms, finite horizons, multiobjective
optimization and the package organization below. Java 17 remains the minimum;
there are no new production dependencies. JMH remains an optional separate module.

The current checkout develops `4.1.0-SNAPSHOT`; build/install locally for its
[new Automaton/weights bridge](AUTOMATON_AND_WEIGHTS.md). This page records the
4.0 package migration, which remains valid. The published 4.0.0 release is on
Maven Central; merging later changes does not publish another artifact.

## Existing 3.0 Boolean clients

The public Boolean API keeps its class names, packages and signatures:
`Automaton`, `State`, `Transition`, `Interval`, `IntervalSet`, `Operation`,
`CompiledDfa`, `Searcher` and the public regular-expression types stay in place.
Existing imports such as `fr.menana.automaton.Automaton` continue to work.
The mutable Boolean representation, algorithms and compiled recognizer retain
their behavior; weighted types use a separate immutable graph representation.

`Alphabet` is now a public immutable interval-partition facade. `IntervalSet`
adds `intervalCount()`, `getIntervalMin(index)`, `getIntervalMax(index)` and
`fromSortedIntervals(minima, maxima, from, to)`. These additions let weighted
algorithms reuse the existing partitioner and read/copy ranges without allocating
one `Interval` per range. The sorted factory copies its input slice, merges
adjacent ranges and rejects inverted, overlapping or unordered ranges. It
retains no caller array. Existing set operations and storage layouts are unchanged.

## Weighted development snapshots

Weighted classes first added on the development branch under
`fr.menana.automaton` move for 4.0. Update imports and fully qualified class names
when moving from those snapshots. Recompile code using that development API;
its old weighted class names do not have compatibility aliases.

| Package | Public types |
|---|---|
| `fr.menana.automaton` | Existing Boolean API; public `Alphabet` partitioner |
| `fr.menana.automaton.regexp` | Existing regular-expression API |
| `fr.menana.automaton.weighted` | `AutomatonGraph`, `WeightedAutomaton`, `MultiWeightedAutomaton`, `WeightedDfaIndex`, `LayeredAutomaton` |
| `fr.menana.automaton.weighted` | `WeightedOperations`, `MultiWeightedOperations`, `WeightedPaths`, `MultiWeightedPaths`, `WeightedTransformations`, `KBestPaths`, `KBestWords` |
| `fr.menana.automaton.weighted` | `WeightedPath`, `MultiWeightedPath`, `WeightedWord`, `ShortestPathResult`, `ParetoResult` and their public nested types |
| `fr.menana.automaton.weighted.compiled` | `CompiledWeightedDfa`, `CompiledMultiWeightedDfa` |
| `fr.menana.automaton.weighted.internal` | Unsupported implementation helpers, excluded from the public API reference and Javadoc |

For example:

```java
import fr.menana.automaton.Interval;
import fr.menana.automaton.weighted.WeightedAutomaton;
import fr.menana.automaton.weighted.MultiWeightedAutomaton;
import fr.menana.automaton.weighted.WeightedDfaIndex;
import fr.menana.automaton.weighted.compiled.CompiledWeightedDfa;
import fr.menana.automaton.weighted.compiled.CompiledMultiWeightedDfa;
```

Internal graph-search helpers remain beside the weighted graph, allowing primitive
CSR arrays to stay package-private. Writable internal arrays are not exposed to
create extra subpackages. Tests follow the weighted package organization.

## Sharing a compiled index across weights

The refactoring extracts `WeightedDfaIndex`, which stores the immutable graph and
interval-to-logical-edge lookup arrays, independently of all scalar/vector costs.
`compile()` still builds an index for convenience. The explicit factories allow
independently constructed scalar/vector evaluators to share one index too:

```java
// cost and duration are weightings of the identical deterministic epsilon-free graph.
WeightedDfaIndex index = WeightedDfaIndex.of(cost.graph());
CompiledWeightedDfa scalar = CompiledWeightedDfa.of(cost, index);
MultiWeightedAutomaton multi = MultiWeightedAutomaton.fromWeightings(cost, duration);
CompiledMultiWeightedDfa vectors = CompiledMultiWeightedDfa.of(multi, index);
assert scalar.index() == vectors.index();
assert vectors.project(0).index() == index;
assert vectors.scalarize(1, 2).index() == index;
```

Both compiled evaluators support `reweight`, reusing the same index and requiring
identity of the immutable graph object. Vector reweighting may change dimensions.
The index retains no previous cost store. Sharing a scalarization still retains
the vector costs it reads, as required by that view's semantics. Explicit factories
reject a mismatched graph even if it has the same shape or accepts the same language.

Building an index has the same sorting/validation work as the previous scalar
compiler. Sharing/reweighting adds a small evaluator object without rebuilding
lookup arrays. Scalarization may additionally copy its coefficient vector. The
binary interval lookup and checked IEEE addition contracts are unchanged. Primitive
buffered vector recognition and scalar recognition with an existing input array
allocate no work/result arrays per call.

## Algorithm contracts

This release's path, numeric and Pareto semantics are described in the
[contracts guide](CONTRACTS_AND_PERFORMANCE.md). Package organization does not
expand algorithm domains: general cyclic Pareto propagation is still unsupported,
residual determinization still has an explicit construction budget, and exact
normalization rejects emitted weights that cannot be represented exactly as doubles.
See [advanced algorithms](ADVANCED_ALGORITHMS.md) for those restrictions.

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
