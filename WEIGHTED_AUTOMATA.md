# Weighted and multi-weighted automata

Automaton **4.0.0** adds immutable weighted automata alongside the mutable
Boolean API retained from 3.0. There are no new runtime dependencies; Java 17
is required. The existing Boolean algorithms and object layouts are unchanged.
Version 4.0.0 is published; the current POM develops 4.1.0-SNAPSHOT.
The [direct Automaton/weights bridge](docs/AUTOMATON_AND_WEIGHTS.md) is new in 4.1.
Weighted types now live in `fr.menana.automaton.weighted`, with compiled
evaluators in `weighted.compiled`. See the [migration guide](docs/MIGRATION_4.md).

## Scalar automata

```java
import fr.menana.automaton.*;
import fr.menana.automaton.weighted.*;
import fr.menana.automaton.weighted.compiled.*;

var builder = WeightedAutomaton.builder();
int start = builder.addState();
int end = builder.addState();
builder.setInitial(start, 1.0);  // initial boundary, zero by default
builder.setAccept(end, 2.0);    // terminal boundary, zero by default
builder.addTransition(start, end, new Interval(0, 9), 3.5);
builder.addTransition(start, end, 4, 1.0); // a separate parallel edge
WeightedAutomaton a = builder.build();

a.cost(4);                    // 4.0 = 1 + 1 + 2
a.cost(7);                    // 6.5 = 1 + 3.5 + 2
a.cost(12);                   // +Infinity: not recognized
a.bestPath(4).edges();         // identifies the cheaper parallel edge
a.bestPath(4).word();          // [4]
```

A path is a sequence of logical edge IDs. Its cost includes the initial
boundary, every traversed edge and the terminal boundary. The cost of a word
is the infimum over its accepting paths; it is not the cost of an arbitrary
recognizing path. Epsilon transitions consume no symbol and can have independent
costs. A relevant negative epsilon cycle can make a word cost `-Infinity`.

`optimalCost()` and zero-argument `bestPath()` optimize over **all** accepting
paths. `bestPath(int... word)` optimizes for a specified word. To request a
witness for the empty word, use `bestPath(new int[0])`. Unrecognized queries
return `+Infinity` for costs and `null` for paths. An unbounded infimum has no
best finite path: reconstruction throws `IllegalStateException`.

`WeightedPath` stores both edge IDs and a concrete word. For unrestricted path
searches, an interval-labelled edge uses its least symbol as a representative.
Different symbols on the same logical edge are not counted as distinct paths.
Arrays returned by public accessors are defensive copies.

## Share structure between weightings

```java
var builder = AutomatonGraph.builder();
int q0 = builder.addState(), q1 = builder.addState();
builder.setInitial(q0);
builder.setAccept(q1);
int edge = builder.addTransition(q0, q1, new Interval(0, 9));
AutomatonGraph graph = builder.build();

WeightedAutomaton first = WeightedAutomaton.of(graph, new double[]{3});
WeightedAutomaton second = first.withWeights(new double[]{-2});
assert first.graph() == second.graph();
```

The graph stores primitive CSR indexes in both directions and flattened interval
bounds. Edge IDs follow insertion order even when CSR changes their traversal
order. Builders can be reused: each `build()` creates an independent snapshot.
Initial/final flags belong to the graph; their costs belong to its weighting.
Graph IDs remain stable throughout its lifetime. Operations producing a new
graph have new local IDs.

`AutomatonGraph.from(existing)` snapshots a 3.0 automaton without modifying,
determinizing or minimizing it. A 3.0 transition carrying both epsilon and
symbols becomes two logical edges. `importAutomaton` also returns the mapping
to the original transitions; the mode is available on the imported graph.
Already-merged parallel transitions cannot be recovered. For a simple import:

```java
WeightedAutomaton weighted = WeightedAutomaton.from(existing, transition -> 1.0);
```

That callback assigns the same cost to both modes of a mixed transition. Use
the explicit import mapping to assign different costs. Mutating the original
automaton, input labels or input arrays never changes the snapshot.

