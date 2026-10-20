# Cyclic weighted algorithms and compiled vector costs

Base: `43f998a`, the merge of PR #7. Earlier audit directories describe their
own historical snapshots. This report covers the subsequent cyclic algorithms,
compiled vector evaluation, scalar composition, and complete English manual.

## Delivered APIs

- `KBestPaths.iterator` / `WeightedAutomaton.pathsByCost`: accepting walks,
  allowing repeated vertices/edges, with one representative symbol per logical
  interval edge. DAG K-best keeps its existing fast suffix-list implementation.
- `KBestWords` / `kBestWords` / `wordsByCost`: distinct concrete words at their
  minimum accepting cost. Exact state-cost vectors resolve ambiguity per prefix;
  no complete deterministic automaton is required. Interval symbols are lazy.
- `WeightedTransformations.determinize(a,maxStates)`: exact residual construction
  with epsilon closure and an explicit state budget. `minimize(a)`: canonical
  suffix potentials and a fixed-point quotient of a deterministic weighted graph,
  including negative consuming cycles. The bounded-integer DAG fast paths remain.
- `CompiledMultiWeightedDfa`: deterministic vector evaluation, caller-owned output
  buffers, slices, and reweighting/projection/scalarization sharing an interval index.
- `MultiWeightedAutomaton.fromWeightings`: correlated costs for one common path
  on the identical graph; O(d) reference snapshot without copying graph/cost tables.
- `MultiWeightedAutomaton.synchronizedProduct`: independently chosen scalar paths
  on common words, possibly different graphs, retaining all vector combinations.

See the [complete manual](../../docs/README.md), [weighted API guide](../../WEIGHTED_AUTOMATA.md),
and [full signature reference](../../docs/API_REFERENCE.md). No original Boolean
algorithm or root POM was changed; no production dependency was added.

## Enumeration invariants

All advanced search sums use the exact binary values of input doubles. Internal
null distances mean absence, never an accepted input weight. Only states both
reachable and coaccessible participate. Irrelevant negative cycles cannot reject
an otherwise valid enumeration.

On the useful subgraph without a negative cycle, `h(s)` is the exact best accepting
suffix cost, including its final boundary. For every useful arc,
`w(e)+h(target) >= h(source)`. The queue orders prefixes by `g+h` and completed
walks by `g+final`. Every completed walk removed from it is no worse than any
completion still represented by an open prefix. Boundaries enter the same sums.

A reverse BFS over tight arcs finds the fewest edges in a suffix attaining h.
Equal-priority entries use estimated complete edge count, then greater prefix
depth, then insertion order. This avoids exploring an entire prefix level before
finding one witness, and prevents a zero cycle from permanently outranking a
finite equal-cost witness. Equal-cost outputs have increasing edge counts.
Every finite depth has finitely many logical walks, including epsilon walks.

Infinitely many lower-cost walks can prevent a higher-cost walk from appearing
in any finite position of a sorted list. This is explicitly documented; the API
does not promise to exhaust infinite ties. A useful negative cycle is rejected
because there is no first best walk. Prefixes share persistent parents, and
word/edge arrays are reconstructed only for outputs. Iterators are confined to
one thread; abandoning one holds no external resource. `hasNext` is idempotent.

For NFA words, each concrete prefix carries exact minimum costs per state after
weighted epsilon closure. One symbolic alphabet-class extension propagates and
closes that vector. Each word has one prefix-tree node regardless of path
ambiguity. Its terminal cost is the minimum over accepting state costs plus final
boundaries. Its priority is `min_s(prefixCost[s]+h[s])`. A 0–1 BFS over tight arcs
counts symbols in optimal suffixes, giving epsilon edges length zero. The same
cost/length ordering then applies to words. Useless states are excluded from
closures as well, so irrelevant negative epsilon cycles cannot cause failure.

