# Mesures pondérées — état final

JMH 1.37, JDK 17, un thread, deux forks ; 3 échauffements et 5 mesures de 500 ms par fork ; heap 128–256 Mio, profiler GC. 33 combinaisons complètes. Machine partagée ; référence préliminaire.

| Opération | Paramètres | µs/op | ± erreur JMH | octets/op |
|---|---|---:|---:|---:|
| backwardDistances | fixture scalaire | 11.422 | 0.544 | 2120.135 |
| bellmanFord | fixture scalaire | 4341.858 | 406.179 | 4962.621 |
| compiledDfa | fixture scalaire | 2.731 | 0.072 | 0.032 |
| concatenate | fixture scalaire | 176.104 | 9.132 | 151066.092 |
| conditionalCost | fixture scalaire | 0.516 | 0.006 | 0.006 |
| construction | fixture scalaire | 85.417 | 5.649 | 81553.011 |
| dagDistances | fixture scalaire | 11.420 | 0.456 | 2696.136 |
| dijkstra | fixture scalaire | 69.382 | 5.495 | 4400.819 |
| epsilonCost | fixture scalaire | 12.980 | 1.054 | 240.152 |
| epsilonReusable | fixture scalaire | 11.879 | 0.165 | 0.139 |
| kBest | fixture scalaire | 370.911 | 40.798 | 149719.817 |
| lexicographic | dimensions=2 | 45.571 | 2.659 | 18680.542 |
| lexicographic | dimensions=8 | 91.001 | 3.383 | 30801.073 |
| lexicographic | dimensions=32 | 287.054 | 13.410 | 80771.424 |
| nfaCost | fixture scalaire | 240.959 | 11.664 | 2130.842 |
| nfaReusable | fixture scalaire | 231.136 | 2.785 | 2.728 |
| pareto | dominance=strong | 10.903 | 3.148 | 5214.120 |
| pareto | dominance=tradeoff | 197.303 | 7.216 | 35322.355 |
| product | fixture scalaire | 28330.883 | 4066.190 | 13212042.755 |
| reconstructWord | fixture scalaire | 826.317 | 1530.723 | 107234.329 |
| reuseUnfolding | fixture scalaire | 0.027 | 0.002 | 48.000 |
| scalarDfa | fixture scalaire | 2.121 | 0.044 | 0.025 |
| scalarizeMaterialized | dimensions=2 | 16.903 | 0.892 | 6320.201 |
| scalarizeMaterialized | dimensions=8 | 46.004 | 2.207 | 6368.546 |
| scalarizeMaterialized | dimensions=32 | 177.407 | 27.438 | 6562.089 |
| scalarizeView | dimensions=2 | 0.063 | 0.003 | 80.001 |
| scalarizeView | dimensions=8 | 0.091 | 0.004 | 128.001 |
| scalarizeView | dimensions=32 | 0.235 | 0.020 | 320.003 |
| scalarizedShortest | dimensions=2 | 19.345 | 1.710 | 2159.922 |
| scalarizedShortest | dimensions=8 | 41.325 | 2.340 | 2142.235 |
| scalarizedShortest | dimensions=32 | 123.056 | 3.510 | 2141.449 |
| unfolding | fixture scalaire | 398.905 | 395.945 | 100244.699 |
| union | fixture scalaire | 178.329 | 24.746 | 151226.115 |

Une très faible allocation positive peut venir du bruit du profiler. Les données JSON conservent les 10 échantillons et les intervalles de confiance JMH. Le coût de création d’une vue ne comprend pas les produits scalaires calculés ensuite : `scalarizedShortest` mesure ces accès dans l’algorithme.