Immutable graphs, weightings and results may be shared between threads.
Builders and reusable evaluators belong to one thread.

## Paths, distances and finite horizons

```java
ShortestPathResult result = WeightedPaths.shortestPaths(a);
double distance = result.distanceTo(end); // initial included; terminal excluded
double optimum = result.optimalCost();   // terminal included
WeightedPath witness = result.bestPath();

var analysis = WeightedPaths.analyze(a);
double viaState = analysis.costThroughState(end);
double viaEdge = analysis.costThroughTransition(0);
double[] suffixCosts = analysis.backward(); // includes terminal costs

double exactLength = a.costOfLength(4);
double upToLength = a.costOfLengthAtMost(4);
WeightedPath pathOfLength = a.bestPathOfLength(4);
double underPrefix = a.optimalCostWithPrefix(1, 2);
```

Automatic selection uses a topological pass on DAGs, Dijkstra for nonnegative
cyclic graphs, and Bellman–Ford otherwise. Explicit methods are also available.
`shortestPaths(a, source)` starts at zero and ignores the initial boundary;
`distancesTo(a, destination)` has a zero destination boundary.
`dagLongestPaths` supports signed weights and rejects cycles.

`ShortestPathResult.Status` distinguishes `FINITE`, `UNREACHABLE` and
`UNBOUNDED`. A negative cycle matters only for vertices reachable from that
cycle and from the query's source. The accepting optimum ignores cycles that
cannot lead to acceptance. For fixed words, a negative epsilon branch must
still consume the **exact remaining suffix** to affect its optimum.

Pass `false` to `shortestPaths(a, false)` to omit predecessor storage.
`a.evaluator()` reuses NFA cost buffers without constructing paths. Epsilon-free
evaluation and nonnegative epsilon closures reuse primitive buffers; signed
epsilon closures can allocate for the exact fallback.

| Algorithm | Arithmetic-operation time | Additional working space |
|---|---|---|
| DAG min/max, forward/backward | O(V+E) | O(V) |
| Dijkstra, indexed primitive heap | O((V+E) log V) | O(V) |
| Bellman–Ford | O(VE) | O(V), plus exact edge values when required |
| Word NFA without epsilon | O(L(V+E log Imax)) | O(V), without a witness |
| Exact/maximum length without epsilon | O(T(V+E)) | O(V), without a witness |
| Conditional state/edge after analysis | O(1) | previously computed distances |

Here `Imax` is the maximum number of intervals on an edge. Epsilon closures
add the corresponding shortest-path work at every word/horizon position.
Exact arithmetic also has bit complexity and allocation costs.

## Reuse an unfolded graph

```java
LayeredAutomaton layers = LayeredAutomaton.exactly(a.graph(), 20);
WeightedAutomaton first = layers.evaluate(a);
WeightedAutomaton second = layers.evaluate(a.withWeights(newCosts));
assert first.graph() == second.graph();

var analysis = layers.analyze(a, (position, edgeId) -> costs[position][edgeId]);
double conditional = analysis.costAt(3, 7);
WeightedPath originalGraphPath = analysis.bestPath();
```

Only accessible and coaccessible `(position, state)` vertices become graph
nodes. `atMost` allows acceptance at every layer; `forWord` restricts each
position to one specified symbol. Epsilon edges stay in the same layer, so
the deployed graph may still have cycles. The horizon counts consumed symbols.

Changing weights reuses the exact deployed structure and its mappings.
Position-dependent callbacks are snapshotted into immutable weights; callbacks
do not become mutable dependencies of a result. Epsilon costs may be requested
at position `T`. An edge cost is constant across all its symbols; costs varying
inside an interval require splitting the label into constant-cost pieces.
`costOfLength` uses implicit two-layer DP when no explicit deployed graph is needed.

## Operations

`union`, `concatenate`, `intersection`, `revert`, `star`, `trim`,
`WeightedOperations.repeat(a, min, max)` and `WeightedOperations.simplify(a)`
leave operands unchanged. Use `max == -1` for unbounded repetition.