A class initially creates its smallest symbol; a cursor adds subsequent symbols
as needed. Class-equivalent sibling symbols share the same immutable state-cost
vector. The full signed-int range is neither enumerated at initialization nor
incremented outside its bounds. Deterministic graphs use a direct path/cursor
fast path without per-state vectors. A complete residual DFA is never necessary
for word search, even when explicit determinization exhausts its budget.

## Transformation invariants

Residual determinization maintains the best cost at each original state for each
prefix. Exact epsilon closure precedes initial normalization and follows every
symbol. A common minimum is emitted as boundary/edge weight and subtracted from
all finite components. These offsets telescope; terminal costs minimize residual
plus original final cost. Residuals merge only under exact canonical array
equality. Zero/decimal scales are canonicalized before hashing.

The residual sequence can be infinite. `maxStates` fails explicitly without a
partial result and without proving nondeterminizability. Even a dominated branch
can produce growing residuals for a function with a small equivalent DFA. No
complete determinization test or twins-property checker is claimed. Negative
useful epsilon cycles are rejected; negative consuming cycles do not by themselves
block determinization.

For minimization, `p(s)` is the cost of the canonical accepted suffix: shortest
consumed length, then lexicographically smallest word. A reverse BFS finds lengths;
costs follow in increasing length. Cycles, including negative ones, cannot block
this recurrence. Normalized weights are `w+p(target)-p(source)`, finals are
`final-p(state)`, and the initial boundary is `initial+p(initial)`.

Two states with continuation functions differing by a constant have the same
accepted suffix language and hence the same canonical suffix. Their p difference
is that constant, so their normalized functions are equal. In a DFA, this equality
is characterized by equal terminal values and equal symbol/weight transitions to
equivalent continuation classes. Refining those signatures to a fixed point gives
the minimal quotient. Adjacent segments with equal weight/destination are merged
before signature comparison, without alphabet expansion. A plain Boolean quotient
is never used to erase costs.

