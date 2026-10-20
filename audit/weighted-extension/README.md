# Validation de l’extension pondérée

Historical snapshot: initial PR #7 implementation (commit `2c19468`).
The [advanced follow-up](../weighted-advanced-cyclic/README.md) extends its restrictions;
measurements and hashes below still describe this original snapshot.

Base : Automaton 3.0.0, commit `f7532436cbe964758988e13f8c5872c43bebe58b`.
Ces résultats ont été obtenus avant le premier commit et le premier push.
Les compteurs Git du fichier `verification.json` décrivent cet instantané
de validation ; la publication ultérieure en pull request est autorisée par Julien.

## Implémentation et tests

Le [guide](../../WEIGHTED_AUTOMATA.md) décrit les API livrées et leurs limites.
Le [plan](../../WEIGHTED_AUTOMATA_PLAN.md), section 14, distingue le réalisé des
familles avancées rejetées explicitement. Les fonctionnalités booléennes n’ont
reçu aucun champ poids ni changement algorithmique ; le POM principal est intact.

`mvn -B clean verify` sous Java 17 : **71 tests réussis**, aucun échec/erreur/ignoré,
packaging principal/sources/Javadocs réussi, aucun avertissement dans ce build.
Voir [verify.log](verify.log). Les validations intermédiaires de P1/P2 et P3/P5
sont conservées dans `p1-p2-tests.log` et `p3-p5-tests.log`.

Parmi les oracles nouveaux : 180 petits DAG confrontés à une énumération des
chemins/mots, 240 graphes cycliques confrontés à Floyd–Warshall, 100 graphes et
leurs chemins de longueur 0 à 4 énumérés, 180 DAG vectoriels comparés à tous les
vecteurs puis une dominance indépendante, 100 familles de déterminisation/
minimisation confrontées aux mots énumérés, 80 DAG pour K chemins exhaustifs,
et des cas d’annulation de grands nombres, ε-cycles nuls/négatifs et coûts
quasi égaux. Les anciennes campagnes 3.0 restent exécutées intégralement.

Les deux représentations des ensembles de parcours sont documentées : un
représentant par vecteur, ou tous les parcours logiques égaux sur un DAG.
Les approximations Pareto ne sont pas implémentées. Les transformations
résiduelles/potentiels sont limitées aux DAG à coûts entiers bornés. Les
limites IEEE 754 des calculs scalaires rapides sont explicites ; les signes
de cycles de Bellman–Ford et les comparaisons multiobjectifs/K utilisent un
recours exact quand leur certificat de sommes primitives ne suffit pas.

## Non-régression

[original-bytecode.json](original-bytecode.json) confirme que **35 fichiers de
classes d’origine sont identiques octet pour octet**, après recompilation
séparée des seuls fichiers 3.0. Les empreintes des sources/tests/POM/CI d’origine
sont dans [original-files.sha256](original-files.sha256).

[UNWEIGHTED.md](UNWEIGHTED.md) compare six combinaisons JMH de reconnaissance,
reconnaissance compilée et minimisation sur graphes creux/denses. Les deux
campagnes ont les mêmes paramètres ; les mesures de base précèdent celles
de l’extension. Elles restent bruitées sur cette machine partagée. Le harnais
complet P0, avec ses 39 combinaisons, est également conservé dans le projet.

## Benchmarks de l’extension

Les sources reproductibles et le module de benchmark autonome sont dans
[benchmarks](../../benchmarks/README.md). Ils ne changent pas les dépendances
du JAR de production. Les rapports JSON/JMH finaux sont joints à ce dossier.
Les mesures de chaque famille sont effectuées sur l’état final testé, et non
présentées comme une mesure exhaustive de chaque instantané intermédiaire.

Les temps sont des références locales préliminaires, avec paramètres et
échantillons bruts conservés. Les comparaisons de vues et matérialisations
mesurent des opérations différentes et ne prétendent pas qu’une vue est
systématiquement plus rapide pour tous les calculs ultérieurs.

## Mémoire des objets accessibles

[memory.log](memory.log) a été obtenu avec l’agent d’instrumentation JVM et un
parcours des références comptant chaque objet une fois par identité. Fixture :
1 025 états, 2 047 arcs, 8 dimensions, 16 scalarisations.

| Ensemble d’objets accessibles | Octets |
|---|---:|
| Graphe seul | 73 000 |
| Multi-pondération, graphe inclus | 269 776 |
| Multi + 16 vues scalaires | 271 912 |
| Multi + 16 scalarisations matérialisées | 664 488 |
| Scalaire après 100 repondérations | 97 664 |
| Scalaire neuf équivalent | 97 664 |

Les 16 vues ajoutent 2 136 octets, tableau de racines inclus ; les versions
matérialisées ajoutent 394 712 octets. Le dernier scalaire ne retient pas
l’historique de ses 100 repondérations. La mesure porte sur les objets Java
accessibles, pas sur la mémoire native, les métadonnées VM, le surcoût GC ni
un calcul de dominateurs de heap dump. Les allocations/op de JMH sont un autre
indicateur, conservé séparément.

## Rapports finaux

- [33 combinaisons pondérées](WEIGHTED.md), [JSON](weighted.json), [journal](weighted.log) : deux forks.
- [6 combinaisons avancées](ADVANCED.md), [JSON](advanced.json), [journal](advanced.log) : deux forks.
- [24 combinaisons de taille/alphabet/degré](SCALE.md), [JSON](scale.json), [journal](scale.log) : un fork, donc sans mesure de variabilité entre JVM.
- [Comparaison booléenne](UNWEIGHTED.md) : six combinaisons par version, deux forks.
- [Vérifications](verification.json), [environnement](environment.json), [API publique d’origine](original-public-api.txt), [classes de bibliothèque identiques dans le harnais](benchmark-library-bytecode.json), [empreintes des sources](source-manifest.sha256).
- [Build du module JMH](benchmarks-build.log), [liste des méthodes](benchmark-list.txt), [mémoire via le JAR autonome](memory-module.log).

Les 63 combinaisons pondérées/avancées/de dimensionnement sont complètes et
sans échec. La campagne booléenne passe de −7,7 % à +1,3 % selon le cas ; pas
de régression importante observée sur ce sous-ensemble, avec les limites de
précision et de couverture exposées dans le rapport.

La fixture DFA de `WeightedBench` lit toujours la première classe sortante :
elle favorise la recherche linéaire et ne démontre pas un avantage universel
de compilation. `ScaleBench` fait tourner les symboles sur toutes les classes
pour comparer les deux reconnaisseurs dans une autre distribution. Aucun
facteur d’accélération général n’est revendiqué à partir de ces seules données.