Union takes the minimum over branches. Concatenation considers every word
split and pays both operands' boundaries. Intersection synchronizes consumed
symbols, intersects interval sets without enumerating integers and adds costs.
An epsilon filter chooses a canonical left-before-right interleaving between
consumed symbols, avoiding duplicate representations of operand-path pairs.
Reversion swaps boundaries through a fresh epsilon-connected initial state.
Star can introduce an improving empty-word cycle.

`trim` preserves accepting paths. `simplify` additionally unions labels having
identical endpoints, mode and **exactly equal** costs. It preserves word costs,
but can change edge IDs and path multiplicity. No weighted complement or
difference is defined. `removeEpsilon` preserves minimum word costs, not all
paths; relevant negative epsilon cycles are rejected. Its output can grow large.

## Multi-weighted automata

```java
var builder = MultiWeightedAutomaton.builder(3);
int start = builder.addState(), end = builder.addState();
builder.setInitial(start);
builder.setAccept(end);
builder.addTransition(start, end, 7, 12.0, 3.0, 0.5);
builder.addTransition(start, end, 7, 8.0, 6.0, 1.0);
MultiWeightedAutomaton multi = builder.build();

WeightedAutomaton scalar = multi.scalarize(0.5, 2.0, 1.0);
assert scalar.graph() == multi.graph();
WeightedAutomaton firstDimension = multi.project(0);
MultiWeightedAutomaton reordered = multi.selectDimensions(2, 0);
MultiWeightedPath lexicographic = multi.lexicographicBestPath();
ParetoResult front = multi.paretoFront();
ParetoResult forWord = multi.paretoForWord(7);
```

Dimensions are fixed and checked; flattened `of` arrays are edge-major and
terminal arrays state-major. Initial/final vectors default to zero. A word in
a NFA can have several vectors. Vector operations retain the **full relation**,
including dominated vectors, until a Pareto query is explicitly requested.

`scalarize` copies only the coefficients, O(d), and evaluates weights on demand,
O(d) per access. `scalarizeMaterialized` caches scalar edge/boundary costs while
sharing the graph, O((V+E)d) work and O(V+E) scalar storage. Negative coefficients
can invalidate Dijkstra. Even nonnegative coefficients do not guarantee
nonnegative scalar weights if vector components are signed.

Lexicographic priority is a permutation of all dimensions; omission means
natural order. It never uses artificially large coefficients. Lexicographic
and Pareto global queries require a DAG. Fixed-word Pareto uses a layered
graph and therefore also supports cyclic consuming graphs, provided there
is no useful epsilon cycle.

Strict dominance means every component is no larger and at least one is
smaller. By default, one path per equal nondominated vector is retained.
`paretoFront(true)` explicitly returns all paths associated with nondominated
vectors on the DAG; the result can be exponentially large. Results never
silently truncate or approximate. No epsilon-dominance, front-size cap or
time-budget heuristic is currently implemented.

Fronts use compact lists; sophisticated 2D/3D indexes are not imposed without
evidence of a benefit. With A generated labels and maximum front M, dominance
work is O(d A M), and M itself can be exponential. Ancestors needed for witnesses
also consume memory. Linear scalarization need not discover every Pareto point.

`union`, `concatenate`, `intersection`, `revert`, `trim` and `simplify` preserve
the full word/vector relation. Addition requires matching dimensions and
resource semantics. For independent resources:

```java
MultiWeightedAutomaton product = MultiWeightedOperations.productIndependent(left, right);
MultiWeightedAutomaton sequence = MultiWeightedOperations.concatenateIndependent(left, right);
```

These produce dimension `left.dimensions() + right.dimensions()` and retain
separate resource blocks, padding absent blocks with zeros. They differ from
componentwise addition. Simplification does not discard dominated vectors;
merging equal-vector labels can change path multiplicity.

## Numerical contracts

All stored weights, boundaries, components and coefficients must be finite.
`NaN`, `+Infinity` and `-Infinity` inputs are rejected. Zero signs are normalized.
Results use `+Infinity` for absence and `-Infinity` for an unbounded infimum;
overflow in finite cost arithmetic throws `ArithmeticException`.

