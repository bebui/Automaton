# Compiled vector costs and scalar composition

JMH 1.37, Java 17, two forks, 3 × 500 ms warmup and 5 × 500 ms measurement per fork, GC profiler. Shared-host preliminary measurements; raw samples/errors retained.

The DFA has 128 states and 16 outgoing interval classes, with 64-symbol words rotating across all classes. Output buffers are reused for `vectorBuffered`. `productScalars` always uses the same pair of nine-state chains, so its dimension-parameter rows repeat one fixed workload. Sub-byte profiler allocation for buffered evaluation is close to background noise; scalarization with index reuse performs a different operation from recompilation.

| Method | dimensions | µs/op | JMH error | B/op |
|---|---:|---:|---:|---:|
| compileVector | 2 | 110.814 | 6.557 | 64177.31 |
| compileVector | 8 | 110.534 | 2.489 | 64177.31 |
| compileVector | 32 | 110.588 | 4.360 | 64177.31 |
| productScalars | 2 | 108.415 | 41.766 | 61677.47 |
| productScalars | 8 | 129.206 | 46.992 | 60516.96 |
| productScalars | 32 | 172.607 | 222.027 | 60415.65 |
| reweightVector | 2 | 0.058 | 0.007 | 104.00 |
| reweightVector | 8 | 0.058 | 0.008 | 104.00 |
| reweightVector | 32 | 0.057 | 0.005 | 104.00 |
| scalarizeAndCompile | 2 | 110.584 | 3.056 | 64185.31 |
| scalarizeAndCompile | 8 | 111.219 | 3.387 | 64233.32 |
| scalarizeAndCompile | 32 | 114.081 | 5.075 | 64425.35 |
| scalarizeCompiled | 2 | 0.078 | 0.001 | 112.00 |
| scalarizeCompiled | 8 | 0.110 | 0.001 | 160.00 |
| scalarizeCompiled | 32 | 0.232 | 0.004 | 352.00 |
| sharedWeightings | 2 | 0.053 | 0.003 | 64.00 |
| sharedWeightings | 8 | 0.071 | 0.001 | 88.00 |
| sharedWeightings | 32 | 0.143 | 0.007 | 184.00 |
| vectorAllocated | 2 | 4.862 | 0.665 | 32.06 |
| vectorAllocated | 8 | 7.622 | 0.564 | 80.09 |
| vectorAllocated | 32 | 14.757 | 0.545 | 272.17 |
| vectorBuffered | 2 | 4.642 | 0.213 | 0.05 |
| vectorBuffered | 8 | 7.481 | 0.207 | 0.09 |
| vectorBuffered | 32 | 14.733 | 1.923 | 0.17 |

[Raw JSON](compiled-multi.json), [log and command](compiled-multi.log). No universal speedup is inferred from these fixtures.
