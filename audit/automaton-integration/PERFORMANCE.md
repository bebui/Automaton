# Automaton / weighted integration performance

Baseline: merged published-4.0 sources `13d8ba6`. Candidate: 4.1 snapshot bridge
and shared primitive reachability, including the verified-index legacy adapter.

JDK 17, JMH 1.37, average microseconds/op, two forks, three 500 ms warmup and
five 500 ms measurement iterations per fork, GC profiler and 128–256 MB heaps.
Numbers are mean ± JMH-reported error; ratio is candidate/baseline, a descriptive
comparison rather than a significance test. This is a shared host. Wide errors
are retained; no universal speedup or workload guarantee is inferred. Builds
and tests did not run concurrently with benchmark measurements.

## Traversal

[Baseline JSON](traversal-base.json), [final JSON](traversal-final.json),
[baseline log](traversal-base.log), [final log](traversal-final.log).

| Method / parameters | Baseline µs/op | Final µs/op | Ratio | Bytes/op before → final |
|---|---:|---:|---:|---:|
| `graphUseful`; degree=16, states=4096 | 641.3 ± 82 | 648.1 ± 61 | 1.011 | 24631.506 → 24631.644 |
| `graphUseful`; degree=16, states=64 | 8.147 ± 0.44 | 7.777 ± 0.57 | 0.954 | 432.096 → 432.092 |
| `graphUseful`; degree=2, states=4096 | 137.3 ± 11 | 125.7 ± 5.9 | 0.915 | 24625.631 → 24625.484 |
| `graphUseful`; degree=2, states=64 | 2.544 ± 0.25 | 2.19 ± 0.041 | 0.861 | 432.030 → 432.026 |
| `legacyCoaccessible`; degree=16, states=4096 | 1.585e+04 ± 2.6e+03 | 1.077e+04 ± 7.9e+02 | 0.679 | 1525839.407 → 512380.864 |
| `legacyCoaccessible`; degree=16, states=64 | 137.6 ± 12 | 59.65 ± 2.1 | 0.434 | 23713.623 → 8160.707 |
| `legacyCoaccessible`; degree=2, states=4096 | 9060 ± 1.4e+04 | 2571 ± 1.7e+02 | 0.284 | 736232.071 → 282910.198 |
| `legacyCoaccessible`; degree=2, states=64 | 37.05 ± 20 | 14.49 ± 0.42 | 0.391 | 11781.672 → 4576.172 |

## Import

[Baseline JSON](import-base.json), [final JSON](import-final.json),
[baseline log](import-base.log), [final log](import-final.log).

| Method / parameters | Baseline µs/op | Final µs/op | Ratio | Bytes/op before → final |
|---|---:|---:|---:|---:|
| `importScalar`; degree=4, states=256 | 501.5 ± 54 | 509.6 ± 82 | 1.016 | 250796.412 → 251874.083 |

## Boolean

[Baseline JSON](boolean-base.json), [final JSON](boolean-final.json),
[baseline log](boolean-base.log), [final log](boolean-final.log).

| Method / parameters | Baseline µs/op | Final µs/op | Ratio | Bytes/op before → final |
|---|---:|---:|---:|---:|
| `compiledRun`; family=dense-interval | 2.844 ± 0.72 | 3.119 ± 0.52 | 1.097 | 0.033 → 0.036 |
| `compiledRun`; family=sparse-interval | 1.132 ± 0.099 | 1.109 ± 0.067 | 0.980 | 0.013 → 0.013 |
| `minimize`; family=dense-interval | 2.776e+04 ± 1.8e+04 | 2.674e+04 ± 4.2e+04 | 0.963 | 1265812.165 → 1264077.916 |
| `minimize`; family=sparse-interval | 2238 ± 1.8e+03 | 6002 ± 6.4e+03 | 2.681 | 238092.760 → 251443.402 |
| `run`; family=dense-interval | 19.05 ± 4.5 | 17.33 ± 2.6 | 0.909 | 0.223 → 0.202 |
| `run`; family=sparse-interval | 2.842 ± 0.19 | 3.472 ± 0.44 | 1.221 | 0.033 → 0.040 |

