# Scalar weighted automata and shared structure

The weighted extension is additive: a path cost is the initial weight plus all
edge weights plus the final weight of its accepting endpoint. The cost of a
word is the infimum over its accepting paths. Rejection is `+Infinity`. A useful
negative epsilon cycle can give `-Infinity` without an optimal finite path.
The global optimum considers all accepted words and walks; consuming negative
cycles can make it unbounded even though each particular word has a finite cost.

<!-- example: scalar -->
```java
WeightedAutomaton.Builder builder = WeightedAutomaton.builder();
int from = builder.addState(), to = builder.addState();
builder.setInitial(from, 0.5);
builder.setAccept(to, 0.25);
builder.addTransition(from, to, new Interval(0, 9), 3.5);
builder.addTransition(from, to, new Interval(0, 9), 1.5);
WeightedAutomaton a = builder.build();
equal(2.25, a.cost(4));
WeightedPath path = a.bestPath(4);
equal(a.cost(4), path.cost());
check(path.edgeCount() == 1 && path.length() == 1);
check(a.bestPath(new int[0]) == null);
equal(2.25, a.bestPath().cost());
WeightedAutomaton.Evaluator evaluator = a.evaluator();
equal(2.25, evaluator.cost(8));
```

## Immutable graph and weightings

`AutomatonGraph.builder()` creates integer state IDs and stable logical edge IDs.
Each edge insertion stays distinct, including parallel edges with identical
labels and different weights. Labels are copied. An edge consumes one symbol
or is epsilon; it never combines both modes. Forward/reverse CSR indexes and
flattened intervals are shared by every weighting of that graph.

`WeightedAutomaton.builder()` offers structure and costs in one construction.
Initial/final costs default to zero. Set them with `setInitial(state,cost)` and
`setAccept(state,cost)`. Builders are mutable; `build()` returns an independent
snapshot without invalidating the builder. Use `setAccept(s,false)` on the graph
builder when managing Boolean acceptance flags; scalar builders' accepting
cost setters do not provide that Boolean overload.

`WeightedAutomaton.of(graph,edges)` copies the edge array in logical-ID order.
The boundary overload additionally takes one initial cost and one final-array
entry per state; final entries of nonaccepting states are ignored semantically.
`withWeights(edges)` copies new edge weights, retains boundaries and shares the
same graph. Graph identity matters for reweighting/compilation/layer reuse.
Equal-looking separately built graphs are not interchangeable indexes.

## Importing Boolean automata

`AutomatonGraph.from(a)` snapshots structure without determinizing it.
`AutomatonGraph.importAutomaton(a)` also gives `originalTransition(edge)` so
weights can be assigned to imported IDs. A mixed Boolean transition is split
into a symbol edge and an epsilon edge, both referring to the same source
transition metadata. `WeightedAutomaton.from(a, transition -> cost)` assigns
the callback's value to both modes. Use the explicit import mapping when the
modes need different weights. Later source edits do not mutate the graph.

## Word cost and witnesses

`cost(int...)` computes cost without reconstructing a path. Deterministic
recognition follows one path; NFA evaluation propagates minimum state costs and
uses **weighted** epsilon closure. Boolean reachability closure alone is not
sufficient. `evaluator()` provides reusable buffers for repeated words and prefix
queries; one evaluator belongs to one thread. Signed epsilon closures can still
allocate exact workspaces.

`bestPath(word)` returns null on rejection and throws on an unbounded optimum.
The no-argument `bestPath()` means the global best accepting path, not the empty
word query. Pass `new int[0]` explicitly to request a witness for epsilon.
`WeightedPath` exposes graph, start/end, edge IDs, consumed word, lengths and cost
through defensive arrays. `edgeCount()` includes epsilon edges; `length()` does
not. Unspecified symbols on a logical interval path use a representative.

All supplied weights must be finite. NaN and both infinities are rejected;
finite overflow throws. Ordinary fast scalar evaluation uses checked IEEE
addition, so cancellation and close ties can round. Exact cycle detection and
advanced ranking have their separately documented contracts. Read
[contracts](CONTRACTS_AND_PERFORMANCE.md) before depending on exact comparisons.

For more API examples, see the [original weighted guide](../WEIGHTED_AUTOMATA.md),
[paths/horizons](PATHS_AND_HORIZONS.md), and [weighted algebra](ADVANCED_ALGORITHMS.md).

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
