# Package-refactoring performance comparison

Baseline: merged PR #8 (`d9c9225`). Candidate: 4.0.0 package organization and
the extracted weight-independent interval index. Both snapshots use the same
fixture/method bodies, with only weighted package imports adapted.

JDK 17, JMH 1.37, average microseconds/op, two forks, three 500 ms warmup and
five 500 ms measurement iterations per fork, GC profiler, 128–256 MB heaps.
Each row reports the JMH mean ± reported error. Ratio is candidate/baseline;
it is descriptive, not a significance test. Allocations are profiler bytes/op.
A small fractional allocation on recognition reflects profiler/background
overhead; these APIs do not allocate work/result arrays for existing inputs.

The machine is shared. No build or other campaign was run concurrently by this
task. Overlapping uncertainty does not establish a speedup or regression.
These targeted 25 cases per version assess this refactoring; earlier algorithm
benchmarks remain historical evidence rather than a repeated complete campaign.

## Multi

[Baseline JSON](multi-base.json), [candidate JSON](multi-current.json),
[baseline log](multi-base.log), [candidate log](multi-current.log).

| Method / parameters | Baseline µs/op | 4.0 µs/op | Ratio | Bytes/op before → 4.0 |
|---|---:|---:|---:|---:|
| `compileVector`; dimensions=2 | 109.4 ± 1.9 | 111.7 ± 1.7 | 1.021 | 64177.296 → 64129.323 |
| `compileVector`; dimensions=32 | 114.8 ± 11 | 113.4 ± 4.9 | 0.988 | 64177.364 → 64129.343 |
| `compileVector`; dimensions=8 | 109.8 ± 2.4 | 112.5 ± 3.6 | 1.024 | 64177.303 → 64129.331 |
| `reweightVector`; dimensions=2 | 0.05867 ± 0.0051 | 0.01471 ± 0.00089 | 0.251 | 104.001 → 24.000 |
| `reweightVector`; dimensions=32 | 0.06273 ± 0.012 | 0.0151 ± 0.0013 | 0.241 | 104.001 → 24.000 |
| `reweightVector`; dimensions=8 | 0.05946 ± 0.0066 | 0.01551 ± 0.0026 | 0.261 | 104.001 → 24.000 |
| `scalarizeCompiled`; dimensions=2 | 0.07944 ± 0.0037 | 0.08222 ± 0.0056 | 1.035 | 112.001 → 104.001 |
| `scalarizeCompiled`; dimensions=32 | 0.247 ± 0.017 | 0.2632 ± 0.048 | 1.066 | 352.003 → 344.003 |
| `scalarizeCompiled`; dimensions=8 | 0.1159 ± 0.005 | 0.1074 ± 0.0095 | 0.927 | 160.001 → 152.001 |
| `vectorBuffered`; dimensions=2 | 4.551 ± 0.14 | 4.526 ± 0.18 | 0.995 | 0.053 → 0.053 |
| `vectorBuffered`; dimensions=32 | 14.18 ± 0.31 | 14.79 ± 1.1 | 1.043 | 0.167 → 0.174 |
| `vectorBuffered`; dimensions=8 | 7.604 ± 0.81 | 7.299 ± 0.12 | 0.960 | 0.089 → 0.086 |

## Weighted

[Baseline JSON](weighted-base.json), [candidate JSON](weighted-current.json),
[baseline log](weighted-base.log), [candidate log](weighted-current.log).

| Method / parameters | Baseline µs/op | 4.0 µs/op | Ratio | Bytes/op before → 4.0 |
|---|---:|---:|---:|---:|
| `construction` | 82.79 ± 2.1 | 84.71 ± 10 | 1.023 | 81552.984 → 81553.009 |
| `product` | 2.693e+04 ± 2e+03 | 3.092e+04 ± 3e+03 | 1.148 | 13199792.527 → 13218203.313 |
| `union` | 174.5 ± 8.7 | 188.7 ± 20 | 1.082 | 151226.073 → 151226.244 |

