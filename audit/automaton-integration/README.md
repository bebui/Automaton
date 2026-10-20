# Connecting Automaton and weighted automata

Baseline: published 4.0.0 sources / merged PR #10, `13d8ba6`. Candidate:
`4.1.0-SNAPSHOT`, not published. See the [implementation plan](../../AUTOMATON_INTEGRATION_PLAN.md)
and [English integration guide](../../docs/AUTOMATON_AND_WEIGHTS.md).

## User-facing connection

Start with the existing `Automaton` construction or regular-expression APIs:
`weighted(callback)`, `multiWeighted(callbacks...)` and `snapshot()` now connect
them to the scalar/vector engine. A reusable root-package `AutomatonSnapshot`
captures one graph and retains original State/Transition identity mappings, which
survive source reindexing. Every weighting created from that snapshot shares the
identical graph, so scalar/vector composition, compiled indexes and horizon
structures can reuse it directly. Separate convenience calls make separate snapshots.

Boundary callbacks capture initial/final costs. Mode-aware callbacks distinguish
consuming and epsilon modes of one legacy mixed transition. The existing
`WeightedAutomaton.from` uses the same bridge; vectors have a matching factory.
Existing weighted builders and package names stay available.

`run(word)` on snapshots/graphs/scalar/vector automata answers Boolean membership
without adding costs. It remains valid when cost evaluation overflows or encounters
a useful negative epsilon cycle. `toAutomaton()` returns a fresh mutable Boolean
projection, explicitly forgetting costs/vectors and merging parallel logical
paths. Editing either source or projection cannot change captured graph/cost arrays.
Weighted transformations are not replaced with Boolean language transformations.

The snapshot retains original objects as mutable identity metadata, not frozen
copies. Callbacks must not race edits to that metadata. Completed scalar/vector
weightings do not retain the snapshot or its original-object mappings. No automatic
snapshot cache or new instance field is added to the mutable Automaton.

## Actual algorithm reuse

Legacy coaccessibility, immutable useful-state analysis and graph Boolean epsilon
closure share `internal.GraphTraversal`. It operates on borrowed primitive CSR
arrays and work buffers, with no boxing or allocation inside the traversal loop.
The legacy adapter constructs reverse adjacency without copying interval labels.
It verifies canonical state indices once, uses direct indices when valid, and
retains an identity-map fallback for reordered/noncanonical source lists.

`getUseFulStates()` preserves its coaccessible-only historical contract. The graph
adds explicit `coaccessibleStates()`; its existing `usefulStates()` still additionally
requires reachability from the initial state. Legacy map queries follow even empty
transition records, while imports retain only consuming/epsilon modes; empty-label
records contribute no path to the recognized language. This edge case is tested
and documented rather than silently changing the old query.

The original mutable recognizer and CompiledDfa remain unchanged. The new Boolean
graph recognizer uses primitive state buffers and the same reachability kernel
for epsilon closure. Snapshot/import and coaccessibility remain linear in graph
size, apart from expected constant-time identity lookups in noncanonical adapters.

## Correctness

Java 17 `mvn -B clean verify`: **98 tests**, zero failures/errors/skips and no
library-build warning; binary/source/Javadoc artifacts generated. Eight integration
tests supplement all 90 previous tests. Seventy generated cyclic multigraphs,
including parallel arcs, epsilon cycles, missing initial states and disconnected
components, are compared with independent Boolean matrix/transitive-closure and
word-DP oracles: 156 words per fixture, checked through five representations.
Coaccessibility/useful-state masks are checked independently too, including
reordered source state lists and legacy empty-label records.

Other checks cover direct regex-to-cost construction, stable source identity
mappings after edits/reindexing, captured boundaries, mixed epsilon-mode costs,
correlated NFA vectors, cost-independent membership under overflow/unboundedness,
parallel-path projection, empty inputs and nonfinite values. The 12 executable
manual examples compile and run. The generated reference covers 43 supported
public types, excluding internal implementation helpers.

[Compatibility checks](compatibility.json) compare every supported published 4.0
public declaration, verify production/benchmark class identity and run the original
11-example 4.0 client bytecode without recompilation against the new library.
The original recognizer body and CompiledDfa/State/Transition sources are checked
unchanged. [Legacy-client output](legacy-client.log).

## Performance and memory

[PERFORMANCE.md](PERFORMANCE.md) retains raw means, reported errors and allocation
measurements for before/after coaccessibility, useful states, scalar import,
Boolean recognition and minimization, plus the new snapshot APIs. The first
always-identity-map adapter is retained as intermediate evidence; the final
verified-index adapter is measured separately. Two forks, three 500 ms warmups,
five 500 ms measurements, JMH 1.37 GC profiler and 128–256 MB heaps are used.
This is a shared host; no universal performance claim follows from noisy timings.
The focused sparse-recognition/minimization recheck retains the higher means from
the main campaign and shows they were not reproduced; both sets are reported.
Including those four rechecks, raw evidence contains 73 measured combinations.

Final legacy coaccessibility allocations fall about 61–66% across these fixtures.
On 4,096 states with degree 16, the final mean is 10.77 ms versus 15.85 ms baseline,
with about 512 KB/op versus 1,526 KB/op. The preliminary mean regression in that
case is not accepted as the final implementation. Immutable useful-state buffer
allocations keep their previous shape. No build/test runs concurrently with a
benchmark campaign.

[SnapshotMemory.java](SnapshotMemory.java) uses the existing optional agent/walker
to compare completed snapshot-created weightings with direct weight stores of the
same graph. Equality of reachable sizes checks that original identity metadata
is not retained by completed weights. Snapshot roots intentionally retain their
metadata. These counts concern reachable Java objects, not heap-dominator/native
retention or JVM overhead. [Memory output](memory.log).
The optional reflective walker requires `--add-opens java.base/java.util=ALL-UNNAMED`
in addition to `-javaagent:benchmarks/target/benchmarks.jar` on Java 17.

## Evidence and reproduction

- [Complete build](verify.log), [benchmark build](benchmarks-build.log),
  [baseline benchmark build](baseline-benchmarks-build.log).
- [Executable examples](documentation-examples.log), [verification summary](verification.json),
  [environment](environment.json), [source manifest](source-manifest.sha256),
  [documentation manifest](documentation-manifest.sha256).
- [Benchmark runner](run_benchmarks.py), [summary generator](summarize_benchmarks.py),
  [API/bytecode/client verifier](check_compatibility.py).

Build a separate benchmark JAR from `13d8ba6`, adding the identical
`GraphBridgeBench.java` fixture; build the candidate benchmark module from this
checkout. Run the comparison through `run_benchmarks.py --baseline-jar ...`.
`--candidate-only --candidate-label final` measures a final candidate separately
without re-running the baseline. Summary generation uses the retained `*-base`
and `*-final` rows; intermediate `*-current` rows stay explicitly identified.
For API checks, pass the baseline benchmark classes directory to
`check_compatibility.py --baseline-classes ...` under Java 17.

Historical audit directories describe their original snapshots and are not rewritten.
The published 4.0.0 artifacts are unchanged. No 4.1 artifact is uploaded/published.
