# Optimal paths and finite-horizon dynamic programming

## Distances and acceptance boundaries

<!-- example: distances -->
```java
WeightedAutomaton.Builder builder = WeightedAutomaton.builder();
int s = builder.addState(), middle = builder.addState(), end = builder.addState();
builder.setInitial(s, 1);
builder.setAccept(end, 0.5);
int edge = builder.addTransition(s, middle, 1, -2);
builder.addTransition(middle, end, 2, 4);
builder.addTransition(s, end, 3, 5);
WeightedAutomaton a = builder.build();
ShortestPathResult result = WeightedPaths.shortestPaths(a);
equal(3, result.distanceTo(end));
equal(3.5, result.optimalCost());
equal(6.5, WeightedPaths.dagLongestPaths(a).optimalCost());
WeightedPaths.Analysis analysis = WeightedPaths.analyze(a);
equal(3.5, analysis.costThroughState(middle));
equal(3.5, analysis.costThroughTransition(edge));
equal(2, WeightedPaths.shortestPaths(a, s).distanceTo(end));
```

`WeightedPaths.shortestPaths(a)` automatically selects topological DP for a DAG,
Dijkstra for nonnegative edges, and Bellman–Ford for signed cyclic graphs. Its
initial seed includes `a.initialWeight()`. The overload with an explicit source
uses a zero seed and ignores the graph's initial boundary. `distanceTo(s)` does
not include s's final cost; `optimalCost()` and `bestPath()` do.

`ShortestPathResult.statusTo` distinguishes `FINITE`, `UNREACHABLE` and
`UNBOUNDED`. Missing states have `+Infinity`; descendants of a reachable negative
cycle have `-Infinity`. Only cycles that also reach acceptance affect the global
accepting optimum. `pathTo` returns null for an unreachable target and throws
for unbounded cost. `shortestPaths(a,false)` omits predecessors; distance queries
work, but reconstruction throws explicitly. Distance arrays are defensive copies.

`dagShortestPaths/dagLongestPaths` accept signed weights but require the whole
graph to be acyclic. Absence is still exposed as `+Infinity` in the public result,
even for longest paths. General longest-walk optimization is not provided.
`dijkstra(a)` rejects **any** negative edge, including an unreachable one.
`bellmanFord(a)` uses exact fallback arithmetic to certify negative-cycle signs.

`backwardDistances(a)` includes terminal boundaries and excludes the initial
boundary. `distancesTo(a,destination)` uses a zero boundary at the specified
target. `analyze(a)` caches forward/backward results for repeated O(1)
`costThroughState/Transition` queries. Single-query static helpers may recompute
those distances, so prefer an analysis object for many conditional queries.

## Symbol horizons and reusable layers

<!-- example: horizons -->
```java
WeightedAutomaton.Builder builder = WeightedAutomaton.builder();
int state = builder.addState(); builder.setInitial(state); builder.setAccept(state);
builder.addTransition(state, state, 1, 2);
builder.addTransition(state, state, 2, 3);
WeightedAutomaton a = builder.build();
equal(6, a.costOfLength(3));
equal(0, a.costOfLengthAtMost(3));
LayeredAutomaton layers = LayeredAutomaton.exactly(a.graph(), 3);
WeightedAutomaton changed = a.withWeights(new double[]{4, 1});
equal(3, layers.evaluate(changed).optimalCost());
check(layers.evaluate(a).graph() == layers.evaluate(changed).graph());
WeightedAutomaton positioned = layers.evaluate(a, (position, edge) -> a.weight(edge) + (position == 1 ? 1 : 0));
equal(7, positioned.optimalCost());
equal(7, layers.analyze(a).costAt(1, 2));
check(Arrays.equals(new int[]{1, 1, 1}, layers.bestPath(a).word()));
```

`costOfLength(n)` uses exactly n consumed symbols; `costOfLengthAtMost(n)` includes
all lengths 0 through n. `bestPathOfLength/bestWordOfLength` reconstruct witnesses.
Epsilon consumes zero positions. Negative consuming cycles are bounded by a
finite horizon; epsilon cycles can still remain unbounded.

`LayeredAutomaton.exactly(graph,T)` and `atMost(graph,T)` keep reachable and
coaccessible `(position,state)` nodes. `forWord(graph,word)` additionally restricts
the symbol at each position. Construction uses O((T+1)V) work arrays and can
produce O(TE) edges. Use reasonable horizons and check the resulting size.

`evaluate(a)` is a lightweight view over the same layered graph. Repeated
reweightings need not reconstruct it. `evaluate(a, PositionWeights)` snapshots
the callback once per retained edge, passing `(sourcePosition, originalEdgeId)`;
epsilon edges also receive their source position. Initial/final costs come from
the supplied weighting. `analyze` exposes forward/backward distances, best path,
and `costAt(position,symbol)` for a consumed symbol at a zero-based position.

Layered witnesses belong to the layered graph. `map(path)` reconstructs original
edge IDs and preserves the actual word/cost; `layers.bestPath(a)` already returns
an original-graph witness. `originalState`, `position`, and `originalEdge` expose
mappings. Vector `evaluate` and mapping are available too.

Layering does not automatically eliminate epsilon. A useful epsilon cycle remains
a cycle within a position. DAG-only algorithms, including vector Pareto and
lexicographic search, still reject it; scalar shortest paths and K walks/words
handle their documented cyclic cases. `optimalCostWithPrefix(prefix)` constrains
the consumed prefix and optimizes over every accepting continuation, without a
fixed suffix length.

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
