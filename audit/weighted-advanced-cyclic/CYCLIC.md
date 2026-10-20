# Cyclic algorithms and distinct words

JMH 1.37, Java 17, two forks, 3 × 500 ms warmup and 5 × 500 ms measurement per fork, GC profiler. Shared-host preliminary measurements; raw samples/errors retained.

The 32/128-state graphs have signed dyadic edges and positive consuming cycles. The NFA adds ambiguity and zero epsilon loops. Minimization is a favorable case where all normalized states are equivalent. `firstFullRangeWord` and `signedHorizonWords` have fixed fixtures independent of the states parameter: their repeated rows are not size scaling. Times include preparation of each query; a first lazy output is not measured after reused preprocessing.

| Method | states | µs/op | JMH error | B/op |
|---|---:|---:|---:|---:|
| distinctWords16 | 32 | 364.513 | 294.996 | 50322.11 |
| distinctWords16 | 128 | 1011.781 | 393.344 | 165077.11 |
| firstFullRangeWord | 32 | 1.624 | 0.045 | 1360.02 |
| firstFullRangeWord | 128 | 1.738 | 0.063 | 1360.02 |
| firstLazyPath | 32 | 39.113 | 2.794 | 21096.46 |
| firstLazyPath | 128 | 152.157 | 10.324 | 81769.81 |
| firstLazyWord | 32 | 38.517 | 1.459 | 21168.46 |
| firstLazyWord | 128 | 148.748 | 4.622 | 81826.63 |
| kCyclic16 | 32 | 58.184 | 1.077 | 29976.77 |
| kCyclic16 | 128 | 175.400 | 9.895 | 90650.09 |
| minimizeCyclic | 32 | 219.378 | 223.580 | 53802.56 |
| minimizeCyclic | 128 | 589.849 | 415.794 | 210814.96 |
| residualCyclic | 32 | 1302.910 | 477.111 | 165287.96 |
| residualCyclic | 128 | 7238.427 | 1447.393 | 755499.24 |
| signedHorizonWords | 32 | 121.149 | 49.283 | 22245.24 |
| signedHorizonWords | 128 | 170.832 | 151.881 | 22286.83 |

[Raw JSON](cyclic.json), [log and command](cyclic.log). No universal speedup is inferred from these fixtures.
