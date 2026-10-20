# Contracts, failures and performance

## Numeric contract

Input scalar/vector weights, boundaries and coefficients must be finite. NaN and
both infinities fail with `IllegalArgumentException`. Result `+Infinity` means
absence; result `-Infinity` denotes a relevant unbounded infimum. Finite overflow
throws `ArithmeticException`. `Double.MAX_VALUE` is never an absence sentinel.
Negative zero is normalized in validated cost storage.

Fast scalar word/DAG/Dijkstra/DP calculations and compiled scalar/vector sums use
checked IEEE double additions. They can round, reorder near ties, or differ under
reversal and regrouping. Equal numeric literals do not justify approximate state
merging; no epsilon tolerance is applied to exact transform invariants.

Bellman–Ford certifies safe primitive sums or uses exact binary-input arithmetic,
including cycle signs. Pareto, lexicographic and DAG K comparisons similarly use
an exact fallback outside their certificate. Cyclic/lazy ranking and residual/
cyclic minimization use exact `BigDecimal(double)` sums internally. They operate
on the values actually represented by input doubles, not their ideal decimal
spellings. Output costs are rounded to doubles; `MultiWeightedPath.exactCost`
can expose a vector component's exact sum. A fast `cost(word)` can differ from
an exact-ranked result under severe cancellation. Exact transformation output
weights must themselves be exactly representable as finite doubles, or fail.

## Ownership and concurrency

| Type | Ownership / concurrent use |
|---|---|
| `Automaton`, `State` collections, `IntervalSet` | Mutable; external synchronization required for concurrent edits |
| Graph/weighted builders | Mutable; do not share concurrent construction |
| `AutomatonGraph`, scalar/vector automata | Immutable snapshots/views; shareable when callers respect ownership |
| `Alphabet`, `WeightedDfaIndex`, `CompiledDfa`, `Searcher`, compiled scalar/vector DFAs | Immutable; shareable |
| Evaluators, lazy iterators | Reusable mutable work state; one per thread |
| Caller output arrays and input arrays | Caller-owned; no concurrent writes while being evaluated |

Weight arrays, labels and dimension/operand/coefficient maps are copied on entry
where documented. Views share immutable stores, not mutable builder internals.
Paths/results return defensive arrays or immutable lists. Source transition
references in import metadata are not immutable copies of the source objects;
the graph itself is independent. Graph identity is an intentional requirement
for ID-preserving reweighting and layered/compiled reuse.

## Algorithm selection and complexity

Let V/E be states/logical arcs, I interval segments, L a word length, T a horizon,
d dimensions and C alphabet classes. Exact-number bit costs are additional.

| Task | Preconditions / structural cost |
|---|---|
| Boolean DFA run | O(L × outgoing arcs × label lookup cost) |
| Boolean NFA run | Set simulation/epsilon closure; depends on active states and arcs, not just V |
| Boolean determinization | Exponential worst case |
| Boolean default minimization | O(m log V) for the refinement stage, with m class-expanded transitions |
| Compiled recognition | O(L log I), plus d additions per symbol for direct vectors |
| Scalar DFA/NFA word cost | Direct path or state DP with weighted epsilon closure |
| DAG shortest/longest distances | O(V+E), signed weights allowed |
| Dijkstra | Nonnegative edges; indexed primitive heap in ordinary scalar paths |
| Bellman–Ford | O(VE) structural relaxations, negative-cycle detection |
| Fixed-length DP | O(T(V+E)) without costly epsilon closure; two distance layers for cost-only |
| Explicit horizon structure | O((T+1)V) scratch, up to O(TE) retained arcs |
| Scalarization view | O(d) setup, O(d) computed edge weight; no graph/weight-table copy |
| Pareto propagation | DAG; potentially exponential labels/front/output |
| Residual determinization | Potentially exponential or unbounded residues; explicit state budget |
| Cyclic minimization | Simple fixed-point refinement, worst O(V(V+E+I log I)) |
| Lazy best-first search | Search/output dependent; each queue operation O(log Q), no specialized sidetrack bound |

Prefer cost-only queries when no witness is needed. Reuse evaluators, compiled
indexes, analyses and layered structures; rebuilding a layered graph for new
weights is unnecessary. Scalarization materialization trades O(E) scalar storage
for cheaper repeated edge access. Negative scalarization coefficients can require
Bellman–Ford. Never prune vector paths based on one scalarization when later
coefficients may change.

## Errors and empty results

Invalid dimensions, bounds, negative K/lengths, algorithm-family violations and
nonfinite inputs fail explicitly where documented. Missing paths/words generally
use null or empty collections; scalar missing cost uses +infinity. Unbounded
reconstruction throws rather than inventing a witness. Compiled vector buffered
rejection returns false and fills +infinity; its allocating convenience returns
null. A budget exception never returns a complete-looking partial front/DFA.
Legacy Boolean slice/mutation APIs do not uniformly validate every misuse; obey
the documented preconditions instead of depending on incidental exceptions.

## Measurement and build tooling

Use `mvn clean verify` for tests/packaging and the optional standalone
[benchmark module](../benchmarks/README.md) for JMH/GC measurements. The source
POM prepares 4.0.0, while original Boolean algorithm bodies remain unchanged.
The [4.0 refactoring audit](../audit/release-4/README.md) records before/after
measurements, bytecode/API checks, source hashes and memory methodology.
The [previous algorithm audit](../audit/weighted-advanced-cyclic/README.md)
records the weighted algorithm fixtures and numerical restrictions.
These are preliminary shared-host measurements, not universal speed claims.
Original baseline reports describe their own historical snapshots.

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