Fast scalar DAG/Dijkstra/word DP uses IEEE 754 additions: returned finite values
are rounded, and cancellation can affect near-tied choices. Reversal, scalarizing
before/after summation and forward/backward grouping are not promised bitwise
equal for arbitrary doubles. No tolerance is used to merge weights or residuals.

Bellman–Ford certifies a bounded-integer domain for primitive synchronous
relaxation. Otherwise, it adds the exact binary-double input values with
`BigDecimal`, including the sign of cycles. This avoids both missed tiny
negative cycles and spurious negative zero cycles caused by cancellation.
Fixed-word DP can still round between successive closure/symbol stages.

Pareto, lexicographic and K-best comparisons certify exact bounded dyadic sums
first. Otherwise, they use exact `BigDecimal` sums internally. Multiobjective
results expose rounded `double` components and `MultiWeightedPath.exactCost(d)`
for the unrounded binary-input sum. Distinct exact vectors can look equal after
rounding. Exact fallback costs more memory and time; it is not a runtime dependency.

## Advanced operations and compilation

`determinize()` and `determinize(maxStates)` use exact residual subsets. The
bounded-integer DAG fast path is retained. Other inputs, including cyclic NFAs
and epsilon cycles of nonnegative total cost, use exact binary-input sums and
exact residual equality. The default limit is 10,000 output states. A budget
exception means no complete result was produced; it does not prove that an
equivalent finite DFA cannot exist. Already deterministic inputs are returned
unchanged (the construction budget does not limit that existing graph).
Relevant negative epsilon cycles are rejected. Every emitted transition and
boundary must be exactly representable as a finite `double`; otherwise an
`ArithmeticException` is raised instead of rounding a transformation silently.
`determinizeDag` retains its explicitly restricted, primitive implementation.

`minimize()` accepts deterministic epsilon-free cyclic graphs, including graphs
with negative consuming cycles. It chooses a canonical accepted suffix by
shortest length then smallest word, normalizes weights using its exact cost,
and refines weighted interval signatures to a fixed point. It minimizes the
number of deterministic states while preserving exact word-cost sums. Output
weights must again be exactly representable as doubles. This does not require a
finite minimum-cost accepting walk. `minimizeDag` retains the bounded-integer
DAG fast path and its conservative bound `abs(weight) <= 2^48/(V+1)^2`.

`kBestPaths(k)` returns logical-edge accepting walks, allowing repeated vertices
and retaining equal-cost walks. DAGs use the original suffix-list algorithm;
cyclic graphs use an exact-potential best-first search. `pathsByCost()` exposes
the lazy iterator. A useful negative cycle is rejected before enumeration;
unreachable cycles and cycles without an accepting continuation are ignored.
Zero-cost cycles yield infinitely many equal-cost walks, fairly ordered by
number of edges. Such an infinite tie can prevent any higher-cost walk from
ever appearing. Labels provide one representative symbol per logical edge,
so these are not distinct-word queries. `KBestPaths.ofLength(a, length, k)`
counts consumed symbols and now permits useful nonnegative epsilon cycles.

`kBestWords(k)` and `wordsByCost()` return `WeightedWord` results: a concrete
`int[]` word and its minimum accepting cost. For an NFA, each word prefix carries
exact best costs per state, with weighted epsilon closure. Only prefixes needed
by the search are expanded; a complete DFA is never required. This works even
when explicit residual determinization would exhaust its state budget. NFA
ambiguity and zero-cost epsilon cycles cannot duplicate a word. Deterministic
inputs use a direct path/cursor fast path without per-state vectors.
Interval symbols are generated lazily, even for the full signed-int range.
`KBestWords.ofLength(a, length, k)` supports signed consuming cycles through
finite unfolding, including zero/positive epsilon cycles. Useful negative
cycles are rejected before unrestricted enumeration; at a fixed horizon only
negative epsilon cycles can remain. Equal-cost words are ordered by consumed
length without a lexicographic promise. Search/frontier size can be exponential.
Cost ordering and transformations use exact binary-input sums; reported costs
are rounded once and can differ from the fast IEEE scalar `cost` in cancellation
cases. Input automata are never modified.