## Scalar

[Baseline JSON](scalar-base.json), [candidate JSON](scalar-current.json),
[baseline log](scalar-base.log), [candidate log](scalar-current.log).

| Method / parameters | Baseline µs/op | 4.0 µs/op | Ratio | Bytes/op before → 4.0 |
|---|---:|---:|---:|---:|
| `recognizeCompiled`; degree=16, states=128, width=1000000 | 3.411 ± 0.33 | 3.225 ± 0.074 | 0.946 | 0.040 → 0.038 |
| `recognizeCompiled`; degree=2, states=128, width=1000000 | 1.493 ± 0.038 | 1.433 ± 0.041 | 0.960 | 0.018 → 0.017 |

## Boolean

[Baseline JSON](boolean-base.json), [candidate JSON](boolean-current.json),
[baseline log](boolean-base.log), [candidate log](boolean-current.log).

| Method / parameters | Baseline µs/op | 4.0 µs/op | Ratio | Bytes/op before → 4.0 |
|---|---:|---:|---:|---:|
| `compile`; family=dense-interval | 537.6 ± 1.2e+02 | 564 ± 1.6e+02 | 1.049 | 111974.387 → 112240.133 |
| `compile`; family=sparse-interval | 74.06 ± 1.4 | 73.53 ± 2.2 | 0.993 | 26032.881 → 26032.871 |
| `compiledRun`; family=dense-interval | 2.502 ± 0.091 | 2.498 ± 0.082 | 0.998 | 0.029 → 0.029 |
| `compiledRun`; family=sparse-interval | 1.083 ± 0.026 | 1.094 ± 0.039 | 1.010 | 0.013 → 0.013 |
| `minimize`; family=dense-interval | 7796 ± 2.8e+03 | 8199 ± 3.2e+03 | 1.052 | 1253915.830 → 1254257.242 |
| `minimize`; family=sparse-interval | 1221 ± 4.2e+02 | 1092 ± 2.3e+02 | 0.894 | 243451.273 → 238479.279 |
| `run`; family=dense-interval | 16.18 ± 0.93 | 15.93 ± 0.8 | 0.985 | 0.190 → 0.187 |
| `run`; family=sparse-interval | 3.118 ± 0.064 | 3.273 ± 0.32 | 1.050 | 0.037 → 0.038 |

## Focused product follow-up

The initial product means differed by about 15%, with overlapping error
intervals. A follow-up reverses the run order (candidate, then baseline)
and uses two forks, five 1 s warmups and five 1 s measurements per fork.
It finds essentially equal mean times and allocations after longer warmup:

| Method | Baseline µs/op | 4.0 µs/op | Ratio | Bytes/op before → 4.0 |
|---|---:|---:|---:|---:|
| `product` | 2.893e+04 ± 3.5e+03 | 2.896e+04 ± 1.6e+03 | 1.001 | 13197905.912 → 13197905.112 |

[Baseline JSON](product-followup-base.json), [candidate JSON](product-followup-current.json),
[baseline log](product-followup-base.log), [candidate log](product-followup-current.log),
[follow-up runner](run_product_followup.py). These longer measurements do
not confirm the initial apparent slowdown; the original rows remain above.

## Interpretation

The extraction primarily changes setup ownership and reuse: vector reweighting
creates only a small evaluator instead of a scalar projection and scalar
compiled wrapper. Shared-index scalarization also avoids rebuilding lookup
arrays; graph and vector stores remain shared. Compare allocation columns
for these setup operations separately from recognition timing.

Original Boolean recognition/compiler algorithm bodies are unchanged; the
Boolean rows measure non-regression on sparse/dense interval fixtures.
Interval copying is exercised by weighted construction, union and product.
Scalar recognition uses two outgoing degrees, and direct vector evaluation
covers 2, 8 and 32 dimensions. No broader workload guarantee is inferred.

[Refactoring report](README.md) · [Environment](environment.json) · [Runner](run_benchmarks.py)
