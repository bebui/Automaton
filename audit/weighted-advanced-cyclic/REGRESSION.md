# Baseline/current comparison

Base: merged PR #7 (`43f998a`), separately compiled with Java 17 from its tracked sources. Candidate: this extension. Both use the same final JMH harness and two forks, 3 × 500 ms warmup, 5 × 500 ms measurement per fork and the GC profiler. The baseline campaign ran before the candidate campaign on a shared host.

| Method | Fixture | Base µs/op | Current µs/op | Change | Base error | Current error |
|---|---|---:|---:|---:|---:|---:|
| residualDeterminize | weighted DAG | 509.674 | 330.292 | -35.2% | 272.430 | 271.883 |
| weightedMinimize | weighted DAG | 75.729 | 69.568 | -8.1% | 46.992 | 30.688 |
| compiledRun | sparse-interval | 1.074 | 1.082 | +0.7% | 0.011 | 0.023 |
| compiledRun | dense-interval | 2.532 | 2.488 | -1.7% | 0.071 | 0.035 |
| run | sparse-interval | 3.070 | 2.965 | -3.4% | 0.265 | 0.044 |
| run | dense-interval | 16.121 | 16.498 | +2.3% | 1.155 | 1.962 |
| kBest | weighted DAG | 342.884 | 362.090 | +5.6% | 33.749 | 52.964 |

Boolean compiled recognition/recognition changes range from −3.4% to +2.3%; the original Boolean bytecode is also identical. DAG K-best is +5.6% in the means with overlapping wide error intervals. Weighted transform uncertainty is particularly large; the lower means do not demonstrate an improvement. No substantial regression is established in this measured subset. These results do not cover every workload, and the nonalternating shared-host campaigns are not a precise causal comparison.

[Baseline JSON](regression-base.json), [candidate JSON](regression-current.json), [baseline log](regression-base.log), [candidate log](regression-current.log).
