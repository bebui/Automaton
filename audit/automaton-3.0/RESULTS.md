# Mesures JMH 3.0.0

39 combinaisons complètes, deux forks et dix échantillons mesurés par combinaison.

Temps moyen et erreur fournis par JMH, en microsecondes par opération. Allocations en octets par opération ; une petite valeur positive sur la reconnaissance reflète notamment le bruit du profiler.

Référence préliminaire sur machine partagée. Plusieurs écarts entre forks sont élevés ; allonger l’échauffement et alterner base/candidat avant de conclure à une régression.

| Opération | Famille | µs/op | ± erreur JMH | octets/op |
|---|---|---:|---:|---:|
| compile | sparse-unit | 19.448 | 3.783 | 6640.231 |
| compile | sparse-interval | 76.944 | 6.055 | 26032.910 |
| compile | dense-unit | 95.771 | 2.796 | 28089.137 |
| compile | dense-interval | 577.737 | 186.063 | 111974.867 |
| compiledRun | sparse-unit | 1.086 | 0.012 | 0.013 |
| compiledRun | sparse-interval | 1.082 | 0.034 | 0.013 |
| compiledRun | dense-unit | 2.371 | 0.040 | 0.028 |
| compiledRun | dense-interval | 2.572 | 0.124 | 0.030 |
| construct | sparse-unit | 33.136 | 4.027 | 28912.394 |
| construct | sparse-interval | 145.205 | 11.041 | 116225.709 |
| construct | dense-unit | 386.186 | 38.632 | 160836.570 |
| construct | dense-interval | 1536.355 | 53.230 | 607506.203 |
| copy | sparse-unit | 31.960 | 1.760 | 25920.379 |
| copy | sparse-interval | 131.029 | 6.454 | 103489.553 |
| copy | dense-unit | 191.994 | 3.710 | 130714.283 |
| copy | dense-interval | 1075.075 | 51.076 | 558012.744 |
| countWords | sparse-unit | 199.557 | 12.819 | 172250.369 |
| countWords | sparse-interval | 1370.665 | 24.716 | 1188256.177 |
| countWords | dense-unit | 2391.061 | 68.841 | 1925564.219 |
| countWords | dense-interval | 13699.797 | 1181.791 | 12725384.039 |
| determinize | NFA regexp | 74.199 | 20.153 | 18288.878 |
| intersection | sparse-unit | 18927.638 | 15039.845 | 2079277.760 |
| intersection | sparse-interval | 97688.346 | 88985.358 | 8183966.253 |
| intersection | dense-unit | 46262.465 | 5431.953 | 9964318.079 |
| intersection | dense-interval | 416596.857 | 156607.214 | 39889082.933 |
| minimize | sparse-unit | 269.511 | 79.923 | 60519.139 |
| minimize | sparse-interval | 1497.677 | 814.350 | 239896.042 |
| minimize | dense-unit | 1894.821 | 1279.485 | 303549.099 |
| minimize | dense-interval | 7854.671 | 5661.652 | 1254522.757 |
| nfaRun | NFA regexp | 81.332 | 1.323 | 35714.676 |
| removeEpsilon | NFA regexp | 13.825 | 5.705 | 6637.874 |
| run | sparse-unit | 2.107 | 0.031 | 0.025 |
| run | sparse-interval | 2.985 | 0.070 | 0.035 |
| run | dense-unit | 15.190 | 1.716 | 0.179 |
| run | dense-interval | 16.102 | 1.228 | 0.190 |
| shortestWord | sparse-unit | 1.088 | 0.021 | 1008.013 |
| shortestWord | sparse-interval | 1.864 | 0.358 | 3568.022 |
| shortestWord | dense-unit | 10.286 | 0.876 | 1480.122 |
| shortestWord | dense-interval | 4.388 | 0.180 | 4296.052 |

[Résultats JSON complets](jmh-results.json), [journal JMH](jmh.log), [source et reproduction](BASELINE.md), [tailles des fixtures](fixtures.log).
