# Automaton 4.0 package refactoring

Baseline: merged PR #8, commit `d9c9225`. Original Boolean reference: Automaton
3.0, commit `f753243`. This change prepares version 4.0.0; no Maven publication
or release tag is performed. See [release notes](../../CHANGELOG.md), the
[scoped plan](../../RELEASE_4_PLAN.md) and [migration guide](../../docs/MIGRATION_4.md).

## Organization and implementation

Production Java types, excluding package descriptors, now occupy these packages:

| Package under `fr.menana.automaton` | Files | Responsibility |
|---|---:|---|
| root | 11 | Existing mutable Boolean API and implementation, interval partitioner |
| `regexp` | 12 | Existing expression parser and expression nodes |
| `weighted` | 20 | Shared immutable graph, scalar/vector weights, algorithms and results |
| `weighted.compiled` | 2 | Scalar/vector deterministic evaluators |
| `weighted.internal` | 1 | Unsupported checked arithmetic implementation |

The weighted core remains cohesive because its algorithms use package-private
primitive CSR arrays. Creating more subpackages would require publicizing those
arrays or adding wrappers inside tight loops solely to change folder counts.
Public Boolean 3.0 packages/signatures stay in place. Development weighted imports
move as documented; there are no aliases for those unreleased old qualified names.

`WeightedDfaIndex` extracts the existing binary interval lookup from the scalar
compiler into an immutable, weight-independent object. Scalar/vector evaluators,
explicit `of(automaton,index)` factories, reweightings, projections and
scalarizations can share it. Graph identity is checked. The vector compiler no
longer needs an intermediate component-zero scalar projection. The index holds
no cost store and reweighting retains no prior vector costs.

The existing interval partitioner is reused through the newly public immutable
`Alphabet` facade. `IntervalSet` gains checked primitive bound reads and a
copying sorted-range factory. Existing Boolean method bodies and object field
layouts are unchanged. Primitive CSR arrays remain nonpublic. Numeric checks,
path/Pareto semantics and supported algorithm families are unchanged. No runtime
dependency is added.

## Correctness and compatibility

Java 17 `mvn -B verify`: **90 tests pass**, no failures/errors/skips and no
library-build warning; binary, source and Javadoc JARs are produced. The clean
build before the final internal-Javadoc adjustment also passed all 90 tests.
The three added tests independently check interval ranges against object-based
inspection, copied-array ownership, sorted-factory validation/extreme bounds,
alphabet snapshot behavior and index identity across independent scalar/vector
weightings. All existing 87 tests are preserved, including the original 47 Boolean
tests. Weighted test files now follow their package directory.

The 11 documented Java examples match their source, compile and execute. The
English API reference covers 41 supported public types including nested types;
internal arithmetic helpers are hidden and excluded. Migration notes specify the
new imports and the unchanged Boolean API.

[Compatibility checks](compatibility.json) establish:

- All 12 originally public Boolean types (including nested types) retain their
  public declarations. All 39 pre-refactoring public weighted/Boolean types retain
  declarations after the documented weighted package relocation.
- 33 of the 35 original Boolean class files are byte-identical. The two exceptions
  are `Alphabet` (public read-only facade) and `IntervalSet` (additive range API).
- Of the original source/test files, only `Alphabet.java`, `IntervalSet.java` and
  package Javadoc change. Every original test source is untouched.
- All 106 current production class files match the separate benchmark build byte
  for byte, including package metadata. An independent direct compilation of the
  final source also matches all 101 non-package class files; see
  [final-source check](final-source-check.json).
- An unchanged client compiled against the original 3.0 classes executes five
  construction/interval/regex/language/compiled-search scenarios against 4.0
  classes without recompilation; [source](LegacyClientSmoke.java), [output](legacy-client.log).

Reproduce the signature/bytecode checks after compiling the three snapshots with
JDK 17 and debug information:

```bash
python3 audit/release-4/check_compatibility.py \
  --baseline /path/to/pre-refactoring/classes --original /path/to/original-3.0/classes
```

## Performance

The targeted JMH comparison is recorded in [PERFORMANCE.md](PERFORMANCE.md):
25 cases per version, plus a longer product follow-up per version (52 total).
Each snapshot uses its own benchmark JAR, built from the corresponding snapshot
sources because weighted qualified names change. Fixtures and benchmark method
bodies are identical. Two forks, three 500 ms warmups and five 500 ms measurement
iterations per fork use the GC profiler and 128–256 MB heaps. No concurrent build
or second benchmark campaign runs during measurement. This is a shared host;
error intervals are retained and universal performance claims are not inferred.
The first product means differed by about 15% with overlapping errors. The longer
reversed-order follow-up gives 28.93 ms before and 28.96 ms after (ratio 1.001),
with essentially equal allocations; it does not confirm that apparent slowdown.
Vector reweighting drops from 104 to 24 measured bytes/op; scalar/vector
recognition times remain within the reported overlapping error intervals.

[Benchmark runner](run_benchmarks.py), [environment](environment.json),
[current build](benchmarks-build.log) and [baseline build](baseline-benchmarks-build.log).
The optional benchmark module does not enter the production artifact.

## Reachable object memory

A deterministic fixture has 1,024 states, 4,096 edges, 8 dimensions and 16 compiled
scalarizations. The existing agent/walker counts each reachable Java object once
per identity. These are not heap-dominator, native-memory or JVM-overhead totals.

| Root object set | Before (bytes) | 4.0 (bytes) |
|---|---:|---:|
| Compiled vector evaluator, graph and cost arrays | 509,560 | 509,512 |
| Above plus 16 scalarizations sharing the index | 512,208 | 512,032 |
| Above plus 16 separately compiled scalar indexes | 1,365,328 | 1,365,664 |
| Last evaluator after 100 reweightings | 509,560 | 509,512 |
| Equivalent freshly compiled evaluator | 509,560 | 509,512 |

The small standalone-index object adds overhead when compiling many separate
indexes; sharing avoids those duplicated arrays. Reweighting retains no history.
[Current source](CompiledIndexMemory.java), [current output](memory-current.log),
[baseline output](memory-base.log); the baseline source is preserved in the
[previous audit](../weighted-advanced-cyclic/CompiledMultiMemory.java).

```bash
mkdir -p /tmp/automaton-index-memory
javac --release 17 -cp benchmarks/target/benchmarks.jar -d /tmp/automaton-index-memory \
  audit/release-4/CompiledIndexMemory.java
java -javaagent:benchmarks/target/benchmarks.jar \
  -cp /tmp/automaton-index-memory:benchmarks/target/benchmarks.jar audit.CompiledIndexMemory
```

## Evidence

- [Library verification](verify.log), [executable examples](documentation-examples.log),
  [fresh Javadoc/source artifacts](documentation-artifacts.log).
- [Verification summary](verification.json), [compatibility](compatibility.json).
- [Source manifest](source-manifest.sha256), [benchmark manifest](benchmark-manifest.sha256),
  [documentation manifest](documentation-manifest.sha256).

Historical audit directories describe their original snapshots and retain their
original logs, hashes and package names. The live manual and this report describe
4.0.0. Version preparation does not publish artifacts.
