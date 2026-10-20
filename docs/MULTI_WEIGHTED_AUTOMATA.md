# Multi-weighted automata

Since **4.1**, start from `automaton.multiWeighted(callbacks...)`
or reuse `automaton.snapshot()` across scalar/vector weightings. See
[Automaton and weights](AUTOMATON_AND_WEIGHTS.md). The APIs below also remain
available for direct construction and independent graph composition.


A `MultiWeightedAutomaton` has a fixed positive dimension. Each path adds vectors
component by component, including initial/final vectors. On an NFA a word denotes
**a set of attainable vectors**, not an independent minimum for each dimension.
Operations keep dominated vectors unless dominance filtering is explicitly asked.

<!-- example: vectors -->
```java
WeightedAutomaton.Builder builder = WeightedAutomaton.builder();
int start = builder.addState(), left = builder.addState(), right = builder.addState();
builder.setInitial(start); builder.setAccept(left); builder.setAccept(right);
builder.addTransition(start, left, 0, 5);
builder.addTransition(start, right, 1, 10);
WeightedAutomaton cost = builder.build();
WeightedAutomaton duration = cost.withWeights(new double[]{6, 1});
MultiWeightedAutomaton multi = MultiWeightedAutomaton.fromWeightings(cost, duration);
check(multi.graph() == cost.graph());
check(multi.paretoFront().size() == 2);
check(Arrays.equals(new int[]{0}, multi.lexicographicBestPath().word()));
check(Arrays.equals(new int[]{1}, multi.scalarize(1, 2).bestPath().word()));
WeightedDfaIndex index = WeightedDfaIndex.of(multi.graph());
CompiledMultiWeightedDfa compiled = CompiledMultiWeightedDfa.of(multi, index);
check(compiled.project(0).index() == index);
check(CompiledWeightedDfa.of(cost, index).index() == index);
double[] output = new double[2];
check(compiled.costs(new int[]{1}, output));
check(Arrays.equals(new double[]{10, 1}, output));
equal(12, compiled.scalarize(1, 2).cost(1));
```

## Construction and views

`builder(d)` accepts exactly d costs for each edge and boundary; omitted boundary
vectors mean zero. Inputs are defensively copied. `of(graph,d,weights,initial,finals)`
uses flat edge-major and state-major arrays: component `(edge,j)` is at
`edge*d+j`. `weight`, `initialWeight`, `finalWeight`, `weights` and `dimensions`
provide component/copy access. `pathCosts(scalarPath)` evaluates the very same
accepting logical path on the shared graph, with exact fallback for unsafe sums.

`project(j)` creates a scalar view with O(1) component access. `selectDimensions`
retains/reorders/duplicates selected dimensions with a copied index map.
`scalarize(coefficients)` copies O(d) coefficients and shares graph/vector storage;
it computes weights on demand. `scalarizeMaterialized` builds scalar cost arrays
when repeated calculations justify them, still sharing the graph.

Coefficients may be negative, so a scalarization can invalidate nonnegative-edge
assumptions. Select shortest-path algorithms from the resulting weights. Positive
scalarizations alone need not recover every Pareto point; do not claim an exact
front by sampling several coefficient combinations.

## Correlated weights versus independent products

<!-- example: correlation -->
```java
WeightedAutomaton.Builder builder = WeightedAutomaton.builder();
int start = builder.addState(), end = builder.addState();
builder.setInitial(start); builder.setAccept(end);
builder.addTransition(start, end, 7, 0);
builder.addTransition(start, end, 7, 10);
WeightedAutomaton a = builder.build(), b = a.withWeights(new double[]{10, 0});
MultiWeightedAutomaton correlated = MultiWeightedAutomaton.fromWeightings(a, b);
MultiWeightedAutomaton independent = MultiWeightedAutomaton.synchronizedProduct(a, b);
equal(10, correlated.scalarize(1, 1).cost(7));
equal(0, independent.scalarize(1, 1).cost(7));
check(correlated.paretoForWord(7).size() == 2);
```

`fromWeightings(a,b,...)` requires identity of the graph objects. It copies only
the operand-reference array. Each vector belongs to one common logical path.
For the example, available vectors are `(0,10)` and `(10,0)`; `(0,0)` is not a
possible common-path vector.

`synchronizedProduct(a,b,...)` accepts different structures and keeps all
combinations of independently chosen paths that read the same word. Its dimensions
correspond to operands. Here independently choosing the cheaper path in each
operand makes `(0,0)` possible. Labels are intersected symbolically, epsilon
interleavings are filtered canonically, and all vectors/path combinations survive.
The graph can grow as a product. A one-operand call is a one-dimensional view.

## Lexicographic and Pareto optimization

`lexicographicBestPath()` uses dimensions in natural order; pass a permutation
of all dimensions for another priority. It compares vectors directly, without
simulating priorities through huge coefficients. It currently requires a DAG.
No accepted path gives null.

For minimization, x strictly dominates y when every `x[j] <= y[j]` and at least
one inequality is strict. Equal vectors do not dominate each other.
`MultiWeightedPaths.dominates(x,y)` validates finite equal-size vectors.

`paretoFront()` gives one representative per nondominated accepting vector.
`paretoFront(true)` keeps all equal-vector **logical paths**, explicitly, on a
DAG. It does not enumerate every concrete symbol assignment of an interval path.
`ParetoResult.paths()` is immutable; `includesAllPaths` records the equality mode;
`isComplete()` is true because no implicit approximation or truncation is used.
The number of labels and paths can be exponential.

`MultiWeightedPath` exposes vector components, logical edges and concrete witness
word. Exact unsafe-sum comparisons use binary-input `BigDecimal` values;
`exactCost(j)` exposes an unrounded component when output doubles look equal.

`paretoForWord(word)` uses finite layers and computes that word's front. This
supports consuming cycles, but useful epsilon cycles still prevent a DAG.
Use a layered vector view for fixed-horizon lexicography/Pareto. General cyclic
vector fronts, epsilon-dominance approximations and capped fronts are not provided.

## Algebra and compiled evaluation

`union`, `concatenate`, `intersection`, `revert` and `simplify` preserve the full
word/vector relation. Equal-dimension synchronized intersection adds matching
components. `MultiWeightedOperations.productIndependent(a,b)` instead appends
independent resource blocks, with dimension da+db. `concatenateIndependent` does
the analogous block composition for successive fragments. These choices encode
different resource semantics and are not interchangeable.

`compile()` creates `CompiledMultiWeightedDfa` for a deterministic epsilon-free
graph. `costs(word)` returns the unique path's vector or null. The caller-buffer
`costs(word,out)` and slice overload allocate no result/work buffer, return a
Boolean, and fill all components with `+Infinity` on rejection. Buffer length
must equal the compiled dimension. Overflow throws; buffer contents after an
arithmetic exception are unspecified.

`compiled.project(j)` and `scalarize(coefficients)` return scalar compiled views
sharing the exact interval index. `compiled.reweight(other)` also reuses it when
the graph object is identical; the new vector dimension may differ. The old
compiled instance remains unchanged. NFA compilation is rejected explicitly;
Boolean determinization would lose the vector relation.

`WeightedDfaIndex.of(graph)` creates the weight-independent index explicitly.
The scalar/vector `of(automaton,index)` factories require the identical graph,
and `index()` exposes the immutable shared index. This allows unrelated cost
stores on the same graph to reuse lookup arrays without first compiling one
of them; see the [migration guide](MIGRATION_4.md#sharing-a-compiled-index-across-weights).

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