```java
Iterator<WeightedPath> walks = scalar.pathsByCost();
WeightedPath first = walks.next();
List<WeightedWord> words = scalar.kBestWords(10);
WeightedAutomaton deterministic = scalar.determinize(20_000);
WeightedAutomaton minimal = deterministic.minimize();
```

The [advanced extension report](audit/weighted-advanced-cyclic/README.md)
gives the invariants, costs, validation and remaining restrictions. Exact
arithmetic and frontier growth can be expensive; this implementation does not
claim specialized sidetrack-algorithm bounds or a general determinization test.

`compile()` creates `CompiledWeightedDfa`, rejecting nondeterminism and epsilon
edges. It does not silently determinize. The index performs binary interval
searches and can be reused with `compiled.reweight(newWeighting)` when the exact
graph object is shared. Existing-array recognition allocates nothing after setup.

## Compiled vector recognition and combining scalar automata

```java
// Same immutable graph: one correlated vector for each common logical path.
MultiWeightedAutomaton multi = MultiWeightedAutomaton.fromWeightings(cost, duration);
CompiledMultiWeightedDfa compiled = multi.compile();
double[] vector = compiled.costs(1, 2, 3); // null if the word is absent

double[] output = new double[multi.dimensions()];
boolean accepted = compiled.costs(existingWord, output); // no allocation
CompiledWeightedDfa score = compiled.scalarize(0.5, 2.0); // same interval index
CompiledMultiWeightedDfa next = compiled.reweight(newVectorsOnSameGraph);

// Independent paths, possibly different structures: synchronize common words.
MultiWeightedAutomaton product = MultiWeightedAutomaton.synchronizedProduct(a, b, c);
```

`fromWeightings` requires at least one scalar operand and identity of the graph
object. It copies only the operand-reference array: O(d) setup/storage, no graph
or weight-table copy. Initial and final costs become vector components too.
On an NFA, all components describe **one common path**. If two parallel paths
have vectors `(0,10)` and `(10,0)`, their componentwise minima `(0,0)` do not
form an available vector; their sum scalarization has minimum 10.

`synchronizedProduct` instead retains every combination of operand paths for
the same word, with one dimension per scalar automaton. Applied to those same
two scalar weightings it can contain `(0,0)` from independently chosen paths.
It uses symbolic interval intersections and the canonical epsilon filter; its
graph can grow as the product of operand sizes. It preserves dominated vectors
and path combinations, not just a Pareto front. This is a full relation; it is
not a routine that synthesizes componentwise minima on a shared path.

`CompiledMultiWeightedDfa` accepts only deterministic epsilon-free graphs, so a
recognized word has one cost vector. An NFA is rejected rather than silently
collapsing its vector relation. The compiled interval index references logical
edge IDs and vector storage; `reweight`, `project` and `scalarize` reuse it.
A reweighting must use the identical graph but may change the dimension count.
The scalar and vector compiled evaluators share `WeightedDfaIndex`, which
contains no costs. Explicit `of(automaton, index)` factories also share an index
between independently created weightings on the same graph. Its interval lookup
loop is unchanged. Buffered evaluation takes O(L(log I + d)) time, where I is the outgoing
interval count. It overwrites the caller buffer and returns false with every
component set to `+Infinity` on rejection. On overflow it throws and buffer
contents are unspecified. The convenience array-returning overload allocates
one output vector and returns null on rejection. Sums follow the compiled
scalar IEEE contract; exact vector path queries retain their separate API.

## Validation

Run `mvn clean verify` under Java 17. Independent exhaustive path/vector oracles,
Floyd–Warshall, cancellation cases and the original 3.0 tests are included.
The isolated JMH and retained-object-memory reports live under
`audit/weighted-extension/`; JMH is not included in the published library JAR.
