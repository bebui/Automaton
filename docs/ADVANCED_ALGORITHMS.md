# Weighted algebra and advanced algorithms

## Scalar and vector algebra

| Operation | Scalar word-cost semantics |
|---|---|
| Union | Minimum over accepting paths of either operand |
| Concatenation | Minimum over all word splits of the sum of fragment costs |
| Synchronized intersection | Sum of operand costs on their common words, using paired paths |
| Reversal | Cost of the reversed word equals the original cost, subject to numeric summation contracts |
| `star()` | Empty word of cost zero plus every finite concatenation of repetitions |
| `WeightedOperations.repeat(a,min,max)` | Repetitions in the inclusive range; max=-1 means unbounded |
| `trim()` | Remove states outside all accepting paths |
| `simplify()` | Trim and merge identical endpoint/mode/cost labels safely |

Intersection filters epsilon interleavings so path pairs are not duplicated by
arbitrary scheduling. It intersects interval labels without enumerating symbols.
Concatenation pays the left final and right initial boundaries at each junction.
Star/repetition can introduce improving epsilon cycles, especially when an operand
accepts an improving empty word. There is no implicit weighted complement or
difference with an unspecified numeric meaning.

Vector union/concatenation/reversal preserve all attainable vectors. Componentwise
intersection requires equal dimensions; independent-block composition is a distinct
API. Simplification can change edge IDs and path multiplicity while preserving
word costs or the full word/vector relation; do it before retaining witnesses
whose exact IDs must remain meaningful.

## Exact transforms

`removeEpsilon()` preserves scalar minimum word costs, not the set of all paths.
It computes weighted epsilon-prefix distances and adjusted final boundaries.
Relevant negative epsilon cycles are rejected because finite-weight output cannot
represent their unbounded word costs. Output size can grow substantially. This
operation follows the documented scalar rounding contract; exact residual
construction uses its own exact closures.

`determinize(maxStates)` uses residual state-cost subsets, exact epsilon closures,
exact normalization and exact residual equality. Cyclic NFAs are allowed when the
construction finishes within the state budget. The default is 10,000 states.
An exception on exhaustion is not a proof of nondeterminizability; even a function
with a small equivalent DFA can have a growing unoptimized residual construction.
Already deterministic inputs are returned unchanged; the budget limits construction,
not the size of that existing graph. Negative useful epsilon cycles are rejected.

`minimize()` requires a deterministic epsilon-free graph, including cyclic graphs
with negative consuming cycles. It chooses a canonical accepted suffix by shortest
length then smallest word, uses its exact cost as a potential, and refines normalized
weighted interval signatures. It does not require a finite best accepting walk.

Both transforms require every emitted weight/boundary to be exactly representable
as a finite double. Otherwise they throw `ArithmeticException`; no approximate
state merging is used. The explicit `determinizeDag/minimizeDag` methods retain
the original bounded-integer DAG fast path and its certificate
`abs(weight) <= 2^48/(V+1)^2`. Automatic methods reuse that path when applicable.
The correctness invariants and proofs are in the [advanced implementation report](../audit/weighted-advanced-cyclic/README.md).

## K walks and distinct words

<!-- example: advanced -->
```java
WeightedAutomaton.Builder builder = WeightedAutomaton.builder();
int start = builder.addState(), end = builder.addState();
builder.setInitial(start, 0.25); builder.setAccept(end, 0.5);
builder.addEpsilonTransition(start, start, 0);
builder.addTransition(start, end, 7, -0.25);
builder.addTransition(end, end, 8, 0.5);
WeightedAutomaton a = builder.build();
List<WeightedPath> paths = a.kBestPaths(3);
for (WeightedPath path : paths) equal(0.5, path.cost());
Iterator<WeightedWord> words = a.wordsByCost();
check(Arrays.equals(new int[]{7}, words.next().word()));
check(Arrays.equals(new int[]{7, 8}, words.next().word()));
WeightedAutomaton minimal = a.determinize(32).minimize();
equal(1, minimal.cost(7, 8));
equal(1, KBestWords.ofLength(a, 2, 2).get(0).cost());
```

`kBestPaths(k)` / `KBestPaths.accepting(a,k)` return logical-edge accepting walks;
states and arcs may repeat. `pathsByCost()` exposes a lazy iterator. Each interval
edge contributes a representative symbol. Equal-cost paths stay distinct. The
DAG implementation keeps bounded suffix lists; cyclic graphs use exact suffix
potentials and best-first prefix exploration.

`kBestWords(k)` / `KBestWords.accepting(a,k)` return distinct concrete `WeightedWord`
objects ranked by minimum accepting cost. `wordsByCost()` is lazy. An NFA prefix
stores exact best costs per state, with weighted epsilon closure; no complete DFA
is required and ambiguity cannot create duplicate words. Interval symbols are
expanded with lazy cursors, even for the full signed-int domain.

Unrestricted increasing-cost enumeration rejects useful negative cycles. A zero
cycle can give infinitely many equal-cost results. Ties are ordered by edge count
for paths and consumed length for words, with no lexicographic promise. An infinite
lower-cost tie prevents reaching a higher cost in finitely many outputs; the API
states that limit explicitly. Iterator instances belong to one thread.

`KBestPaths.ofLength` and `KBestWords.ofLength` use exact consumed lengths. Signed
consuming cycles are bounded by the horizon; nonnegative epsilon cycles are
supported. Useful negative epsilon cycles remain unbounded and are rejected.
K words and K paths are different queries, as the zero-epsilon-loop example shows.
These APIs enumerate walks, not simple paths without repeated vertices.

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