## Snapshot APIs

[Final raw JSON](snapshot-api-final.json), [log](snapshot-api-final.log).
A 256-state, degree-4 fixture has two disconnected components and wide symbolic
intervals. The NFA adds one epsilon edge. Scalars use cost 2, vectors costs 2/7,
and membership reads an existing 64-symbol array. Snapshot reuse avoids copying
topology while still capturing independent cost arrays. Projection is a fresh
mutable Boolean automaton and intentionally forgets costs.

| Method / family | µs/op | Bytes/op |
|---|---:|---:|
| `booleanProjection`; dfa | 346.3 ± 42 | 206429.025 |
| `booleanProjection`; epsilon-nfa | 306.9 ± 59 | 214539.618 |
| `capture`; dfa | 2721 ± 4.7e+03 | 237230.327 |
| `capture`; epsilon-nfa | 1013 ± 1.1e+03 | 234093.427 |
| `membership`; dfa | 2.18 ± 2.1 | 0.025 |
| `membership`; epsilon-nfa | 90.13 ± 17 | 1585.046 |
| `scalarFromMutable`; dfa | 945 ± 9.2e+02 | 254469.398 |
| `scalarFromMutable`; epsilon-nfa | 574 ± 1e+02 | 252081.773 |
| `scalarFromSnapshot`; dfa | 13.81 ± 1.5 | 20616.163 |
| `scalarFromSnapshot`; epsilon-nfa | 15.15 ± 3.4 | 20632.180 |
| `vectorFromSnapshot`; dfa | 37.7 ± 6.6 | 41178.963 |
| `vectorFromSnapshot`; epsilon-nfa | 37.98 ± 11 | 41212.403 |

## Initial adapter and interpretation

The first adapter used identity-map lookups for every arc even when existing
state indices were canonical. In the largest dense fixture its mean was 18.44 ms
against 15.85 ms baseline, with overlapping errors. The final adapter verifies
indices once and uses them directly, retaining the identity-map path for
reordered/noncanonical state lists. The final mean there is 10.77 ms with
about 512 KB/op against 1,526 KB/op baseline. Allocations drop roughly 61–66%
across the four legacy coaccessibility fixtures. Wide baseline errors prevent
precise speedup conclusions in every case. Immutable useful-state buffers retain
their prior allocation shape; old recognition/compiler bodies are unchanged.

Initial raw evidence: [traversal JSON](traversal-current.json), [log](traversal-current.log),
[import JSON](import-current.json), [log](import-current.log),
[Boolean JSON](boolean-current.json), [log](boolean-current.log),
[snapshot API JSON](snapshot-api.json), [log](snapshot-api.log).
Final measurements cover 15 baseline cases, 15 final comparison cases and 12
final new-API cases. Including 27 intermediate candidate cases, 69 combinations
were measured before the four focused rechecks below. No initial rows are
silently substituted for final measurements.

## Focused Boolean recheck

The main campaign showed a 22% higher sparse-recognition mean and a very noisy
sparse-minimization mean. Both cases were measured again, baseline then candidate,
with unchanged JMH settings. The original recognizer body and compiled evaluator
are unchanged; the recheck does not reproduce either slowdown. The main campaign
is retained above, rather than replaced by these later samples. This shared-host
evidence does not establish a persistent recognition/minimization regression.

[Baseline JSON](boolean-recheck-base.json), [candidate JSON](boolean-recheck-final.json),
[baseline log](boolean-recheck-base.log), [candidate log](boolean-recheck-final.log).

| Method / sparse-interval | Baseline µs/op | Candidate µs/op | Ratio |
|---|---:|---:|---:|
| `minimize` | 1196 ± 6e+02 | 1271 ± 8.2e+02 | 1.063 |
| `run` | 3.303 ± 0.73 | 3.034 ± 0.66 | 0.919 |

The complete evidence contains 73 measured combinations including the four rechecks.

[Integration report](README.md) · [Runner](run_benchmarks.py)
