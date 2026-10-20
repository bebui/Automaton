# Mesures scale

JMH 1.37, JDK 17, 1 fork(s), 3 × 500 ms d’échauffement et 5 × 500 ms de mesure par fork. Référence préliminaire sur machine partagée.

| Opération | Paramètres | µs/op | ± erreur JMH | octets/op |
|---|---|---:|---:|---:|
| dagDistances | degree=2, states=128, width=1 | 6.698 | 0.834 | 2672.079 |
| dagDistances | degree=2, states=128, width=1000000 | 6.697 | 0.904 | 2672.079 |
| dagDistances | degree=2, states=8192, width=1 | 480.260 | 105.010 | 163957.749 |
| dagDistances | degree=2, states=8192, width=1000000 | 510.725 | 110.670 | 163958.068 |
| dagDistances | degree=16, states=128, width=1 | 29.827 | 1.850 | 2672.351 |
| dagDistances | degree=16, states=128, width=1000000 | 31.034 | 4.126 | 2672.368 |
| dagDistances | degree=16, states=8192, width=1 | 2057.923 | 338.972 | 163976.278 |
| dagDistances | degree=16, states=8192, width=1000000 | 2057.555 | 190.305 | 163976.204 |
| recognizeCompiled | degree=2, states=128, width=1 | 1.489 | 0.031 | 0.018 |
| recognizeCompiled | degree=2, states=128, width=1000000 | 1.483 | 0.014 | 0.017 |
| recognizeCompiled | degree=2, states=8192, width=1 | 1.485 | 0.007 | 0.017 |
| recognizeCompiled | degree=2, states=8192, width=1000000 | 1.494 | 0.038 | 0.018 |
| recognizeCompiled | degree=16, states=128, width=1 | 3.348 | 0.331 | 0.039 |
| recognizeCompiled | degree=16, states=128, width=1000000 | 3.309 | 0.128 | 0.039 |
| recognizeCompiled | degree=16, states=8192, width=1 | 4.047 | 0.339 | 0.047 |
| recognizeCompiled | degree=16, states=8192, width=1000000 | 4.120 | 0.365 | 0.048 |
| recognizeLinear | degree=2, states=128, width=1 | 2.332 | 0.972 | 0.027 |
| recognizeLinear | degree=2, states=128, width=1000000 | 2.476 | 0.102 | 0.029 |
| recognizeLinear | degree=2, states=8192, width=1 | 2.604 | 0.075 | 0.031 |
| recognizeLinear | degree=2, states=8192, width=1000000 | 2.689 | 0.059 | 0.032 |
| recognizeLinear | degree=16, states=128, width=1 | 8.916 | 0.407 | 0.105 |
| recognizeLinear | degree=16, states=128, width=1000000 | 8.803 | 0.464 | 0.104 |
| recognizeLinear | degree=16, states=8192, width=1 | 9.197 | 0.594 | 0.108 |
| recognizeLinear | degree=16, states=8192, width=1000000 | 9.640 | 2.574 | 0.112 |

Un seul fork ici : ces mesures ne couvrent pas la variance entre JVM. Les graphes de distance sont des DAG ; les graphes de reconnaissance sont des DFA cycliques complets, et les 64 symboles préalloués couvrent uniformément les classes sortantes. Les largeurs d’intervalles ne provoquent aucune énumération de l’alphabet.
