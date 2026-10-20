# Non-régression booléenne

Même JDK 17, deux forks, échauffement 3 × 500 ms, mesures 5 × 500 ms par fork ; campagnes successives sur machine partagée. Les écarts de temps sont une observation locale, pas une modification des algorithmes : leurs 35 fichiers de classes sont identiques octet pour octet.

| Opération | Famille | Base µs/op | Extension µs/op | Variation | Erreur base | Erreur extension |
|---|---|---:|---:|---:|---:|---:|
| compiledRun | dense-unit | 2.409 | 2.414 | +0.2 % | 0.068 | 0.073 |
| compiledRun | sparse-unit | 1.085 | 1.099 | +1.3 % | 0.005 | 0.040 |
| minimize | dense-unit | 1744.422 | 1736.874 | -0.4 % | 336.176 | 1018.317 |
| minimize | sparse-unit | 403.193 | 372.267 | -7.7 % | 287.134 | 290.096 |
| run | dense-unit | 15.492 | 15.334 | -1.0 % | 0.671 | 1.216 |
| run | sparse-unit | 2.090 | 2.109 | +0.9 % | 0.050 | 0.041 |

Les erreurs sont celles de JMH ; conserver les distributions brutes dans les deux JSON. Une variation dépassant 10 % ne prouve pas une régression reproductible sur cette machine. Voir également `original-bytecode.json` et `original-files.sha256`.