Primary reference for residual construction and weight normalization:
[Mohri, Weighted Automata Algorithms, sections 6.2–6.4](https://cs.nyu.edu/~mohri/pub/hwa.pdf).
The canonical-suffix choice and reasoning above describe this implementation;
they do not assert universal convergence of cyclic residual construction.

## Numeric domain and limits

Exact arithmetic uses `BigDecimal(double)` for the input's exact binary value,
rather than `BigDecimal.valueOf(double)` for its decimal display. Ranked output
costs are rounded once, with no tolerance in their comparisons. Every emitted
transformed weight/boundary must itself be exactly representable as a finite
double, or `ArithmeticException` is raised. Valid double inputs can have sums or
normalized differences that cannot fit exactly in another double. This restriction
does not apply to direct word enumeration, which emits no transformed edges.

Primitive certified DAG implementations remain active in their supported domain.
Ordinary fast scalar/compiled-vector evaluation retains its checked IEEE summation
contract and can differ from exact final rounding. No simple-path algorithm,
specialized sidetrack bound, general cyclic vector front, or implicit Pareto
approximation is claimed. An infinite lower-cost tie can block higher outputs.

## Structural complexity

Exact-number time/size costs are additional to these structural bounds.

| Operation | Bound / conditions |
|---|---|
| DAG suffix potentials | O(V+E), O(V+E) stored exact input/distances |
| Nonnegative-edge suffix potentials | O((V+E) log(V+E)), queue can contain stale entries |
| Signed cyclic suffix potentials | O(VE), reject useful negative cycles |
| Lazy walk iterator | Arc expansion and O(log Q) queue operations per explored prefix; frontier/search dependent |
| NFA distinct words | O(V) per state-cost vector; each interval class scans E and performs worst-case O(VE) epsilon closure; potentially exponential search |
| Explicit residual determinization | Potentially exponential/infinite; O(RV) residual storage plus output arcs |
| Cyclic minimization | Worst O(V(V+E+I log I)) simple refinement, O(V+E+I) live storage |
| Vector compilation | Shared scalar interval-index layout; O(V+I) storage plus sorting |
| Buffered vector recognition | O(L(log I+d)), caller buffer O(d), no per-call work/result allocation |
| View from scalar weightings | O(d) references; O(1) component access |
| Scalar synchronized product | Accessible product with epsilon filter; interval labels remain symbolic |

## Validation

Java 17 `mvn -B clean verify`: **87 tests passed**, no failure/error/skip, binary,
source and Javadoc packaging successful, no library-build warning. The baseline
had 71 tests; 10 cyclic-advanced and 6 compiled/composition tests were added.

Independent checks include 60 cyclic K-walk fixtures against bounded exhaustive
lists with a cost bound proving completeness, 45 cyclic residual constructions
against exact Floyd–Warshall epsilon closure plus word DP, 80 cyclic deterministic
minimization fixtures with an independent pair-product equivalence/count oracle,
70 signed finite-horizon word fixtures and 40 unrestricted NFA word fixtures with
an independent complete cost bound. Compiled vectors use 65 generated transition
tables and 100 queries each. Twenty-five three-operand products compare complete
vector/path multiplicities against independently enumerated Cartesian products.
Zero/tiny negative/irrelevant cycles, cancellation, full-int intervals, budgets,
nonrepresentable transforms, slices, overflow and defensive ownership are covered.

The 11 executable documentation snippets compile and run successfully. The
[manual](../../docs/README.md) covers Boolean and weighted APIs in English; its
[generated reference](../../docs/API_REFERENCE.md) contains 39 public types,
including public nested types. Browsable Javadoc generation also succeeds.

[Compatibility checks](compatibility.json) confirm 35 original 3.0 class files
are byte-identical, 35 original source/test/POM/CI file hashes are unchanged,
all previous checked public signatures remain, and all 102 tested core class
files match the standalone benchmark build byte for byte.

## Benchmarks and memory

- [Cyclic algorithms](CYCLIC.md): 16 combinations, [raw JSON](cyclic.json), [log](cyclic.log).
- [Compiled vectors and composition](COMPILED_MULTI.md): 24 combinations, [raw JSON](compiled-multi.json), [log](compiled-multi.log).
- [Baseline/current comparisons](REGRESSION.md): 7 combinations per version,
  [baseline JSON](regression-base.json), [current JSON](regression-current.json).
- [Library verification](verify.log), [Javadoc goal](javadoc.log), [documentation examples](documentation-examples.log),
  [benchmark build](benchmarks-build.log), [verification summary](verification.json),
  [environment](environment.json), [source hashes](source-manifest.sha256), [documentation hashes](documentation-manifest.sha256).

All 54 combinations completed with two forks, three 500 ms warmups and five
500 ms measurements per fork, GC profiler enabled. Shared-host timings and wide
errors are preliminary local references. No universal speedup or precise
performance conclusion is inferred from overlapping uncertainty intervals.

Reachable Java object sizes for a deterministic fixture (1,024 states, 4,096
edges, 8 dimensions and 16 scalarizations), counted once per identity:

| Root object set | Bytes |
|---|---:|
| Compiled vector evaluator, including graph and cost arrays | 509,560 |
| Above plus 16 scalarizations sharing its index | 512,208 |
| Above plus 16 separately compiled scalar indexes | 1,365,328 |
| Last evaluator after 100 reweightings | 509,560 |
| Equivalent newly compiled evaluator | 509,560 |

The reweighted object retains no history of previous weights. This instrumentation
counts reachable Java objects, not heap-dominator retention, native memory, JVM
metadata or collector overhead. GC allocation/op is a separate JMH measurement.
The reproducible [memory source](CompiledMultiMemory.java) uses the existing agent:

```bash
mkdir -p /tmp/automaton-multi-memory
javac -cp benchmarks/target/benchmarks.jar -d /tmp/automaton-multi-memory \
  audit/weighted-advanced-cyclic/CompiledMultiMemory.java
java -javaagent:benchmarks/target/benchmarks.jar \
  -cp /tmp/automaton-multi-memory:benchmarks/target/benchmarks.jar audit.CompiledMultiMemory
```

See [memory.log](memory.log) for the raw output.
