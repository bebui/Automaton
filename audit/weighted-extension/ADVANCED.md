# Mesures advanced

JMH 1.37, JDK 17, 2 fork(s), 3 × 500 ms d’échauffement et 5 × 500 ms de mesure par fork. Référence préliminaire sur machine partagée.

| Opération | Paramètres | µs/op | ± erreur JMH | octets/op |
|---|---|---:|---:|---:|
| eliminateEpsilon | petite fixture certifiée/exacte | 27.376 | 19.042 | 10309.050 |
| exactBellmanFord | petite fixture certifiée/exacte | 2.622 | 0.305 | 1568.031 |
| exactPareto | petite fixture certifiée/exacte | 5.085 | 1.128 | 2943.039 |
| residualDeterminize | petite fixture certifiée/exacte | 621.210 | 503.979 | 56997.767 |
| weightedCompile | petite fixture certifiée/exacte | 4.276 | 0.235 | 2968.051 |
| weightedMinimize | petite fixture certifiée/exacte | 87.796 | 85.574 | 35137.032 |
