# Extension pondérée d’Automaton 3.0 — audit et plan technique

## 1. Statut, périmètre et reproductibilité

Audit du dépôt `https://github.com/bebui/Automaton`, branche `master`, commit
`f7532436cbe964758988e13f8c5872c43bebe58b` (fusion de `release/3.0.0`).
Le `pom.xml` déclare **3.0.0**. Inspection effectuée le 20 octobre 2026,
heure de Paris, sur cette copie du code, sans utiliser la documentation 1.x/2.x.

**P0 terminé ; extension locale P1–P6 réalisée dans les familles décrites ci-dessous.**
L’audit initial est conservé dans `audit/automaton-3.0/` ; la validation de
l’extension est dans `audit/weighted-extension/` et son guide utilisateur dans
[WEIGHTED_AUTOMATA.md](WEIGHTED_AUTOMATA.md). Les algorithmes booléens, leurs
classes et tests, le POM et la CI restent inchangés. Le README est complété.

Le 20 octobre 2026, Julien a validé la suite : « vas y, et avance sans me demander
mon avis pour le moment tant qu’on ne pousse rien sur le git ». Cette autorisation
remplace la demande de validation intermédiaire pour **cette extension**.
Aucun push, publication ou modification de service n’est autorisé ni effectué.
Les propositions historiques des sections suivantes sont la trace de conception ;
la section 14 précise les réalisations et limites effectives.

L’instruction du projet dans `../AGENTS.md` impose : « Toujours attendre sa réponse
explicite écrite » lorsque des choix d’implémentation sont proposés. Le présent
document préparait cette validation avant P1, désormais accordée pour le travail local. Il ne constitue pas une autorisation
de modifier les services ni de publier une version.

Ont été lus intégralement : les 25 sources Java de production, les 7 sources de
tests, `README.md`, le POM, les deux descriptions de package, `.gitignore`, la CI
et les fichiers de licence. L’inventaire et les empreintes des fichiers suivis
figurent dans les pièces d’audit. Les Javadocs font partie des sources examinées.

## 2. Architecture constatée en version 3.0

### 2.1 Construction, mutation et identité

`Automaton` est mutable, non thread safe, et possède une `ArrayList<State>`, un
index initial (`-1` si absent) et un indicateur de déterminisme. Au plus un état
initial, plusieurs états acceptants. Les index sont contigus dans les constructions
normales, mais **ne sont pas stables** : `reIndex()` renumérote par BFS et retire
les états inaccessibles. `clone()` recopie états, transitions et étiquettes.

`State` utilise l’égalité d’identité et contient une
`LinkedHashMap<State, Transition>` indexée par destination. L’ordre d’insertion
est conservé. `State.addToTransitionMap()` **fusionne tous les arcs entre deux
états** : union des étiquettes, OU du drapeau ε. Il n’existe pas d’identifiant
public stable de transition ni de possibilité de conserver deux arcs parallèles
indépendants entre les mêmes extrémités.

`Transition` peut représenter un arc consommant, un arc ε, ou **les deux à la
fois**. `values == null` représente un arc purement ε ; un ensemble vide est
également possible. `getValues()` renvoie une copie. En revanche,
`Automaton.getStates()` et `State.getTransitions()` exposent réellement leurs
collections mutables : contrairement à une phrase du README, toute mutation
ne passe pas nécessairement par les méthodes de l’automate. Il n’existe aucun
compteur de modification permettant d’invalider sûrement une vue ou un cache.

Conséquences : ne pas indexer des poids durables par `State.getIndex()` sur un
automate mutable ; ne pas utiliser `Transition` comme clé ordinaire durable
(son `equals/hashCode` dépend du contenu et des index) ; ne pas hériter
d’`Automaton` pour ajouter des coûts aux arcs fusionnés.

### 2.2 Intervalles et ε

`IntervalSet` stocke déjà les bornes dans un `int[]` compact. Les intervalles
fermés sont triés, disjoints et non adjacents. `contains` utilise une recherche
binaire ; union/intersection/complément travaillent sur les bornes. `size()` est
un `long`, jusqu’à `2^32`. `ALL` et `EMPTY` sont gelés ; leurs clones sont mutables.
Les accès primitifs `intervalCount/minAt/maxAt/append` sont internes au package.

`Alphabet`, interne, partitionne les étiquettes en classes de symboles à partir
de coupures `long` : les extrêmes de l’alphabet sont traités sans les énumérer.
Son coût de construction est `O(I log I)` pour `I` intervalles. Attention à
l’expansion d’un arc en de nombreuses classes dans les algorithmes suivants.

`Automaton.run` suit directement le parcours d’un DFA. Pour un NFA, il manipule
des collections d’états et calcule la fermeture ε booléenne avant et après
chaque symbole. Les fermetures utilisent des parcours itératifs avec marquage,
donc terminent sur les ε-cycles. Elles ne calculent **aucune distance pondérée**.

### 2.3 Algorithmes et compilation déjà disponibles

| Fichier | Fonctionnalités existantes | Réutilisation envisagée |
|---|---|---|
| `Automaton.java` | Construction, reconnaissance DFA/NFA, copie, accessibilité/coaccessibilité, Graphviz, façades des opérations | Import instantané et conventions d’API ; aucun ajout de poids aux objets existants |
| `Interval.java`, `IntervalSet.java` | Intervalles entiers et opérations compactes | Réutiliser les opérations sur ensembles d’étiquettes |
| `Alphabet.java` | Classes symboliques par coupures | Réutiliser si nécessaire dans le package, sans rendre public cet utilitaire |
| `Operation.java` | Déterminisation par sous-ensembles, minimisation Valmari–Lehtinen et Brzozowski, union, concaténation, intersection, différence, complément, réversion, suppression ε, réduction | Réutiliser les idées de parcours et de représentation ; pas les transformations booléennes sur les coûts |
| `Bisimulation.java` | Réduction NFA Paige–Tarjan | Aucune réutilisation directe pour identifier des états pondérés |
| `CompiledDfa.java` | DFA immuable en tableaux `start/mins/maxs/dests`, reconnaissance par recherche binaire | Référence de disposition mémoire et de reconnaissance ; pas un import pondéré NFA |
| `Languages.java` | Produits accessibles, équivalence Hopcroft–Karp, inclusion par antichaînes, mot le plus court, finitude, comptage, tirage uniforme et énumération | Réutiliser la démarche symbolique et les oracles de tests |
| `Searcher.java` | Recherche leftmost-longest avec deux DFA compilés | Préserver ; extension pondérée non prioritaire |
| `regexp/*` | Parseur, construction de Thompson, répétitions, intersection et complément d’expressions | Préserver le parseur ; pas de nouvelle syntaxe pondérée à ce stade |

La déterminisation utilise déjà des tableaux primitifs d’adjacence et des buffers
de fermeture réutilisés. La minimisation et la détection des états vivants
emploient également des tableaux. `Languages.shortestWord` est un parcours 0–1
(ε coûte zéro symbole), **pas un plus court chemin à coûts arbitraires**.
`countWords`, `randomWord` et `words` possèdent des tables de programmation
dynamique sur longueurs, mais déterminisent et perdent donc l’identité des
parcours NFA. Il ne faut pas y injecter simplement des poids.

`CompiledDfa.of` déterminise automatiquement un NFA, éclate les ensembles en
intervalles triés et ne conserve pas la correspondance avec les arcs originaux.
Un import pondéré passant par cette méthode détruirait la sémantique recherchée.

Les opérations publiques produisent normalement de nouveaux automates. Union
et concaténation peuvent minimiser automatiquement ; les produits booléens
compilent/déterminisent leurs entrées. Ces automatismes seraient incorrects
pour les nouveaux types pondérés.

### 2.4 Points existants à prendre en compte, sans correction dans cet audit

- La Javadoc de `removeDeadStates()` parle d’inaccessibilité ; l’implémentation
  retire d’abord les transitions vers les états non coaccessibles puis appelle
  `reIndex()`. L’état initial peut rester seul, même non utile.
- L’indicateur de déterminisme est conservateur : réajouter un symbole vers la
  même destination peut le mettre à `false` bien que l’arc fusionné reste DFA.
  Les modifications directes des collections peuvent aussi le rendre périmé.
- `Interval.size()` retourne toujours un `int` et déborde sur de grands
  intervalles. Pour les nouveaux calculs, utiliser `IntervalSet.size()` ou
  `(long) max - min + 1`. Ne pas modifier la signature existante.
- `IntervalSet.compareTo` n’est pas un ordre total cohérent avec `equals`
  (mêmes minimum, maximum et cardinalité possibles pour des ensembles différents)
  et n’accepte pas les ensembles vides. Ne pas l’utiliser comme clé de
  canonicalisation des futures transitions.

Ces observations ne signifient pas qu’une régression a été introduite ; elles
décrivent la base à conserver. Toute correction séparée demanderait un test
ciblé et une justification distincte.

## 3. Build, tests et référence de performances

Java **17**, Maven ; seule dépendance déclarée : JUnit **4.13.2**, portée `test`.
Aucune dépendance de production. Compiler 3.13.0, Surefire 3.2.5, Jar 3.4.2,
Source 3.3.1, Javadoc 3.8.0. Profil de publication distinct `release`, non activé.
La CI lance `mvn clean install` sous Java 17. Aucun JMH, JaCoCo ou module de
benchmark n’est présent dans le dépôt audité.

Le Java par défaut de la machine est Java 8 ; les commandes d’audit sélectionnent
Java 17 **localement au processus**, sans changer la configuration système :

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH mvn -B test
```

Résultat : **47 tests, 0 échec, 0 erreur, 0 ignoré**, build réussi.
Le temps Maven initial inclut compilation et résolution des plugins et n’est
pas un benchmark. Voir `audit/automaton-3.0/tests.log`.
Un second appel `mvn -B -DskipTests verify` réussit également : JAR principal,
sources et Javadocs générés. Il ne compte pas comme une seconde exécution des
tests ; son journal est `audit/automaton-3.0/build.log`.

Les tests couvrent notamment 2 000 NFA aléatoires comparés à une simulation
indépendante, 5 000 jeux d’intervalles comparés à `BitSet`, deux campagnes de
250 automates × 8 000 mots, et un interpréteur indépendant d’expressions
régulières confronté à l’énumération exhaustive des mots courts. Les graines
sont fixes. La couverture est substantielle pour le booléen ; aucune couverture
pondérée, numérique ou de Pareto n’existe. Aucun taux de couverture n’a été mesuré.

Le harnais JMH d’audit est compilé **hors du projet**, contre les classes 3.0
inchangées. Le source reproductible, les paramètres, les résultats bruts, les
allocations par opération et les limites de la mesure sont conservés dans
`audit/automaton-3.0/BASELINE.md`. Cette référence est locale et préliminaire :
machine partagée, petits graphes, échauffement court. Elle permet une comparaison
appariée ultérieure, pas une promesse générale de performance. La mémoire
allouée par opération ne mesure pas à elle seule la mémoire retenue d’un graphe.

Campagne achevée : **39 combinaisons**, **2 forks** et **10 échantillons mesurés**
par combinaison, sans erreur. La [table complète](audit/automaton-3.0/RESULTS.md)
fournit les temps, erreurs et allocations ; le JSON conserve les mesures brutes.
À titre de repère sur ces seules fixtures, lire un mot de 64 symboles prend
2,107 à 16,102 µs avec `run` et 1,082 à 2,572 µs avec `compiledRun`.
Ces plages correspondent à quatre jeux différents ; elles ne sont pas des
intervalles de confiance ni une comparaison indépendante de la taille des graphes.
À la fin de P0, les 38 empreintes de fichiers suivis étaient identiques à celles
du début d’audit. L’extension modifie ensuite uniquement la documentation parmi
ces fichiers ; les empreintes des sources/tests/POM/CI restent identiques.

## 4. Sémantique proposée et contrats numériques

### 4.1 Objets mathématiques et résultats

Un chemin est une suite d’**identifiants d’arcs**, avec ses états et ses symboles
consommés ; ε n’ajoute aucun symbole. Un mot est seulement la suite des symboles.
Un arc portant plusieurs entiers a un coût constant pour tous ses symboles.
Un chemin d’arcs peut donc représenter plusieurs mots : une reconstruction
retourne aussi un choix concret de symboles, le minimum de l’étiquette par
défaut lorsque le mot n’est pas imposé.

Pour un chemin acceptant `p` : `C(p) = initial + somme(arcs) + final(dernier)`.
Poids initial et finaux nuls par défaut. Conserver un unique état initial et
des états finaux multiples ; la réversion/union peut créer un nouvel initial
et des arcs ε pour représenter plusieurs départs pondérés.

`cost(w) = inf { C(p) | p accepte w }` ; ensemble vide : `+Infinity`.
`optimalCost()` considère tous les chemins acceptants, indépendamment du mot.
Un cycle améliorant donne parfois un **infimum `-Infinity` sans chemin optimal**.
Il faut distinguer les statuts `FINITE`, `UNREACHABLE`, `UNBOUNDED`, et réserver
la reconstruction aux résultats finis. Un cycle négatif inaccessible ou sans
continuation acceptante ne rend pas l’optimum global non borné. Pour un mot,
la continuation doit accepter **le suffixe exact restant**.

En multi-pondéré, un chemin possède un vecteur additif ; un mot NFA possède
un **ensemble** de vecteurs. Aucun minimum composante par composante artificiel
ne doit être retourné comme s’il provenait d’un parcours réel. Une requête
Pareto globale et une requête Pareto pour un mot sont distinctes.

### 4.2 Valeurs flottantes

Proposition : accepter seulement les poids, composantes, coefficients et coûts
de frontière **finis** ; rejeter `NaN` et les deux infinis en entrée. Présence
initiale/acceptante stockée séparément ; `+Infinity` n’est pas un arc désactivé.
Réserver les infinis de sortie à l’absence et à la non-bornitude. Normaliser
`-0.0` en `+0.0` pour la canonicalisation ; comparer exactement les poids des
arcs avant toute fusion, sans seuil arbitraire.

Vérifier additions et produits : débordement fini → infini provoque une
`ArithmeticException`, jamais un faux résultat « inaccessible » ou « non borné ».
Les sentinelles sont traitées avant l’addition (`+∞ + -∞` ne doit jamais arriver).
Ne pas employer `Double.MAX_VALUE` comme sentinelle. Vérifier aussi les tailles
`T*V`, `E*d`, compteurs et offsets avant allocation.

**Limite numérique explicitée dans le contrat retenu :** l’addition IEEE 754 n’est pas
associative. Réversion, distances arrière, scalarisation et normalisation des
poids ne peuvent garantir une égalité bit à bit de toutes les sommes de `double`.
Une simple comparaison stricte dans Bellman–Ford ne fournit pas à elle seule
une preuve du signe réel d’un cycle de coût presque nul.

Contrat recommandé : sémantique mathématique additive, résultats `double`
arrondis documentés ; garanties exactes démontrées sur des données dont les
sommes sont exactement représentables (par exemple entiers avec toutes les
sommes intermédiaires dans `[-2^53, 2^53]`). Les tests flottants évaluent également
l’erreur, sans tolérance utilisée pour fusionner des états ou des résidus.
Pour prétendre à une certification sur **tous** les `double` finis, il faudrait
une arithmétique exacte des dyadiques ou un raffinement certifié : coût et API à
évaluer séparément. Un mode approché ne devra pas être présenté comme certifié.
Les transformations avancées seront limitées aux domaines certifiés ; un simple
budget d’itérations ne prouve ni correction ni impossibilité de déterminisation.

## 5. Architecture proposée : graphe partagé et pondérations séparées

### 5.1 Comparaison des options après inspection

| Option | Intérêt | Limite constatée | Proposition |
|---|---|---|---|
| Ajouter un poids dans `Transition` | API familière | Coût mémoire pour le booléen ; arcs parallèles fusionnés ; impossible de partager plusieurs pondérations | Écarter |
| Vue vivante sur `Automaton` mutable | Aucun instantané initial | Collections exposées, index variables, aucune invalidation fiable, mêmes fusions | Écarter pour une vue pérenne |
| Cloner un `Automaton` par pondération | Simple | Duplique états/étiquettes et conserve le mauvais modèle d’arc | Écarter |
| Structure immuable dédiée, importée une fois ou construite directement, poids en tableaux/vues | Arcs parallèles, identifiants stables, CSR partagé, calculs primitifs | Coût d’import initial et nouveau constructeur | **Recommandation à valider** |

Le partage est garanti **entre toutes les pondérations d’une même structure**.
Il ne s’agit pas d’une promesse de partage physique sûr avec les collections
mutables de l’automate d’origine. L’import paie une seule copie structurelle ;
la modifier ensuite ne modifie pas l’instantané pondéré, et réciproquement.

### 5.2 Types et stockage

Nouveaux types publics envisagés dans `fr.menana.automaton`, afin de réutiliser les
accès primitifs internes d’`IntervalSet` sans élargir leur visibilité :

| Type proposé | Responsabilité |
|---|---|
| `AutomatonGraph` et son `Builder` | Structure immuable, états et arcs identifiés par `int`, un état initial, ensemble final, arcs symboliques ou ε |
| `WeightedAutomaton` | Façade scalaire immuable ; graphe partagé, poids initiaux/finaux et par arc ; `of(graph, weights)`, `cost`, `bestPath` |
| `MultiWeightedAutomaton` | Même graphe, dimension fixe strictement positive, vecteurs de frontière et d’arcs, projections et scalarisations |
| `WeightedPath` / `MultiWeightedPath` | Identifiants d’arcs, états, mot concret et coût ; tableaux copiés ou lecture indexée immuable |
| `ShortestPathResult` | Distances, statuts par état, prédécesseurs optionnels et état final choisi ; reconstructions à la demande |
| `WeightedPaths` / `WeightedOperations` | Algorithmes et constructions pondérées, pas d’appel automatique aux opérations booléennes |
| `LayeredAutomaton` | Déploiement structurel réutilisable pour un horizon et éventuellement des domaines de symboles |
| `ParetoResult` | Vecteurs non dominés, témoins et statut exact/complet explicite |

Les petits adaptateurs primitifs d’accès aux poids et les buffers de calcul
restent internes tant qu’un besoin public concret ne justifie pas leur exposition.
Éviter un framework de graphes génériques, les génériques numériques, les
semi-anneaux et les hiérarchies de stratégies prématurées.

Structure : tableaux `source[E]`, `target[E]`, étiquettes compactes privées et
type d’arc ; `outStart[V+1]`, `outEdgeIds[E]` en CSR. Index inverse
`inStart/inEdgeIds` construit une fois pour les requêtes arrière. Chaque arc
possède un ID logique d’insertion stable, indépendant de sa position dans CSR.
Les optimisations ou produits créant un autre graphe fournissent une correspondance
d’IDs lorsqu’elle est nécessaire ; les IDs ne sont pas globaux entre graphes.

Un arc est **soit** ε, **soit** symbolique non vide. L’import d’une transition
3.0 mixte crée deux arcs indépendants avec une correspondance explicite
`(Transition d’origine, mode ε/symbole)`. Il ne peut retrouver des doublons déjà
fusionnés avant import : construire directement le graphe pour ces cas.
Valider l’appartenance et la cohérence des états/arcs à l’import ; ne pas appeler
`reIndex`, `determinize`, `minimize` ou `compile` sur les entrées.

Les étiquettes sont copiées à l’entrée et protégées à la sortie. Un stockage
initial par `IntervalSet` privés est possible ; comparer un tableau de bornes
aplati avant d’en imposer la complexité. Un seul stockage d’étiquettes est partagé
par toutes les pondérations. Prétraitements structurels : ordre topologique,
accessibilité, coaccessibilité et index inverse ; aucune dépendance aux poids.

Scalaire : `double[E]`, coût initial scalaire, coûts finaux indexés par état.
Multi : tableau aplati par arc `double[E*d]`, plus frontières ; accès séquentiel
aux composantes. Copie défensive lors de la construction des pondérations.
Changer les poids crée une nouvelle pondération du **même** objet graphe.

Les automates immuables sont partageables entre threads ; un espace de travail
réutilisable appartient à un seul appelant/thread. Les résultats durables ne
doivent pas être écrasés lors de sa réutilisation : transférer les tableaux à
un résultat immuable ou les copier explicitement. Le chemin rapide « coût seul »
peut réutiliser intégralement ses buffers sans allocation de résultat détaillé.

`multi.scalarize(coefficients)` retourne une façade scalaire partageant graphe
et vecteurs, copie seulement les `d` coefficients et calcule les produits
scalaires à la demande. Coût de création `O(d)`, accès `O(d)`. Une matérialisation
optionnelle coûte `O(E*d)` et occupe `O(E+V)` ; aucun cache global ou silencieux.
La projection d’une composante est une vue `O(1)` par poids. Les coefficients
négatifs peuvent produire des arcs négatifs, imposant DAG ou Bellman–Ford.
Même avec des coefficients positifs, vérifier les poids agrégés réels avant
d’utiliser Dijkstra ; ne pas supposer les vecteurs d’origine positifs.

Exemple conceptuel d’API, **non encore disponible** :

```java
var builder = AutomatonGraph.builder();
int s0 = builder.addState();
int s1 = builder.addState();
builder.setInitial(s0);
builder.setAccept(s1);
int e0 = builder.addTransition(s0, s1, new Interval(0, 9));
AutomatonGraph graph = builder.build();
WeightedAutomaton a = WeightedAutomaton.of(graph, new double[] {3.5});
double cost = a.cost(4);
```

Un constructeur pondéré de commodité pourra collecter structure et poids dans
le même builder. Il doit garder la même représentation finale et autoriser les
arcs parallèles, sans obliger l’utilisateur à manipuler des tableaux pour chaque
cas simple. Le nommage final suivra la validation de cette conception.

## 6. Algorithmes scalaires et programmation dynamique

Notations : `V` états, `E` arcs logiques, `I` intervalles, `L` symboles d’un mot,
`Eε` arcs ε, `T` horizon. Les complexités supposent les poids disponibles en
`O(1)` ; multiplier les accès par `d` pour une scalarisation non matérialisée.
Un test d’appartenance à une étiquette de `k` intervalles coûte `O(log k)`.

| Calcul | Préconditions et traitement | Temps / mémoire auxiliaire cible |
|---|---|---|
| Tri topologique | Kahn itératif ; tous les arcs, y compris ε ; rejeter un cycle, ne pas retourner un ordre partiel | `O(V+E)` / `O(V)` |
| Distances DAG avant/arrière, min/max | Poids signés finis ; ordre topologique inversé pour arrière ; poids de frontière inclus une fois | `O(V+E)` / `O(V)` |
| Meilleur passage par état/arc | DAG : `f[v]+b[v]`, `f[u]+w[e]+b[v]` ; ignorer les sommets inaccessibles | Après distances : `O(V+E)` / `O(1)` par requête |
| Dijkstra | Tous les arcs considérés non négatifs ; tas binaire primitif indexé ; coût initial éventuellement négatif pour l’unique source | `O((V+E) log V)` / `O(V)` |
| Bellman–Ford | Poids signés ; relaxation stricte ; propagation depuis les améliorations de la passe supplémentaire | `O(V*E)` / `O(V)` |
| États utiles | Intersection des ensembles accessible et coaccessible ; indépendante des coûts | `O(V+E)` / `O(V)` |
| Coût d’un mot DFA sans ε | Validation structurelle réelle du déterminisme ; parcours direct ; index d’intervalles optionnel | `O(L*t*log k)` sans index, puis `O(L*log J)` avec `J` intervalles sortants / `O(1)` |
| Coût d’un mot NFA sans ε | Deux tableaux `double[V]` alternés, minimum par état | `O(L*(V+E*log k))` / `O(V)` |
| Coût NFA avec ε | Fermeture pondérée initiale puis après chaque symbole ; Bellman–Ford sur ε et propagation de `-∞` | `O((L+1)*V*(Eε+1)+L*(V+E*log k))` / `O(V)` |
| Longueur exactement/au plus `T`, sans ε | DP par couches, transitions consommantes uniquement ; tous les symboles d’un arc ont le même coût | `O(T*(V+E))` / `O(V)` pour le coût ; `O(T*V)` avec témoins |
| Horizon avec ε | Fermeture pondérée dans chaque couche ; ε ne change pas la position | Ajouter `O((T+1)*V*(Eε+1))` au pire |

Les distances vers une destination sont calculées sur l’adjacence inverse.
Les distances vers l’acceptation démarrent des coûts finaux ; si Dijkstra arrière
emploie plusieurs graines négatives, les décaler par une constante commune ou
utiliser une initialisation multi-source justifiée, sans introduire naïvement
des arcs négatifs dans un Dijkstra standard.

Les formules de passage par état/arc s’étendent aux graphes cycliques pour les
parcours autorisant les répétitions, après calcul correct des distances et de
leurs statuts. Vérifier d’abord que les deux côtés existent ; sinon le passage
est impossible même si l’autre côté est non borné. Si les deux existent et
l’un est non borné, le coût conditionnel est `-∞`. Ne jamais évaluer directement
une expression mêlant les deux sentinelles infinies.

Bellman–Ford marque `UNBOUNDED` uniquement les états accessibles depuis un cycle
négatif accessible à la source. La réduction aux états coaccessibles est utile
pour l’optimum acceptant, mais ne doit pas supprimer des distances par état que
l’appelant a demandées. Fournir un témoin de cycle lorsqu’une certification est
possible dans le contrat numérique retenu. Ne pas reconstruire un prétendu
chemin optimal de coût `-∞`.

Pour un mot avec ε, conserver séparément la propagation des états déjà `-∞` et
la recherche de nouveaux cycles dans les fermetures. Une branche non bornée peut
disparaître au symbole suivant : elle ne condamne pas les autres branches.
La reconstruction se fait sur `(position, état)` avec témoins par relaxation
ou une seconde passe, afin que les prédécesseurs ε ne forment pas un cycle à
cause des égalités. Pas de chemin construit dans `cost`.

Pour `T=0`, seuls les chemins ε et les frontières participent. Pour « au plus T »,
minimiser sur toutes les couches finales de 0 à T ; ne pas ajouter implicitement
des symboles de remplissage. Les longueurs négatives sont rejetées. « Meilleur
mot de longueur T » retourne le mot d’un chemin globalement minimal : cela ne
constitue pas une énumération de mots distincts triés par coût. Un plus long
chemin dans un graphe quelconque ne fait pas partie du premier lot ; les cycles
positifs et la distinction entre parcours et chemins simples exigent une autre API.

## 7. Opérations pondérées préservant les coûts

Les résultats sont de nouveaux graphes et pondérations ; les opérandes et leurs
tableaux ne sont jamais modifiés. Conserver les parcours parallèles par défaut.

| Opération | Construction et sémantique | Complexité principale |
|---|---|---|
| Union | Copie disjointe ; nouvel initial ; arcs ε portant les poids initiaux de chaque branche ; coût initial du résultat nul ; coûts finaux conservés | `O(Va+Vb+Ea+Eb)` hors copie des bornes |
| Concaténation | Initial/coût initial de A ; chaque final `f` de A relié en ε à l’initial de B avec `finalA[f]+initialB` ; ces fins intermédiaires ne sont plus finales ; fins de B conservées | Linéaire en entrées et nombre de finals A |
| Intersection synchronisée | Paires accessibles ; intersection d’`IntervalSet`, somme des coûts d’arcs et frontières ; accepter si les deux acceptent | Paires d’arcs sortants examinées et coût d’intersection des bornes ; états au pire `Va*Vb` |
| Réversion | Inverser les arcs ; nouvel initial de poids zéro vers chaque ancien final via ε de coût final ; ancien initial devient final avec l’ancien coût initial | `O(V+E)` hors bornes |
| Étoile | Nouvelle entrée/sortie acceptante de coût zéro ; entrer dans une copie avec son coût initial ; après chaque acceptation, sortir ou recommencer en payant le coût final et le prochain coût initial | Linéaire ; peut créer un ε-cycle améliorant |
| Répétition bornée | Copies de fragments avec comptage des répétitions, en conservant tous les découpages | Proportionnel aux copies/arcs produits ; valider les limites |
| Suppression des inutiles | Accessible ET coaccessible, suppression sans modifier les chemins acceptants survivants | `O(V+E)` |
| Fusion d’étiquettes | Même source, destination, mode et coût **exactement égal** ; union des étiquettes | Tri/regroupement puis coût de fusion des bornes |

Avec ε, l’intersection doit permettre des déplacements ε d’un seul opérande
sans consommation, en plus des paires d’arcs consommants synchronisées.
Un filtre d’entrelacement ε est nécessaire si l’API veut préserver le nombre
de parcours ou les K meilleurs chemins ; un produit non filtré peut dupliquer
des parcours équivalents tout en préservant l’infimum scalaire. Première livraison
possible : produit sans ε avec rejet explicite sinon ; extension filtrée dans
le lot suivant. Aucune précondition ne doit rester implicite.

Sur des arcs parallèles de mêmes extrémités et même étiquette, garder seulement
le moins cher préserve le coût minimal de chaque mot, mais détruit des chemins.
Une telle simplification doit être **explicitement distincte** d’un nettoyage
préservant tous les parcours. La fusion d’étiquettes égales en coût conserve la
fonction de coût, mais change aussi les IDs ; fournir les correspondances utiles.
Une réduction dépendant de poids actuels ne reste pas valide sous toute future
repondération : elle produit un nouveau graphe lié à ces poids.

La suppression ε scalaire se fera seulement si les distances ε nécessaires
sont finies et la correction démontrée ; le résultat peut grossir quadratiquement
ou davantage selon la représentation. Ne pas remplacer une fermeture pondérée
par `Operation.removeEpsilon`. Pas de complément/différence pondérés prévus.

## 8. Déploiement sur horizon et évaluations répétées

`LayeredAutomaton` conserve des correspondances `(position, state)` et
`(position, edgeId)`, l’accessibilité avant et la coaccessibilité arrière pour
l’horizon exact et les domaines éventuels. Construction en `O(T*(V+E))` sans ε,
stockage du graphe déployé en `O(VT+ET)` après élagage. Pour une requête de coût
simple, préférer une DP implicite en deux couches pour éviter ce stockage.

L’élagage est **structurel**, pas fondé sur des coûts provisoires, et reste
réutilisable après changement de poids. Un objet d’évaluation reçoit les poids
ou une fonction primitive `weight(position, edgeId)` ; même structure, ordre et
buffers réutilisés. Le domaine des symboles et la convention de longueur sont
fixés dans la structure. Une mutation de ces domaines exige une reconstruction.

ε reste dans la même couche : même à horizon fini, le graphe peut être cyclique.
Employer les fermetures pondérées par couche ou une élimination ε prouvée ;
aucun tri topologique de l’ensemble ne sera supposé sans vérification.

Les coûts dépendant seulement de la position et de l’arc conservent la compacité
des intervalles. Un coût dépendant du **symbole à l’intérieur d’un intervalle**
exige un découpage symbolique par coûts constants, ou un oracle de minimum
documenté ; ne pas promettre cette généralité avec un seul `double` par arc.

Les distances avant/arrière donnent le coût conditionnel à un état ou à un
symbole en position `p` en minimisant sur les arcs compatibles. Inclure les
frontières une fois et préciser le statut lorsque l’une des parties est non
bornée ou inaccessible. Reconstruire un mot à partir des arcs et d’un symbole
compatible par position ; conserver la trace des ε sans allonger le mot.

## 9. Multi-pondération, lexicographie et Pareto

Dimension fixe et vérifiée à toutes les entrées. Addition des frontières et des
arcs composante par composante ; buffers `double[]` réutilisables pour évaluer un
parcours donné. Projection, permutation/sélection de dimensions et combinaison
linéaire utilisent des vues tant que cela ne complique pas inutilement l’API.
Les vues scalarisées ne copient ni états, ni arcs, ni étiquettes.

Optimisation lexicographique d’abord **sur DAG** : comparer les vecteurs dans
l’ordre de priorité des dimensions, première différence stricte ; une étiquette
optimale par état suffit car l’ordre est compatible avec l’addition dans le
contrat numérique admis. Temps `O(d*(V+E))`, mémoire `O(d*V)` plus témoins.
Ne pas simuler la lexicographie avec des coefficients « très grands ».

Dominance pour minimisation : `x` domine `y` si `x[i] <= y[i]` pour toute
dimension et au moins une inégalité stricte. Égalité : un seul représentant
par vecteur dans le résultat exact par défaut. Parcours identifiés par arcs,
pas uniquement par mots. La politique de départage est stable, sans promesse
de plus petit mot lexicographique parmi toutes les égalités.

Propagation topologique sur DAG : étendre chaque étiquette vers les successeurs,
éliminer les dominées à destination, conserver parent et arc pour les témoins.
À l’acceptation, **ajouter les coûts finaux avant de comparer globalement**.
Pour un mot imposé, travailler sur ses couches et ses arcs compatibles ; sans
cela, on confondrait des vecteurs de mots différents.

Avec `A = somme_sur_arcs(u,v) |front(u)|` candidats et au plus `M` étiquettes par
front, une liste simple coûte `O(d*A*M)` au pire ; la mémoire inclut les
étiquettes vivantes **et les ancêtres conservés pour reconstruction**.
`M` peut être exponentiel même sur DAG. Commencer par des listes/tableaux
compacts ; mesurer ensuite les fronts 2D triés, puis les index 3D. Une structure
sophistiquée ne supprime pas l’explosion du nombre de solutions.

« Tous les chemins par vecteur » exige un DAG de prédécesseurs multiples et une
énumération explicite, éventuellement paresseuse. Sur DAG le nombre de chemins
est fini mais peut être exponentiel ; sur un graphe cyclique, même un cycle nul
donne une infinité de chemins égaux. Pas d’énumération exhaustive implicite.

Les variantes ε-dominance, budget de temps/étiquettes, front plafonné et recherche
de compromis arrivent après la version exacte. Les résultats portent un statut
incomplet/approché et les paramètres réellement utilisés ; aucun tronquage
silencieux. Une scalarisation peut manquer des points Pareto non supportés et
n’est donc pas présentée comme un calcul du front complet.

Opérations multi-pondérées : union, concaténation, réversion et simplifications
sûres conservent la **relation complète mot → vecteurs de parcours**, pas
seulement son front. Un filtrage Pareto est une requête distincte.
Deux compositions synchronisées seront nommées explicitement :

- **Addition de vecteurs** : mêmes dimensions et même sémantique de ressources,
  résultat de dimension `d`, frontières et arcs additionnés.
- **Concaténation de vecteurs** : ressources indépendantes, résultat de dimension
  `da+db`, blocs gauche/droit conservés. Dans une concaténation de langages,
  étendre les arcs d’une branche avec des zéros pour les dimensions de l’autre.

La projection peut rendre deux vecteurs égaux ou dominés ; elle conserve tous
les parcours tant qu’un filtrage n’a pas été demandé. Un pruning valable pour
minimisation n’est pas valide pour toutes les scalarisations à coefficients
négatifs ; ne jamais le réutiliser silencieusement dans ce contexte.

## 10. Algorithmes avancés : périmètre conditionnel

La déterminisation par poids résiduels ne termine pas pour tous les automates
pondérés. La propriété des jumeaux donne des conditions de déterminisabilité ;
les implications exactes dépendent de la famille, notamment de l’ambiguïté.
Référence primaire : [Mohri, *Weighted Automata Algorithms*, §6.2–6.4](https://cs.nyu.edu/~mohri/pub/hwa.pdf).
Cette étude n’introduit aucune API générique de semi-anneaux dans la bibliothèque.

Proposition de progression : DFA déjà déterministe → retour d’une pondération
équivalente ; puis NFA acyclique sans ε avec domaine numérique certifié, dont
l’exploration des résidus est finie, mais potentiellement exponentielle ; puis
seulement les familles cycliques munies d’un test suffisant justifié. Une borne
de ressources peut arrêter un calcul avec un statut explicite, jamais annoncer
« non déterminisable » simplement parce que le budget est épuisé. Comparaison
approximative de résidus interdite pour prétendre préserver exactement les coûts.

Minimisation : commencer par un partage de suffixes structurellement identiques
sur DFA acycliques (mêmes poids finaux, mêmes étiquettes, poids et classes de
destination). C’est une réduction sûre, **pas une minimisation pondérée générale**.
La normalisation par potentiels puis le quotient demandent une preuve de
préservation et un domaine numérique approprié. Ni Hopcroft booléen ni
Brzozowski booléen ne suffisent à préserver une fonction de coûts.

K meilleurs chemins : première famille, DAG et horizon sans ε-cycles. Conserver
les K meilleurs suffixes avec parents et fusion des candidats ; prototype
simple `O(K*E*log(K*E))` temps, `O(K*V)` étiquettes hors mots reconstruits,
puis mesurer une file de fusion plus spécialisée. Les égalités de coût
conservent des chemins distincts par arcs. La taille des mots reconstruits
fait partie du coût de sortie. Sur graphe cyclique, préciser si l’on cherche
des parcours avec répétition ou des chemins simples avant toute implémentation.
Les K meilleurs **mots distincts** constituent un autre problème (ambiguïté NFA).

Meilleurs préfixes/suffixes et coût sous préfixe sont des extensions naturelles
des distances/DP. Une compilation pondérée DFA pourra ajouter un ID d’arc à
chaque intervalle trié, référencer la pondération séparée et lire sans allocation.
Elle ne déterminise jamais un NFA implicitement. Énumération générale par coût
croissant reportée tant que finitude, égalités et cycles ne sont pas spécifiés.

## 11. Validation indépendante, benchmarks et non-régression

Chaque algorithme recevra ses tests propres avant l’étape suivante : cas DFA/NFA,
arcs parallèles, intervalles disjoints/chevauchants, ε purs et import mixte, mots
ambigus, automate vide/sans initial, états inaccessibles/non coaccessibles,
frontières non nulles, coûts signés/nuls, cycles nuls et améliorants pertinents
ou non, `T=0`, extrêmes `int`, dimensions invalides, entrées non finies et
débordements. Tester égalités exactes et quasi-égalités séparément.

Oracles : énumération exhaustive de chemins sur petits DAG, multiensembles de
vecteurs puis filtrage par double boucle indépendante ; chemins bornés pour les
cas cycliques, sans en déduire abusivement une preuve de non-bornitude. Pour
les cycles négatifs, examiner indépendamment les cycles simples et les ensembles
d’accès sur de petits graphes. Utiliser entiers/dyadiques exactement représentables
et un oracle `BigDecimal(double)` pour les contre-exemples flottants ; ne pas
confondre l’écriture décimale courte d’un double avec sa valeur binaire exacte.

Propriétés : reconstruction égale au coût annoncé, appartenance des symboles
aux arcs, longueur consommée exacte, coûts conservés par réversion/nettoyage,
union minimum par mot, concaténation minimum sur tous les découpages,
intersection somme par mot sur le domaine fini puis tests spécifiques aux
branches non bornées, projection/scalarisation des **mêmes chemins**, non-mutation
des opérandes et identité de graphe partagée entre les vues. Pour les flottants,
séparer assertions exactes sur invariants et bornes d’erreur sur sommes.

Maintenir les 47 tests 3.0 et le build des Javadocs. Comparer les signatures
publiques existantes à un manifeste `javap` de la base et vérifier que les
classes booléennes ne portent aucun champ poids/buffer ajouté. Ajouter des tests
d’import face aux collections mal formées plutôt que modifier globalement leur API.

JMH restera une dépendance du harnais/profil de benchmark, jamais du JAR de
production. Documenter graine, dimensions réelles (avant/après fusion), taille
d’alphabet, intervalles, DAG/cycles, DFA/NFA, horizon et nombre de points Pareto.
Comparer coût seul/reconstruction, construction/import/réutilisation, vues et
scalarisations matérialisées. Mesurer `gc.alloc.rate.norm` et GC ; mesurer la
mémoire retenue séparément (par exemple JOL dans un outil optionnel) avant de
revendiquer un gain mémoire structurel.

Matrice cible : graphes creux/denses, 10² à 10⁵ états selon faisabilité, degrés
variés, alphabets petits et très larges, intervalles unitaires et fragmentés,
ε-cycles, dimensions 1/2/3/8/32, horizons croissants et fronts avec faible/forte
dominance. La référence P0 est un sous-ensemble réalisable de cette matrice,
pas la couverture complète de charges encore inexistantes.

Pour les décisions de performance : au moins 3 forks et échauffement allongé
après stabilisation, même machine/JDK/heap, mesures base et candidat alternées,
résultats bruts et dispersion conservés. Un ralentissement médian reproductible
au-delà de 10 % sur une opération booléenne ou une hausse mémoire doit être
investigué, pas automatiquement accepté ou refusé sur une mesure bruitée.
Le seuil est une proposition de politique de revue, pas un résultat mesuré.

## 12. Lots de réalisation proposés et critères de passage

| Priorité / phases demandées | Lot cohérent | Critère de validation avant le lot suivant |
|---|---|---|
| P0 / phase 0 | Audit intégral, tests 3.0, référence JMH, présent plan | Pièces reproductibles et validation écrite des choix ci-dessous |
| P1a / phases 1 et 3 sans ε | Graphe immuable/builder/import, poids finis et frontières, arcs parallèles, évaluation DFA/NFA et témoin | Oracles exhaustifs par mot, copies défensives, partage de graphe ; tests booléens inchangés |
| P1b / phase 2 DAG | Tri, distances min/max avant/arrière, utiles, passages conditionnels et reconstruction | Poids négatifs, cas vides, vérification indépendante de tous les petits chemins DAG |
| P1c / phases 2 et 3 avec cycles | Dijkstra, Bellman–Ford, statuts de non-bornitude et fermetures ε pondérées | Cycles pertinents/non pertinents, suffixes exacts d’un mot, politique numérique documentée et testée |
| P2a / phase 4 | Union, concaténation, réversion, répétition et nettoyage sûr | Coûts de tous les mots courts et frontières vérifiés indépendamment |
| P2b / phase 4 | Produit synchronisé sans ε puis filtre ε, élimination ε sous hypothèses | Relations de parcours annoncées préservées ; préconditions et limites explicites |
| P3 / phases 2 longueur et 5 | DP exacte/au plus T, déploiement élagué, coûts positionnels, distances conditionnelles et buffers | Même structure réévaluée sous plusieurs poids ; ε sans consommation ; benchmark réutilisation/construction |
| P4 / phase 6 | Vecteurs, projections, scalarisation vue et matérialisée, lexicographie DAG | Même graphe par identité, somme par chemin, coefficients négatifs et allocation mesurés |
| P5a / phase 7 | Pareto exact DAG et témoins, par mot et global, égalités | Front comparé à l’énumération exhaustive indépendante ; aucun résultat artificiel composante par composante |
| P5b / phases 7 et 8 | Opérations multi, deux compositions de vecteurs, énumération explicite des témoins ; approximations seulement ensuite | Relation mot/vecteurs préservée ; distinction exact/incomplet/approché visible |
| P6 / phase 9 | K chemins sur DAG/horizon, réduction sûre DFA DAG, compilation ; étude puis familles certifiées pour déterminisation/minimisation | Hypothèses prouvées, oracles et budgets explicites ; aucune généralisation non justifiée |

À chaque lot : vérifier l’existant, écrire les contrats et invariants dans les
Javadocs, implémenter le périmètre validé, ajouter les tests indépendants,
exécuter toute la suite et le packaging/Javadoc, lancer les mesures ciblées et
le sous-ensemble booléen, corriger les régressions, puis actualiser ce plan avec
ce qui est réellement livré. Aucun lot n’est déclaré livré par cette feuille de route.

## 13. Risques et décisions de conception validées

| Risque | Mesure proposée |
|---|---|
| Perte de parcours par fusion booléenne | Nouveau multigraphe pondéré ; aucun passage par une minimisation booléenne |
| Vue périmée sur un automate mutable | Instantané explicite à l’import, copies défensives et IDs locaux stables |
| Régression sur le booléen | Extension additive ; aucun poids dans `State`/`Transition`, tests et benchmarks conservés |
| Faux `-∞` ou perte de précision | Contrat numérique explicite, overflow rejeté, limites de certification documentées |
| Explosion produits/horizons/Pareto/résidus | Parcours accessibles, taille vérifiée, résultat incomplet explicite si budget demandé |
| Caches invalides sous repondération | Prétraitement structurel partagé ; distances, dominance et caches de poids attachés à chaque pondération |
| API trop étendue | Façades concrètes et helpers internes ; exposer chaque fonctionnalité avec le lot qui la teste |

Base validée par Julien, avec autorisation de poursuivre ensuite les autres lots localement :

1. Structure immuable dédiée partageable, import instantané de 3.0, arcs
   parallèles et poids séparés ; aucune refonte du moteur booléen.
2. Poids et coefficients finis ; frontières nulles par défaut, infinis de
   résultats distinguant absence et non-bornitude ; débordements rejetés.
3. Calcul en `double` avec limites d’arrondi explicites ; aucune certification
   générale de cycles ou transformation avancée sans domaine numérique adéquat.
4. Premier périmètre : construction/import, coût de mots sans ε et algorithmes
   DAG ; ε et cycles dans le lot P1c, puis opérations et autres phases selon
   les critères ci-dessus.

Hors périmètre confirmé : relaxation lagrangienne, RCSPP, PPC/Choco, framework
de semi-anneaux, API d’algèbre tropicale et méthodes propres aux plannings.


## 14. État de la réalisation locale

Les classes ajoutées sont dans `fr.menana.automaton` ; aucune surcharge numérique
n’a été ajoutée aux objets booléens existants et aucune dépendance de production
n’a été introduite. Les fonctionnalités ci-dessous sont implémentées, et non
seulement proposées.

| Lot | Réalisation effective |
|---|---|
| P1 | `AutomatonGraph` immuable, multiarcs et IDs stables, CSR avant/arrière, labels aplatis, import sans déterminisation ; `WeightedAutomaton`, frontières, évaluation DFA/NFA et ε, évaluateurs réutilisables ; DAG min/max, Dijkstra, Bellman–Ford, statuts et témoins, distances avant/arrière et coûts de passage |
| P2 | Union, concaténation, produit synchronisé avec filtre ε canonique, réversion, étoile et répétition, trimming, fusion de labels sûrs, élimination ε scalaire avec rejet des cycles négatifs pertinents |
| P3 | DP implicite à longueur exacte/maximum, `LayeredAutomaton` exact/maximum/mot fixé avec seuls nœuds utiles, repondération sans reconstruction, coûts positionnels figés, analyse avant/arrière, coût position/symbole et reconstruction sur le graphe original |
| P4 | `MultiWeightedAutomaton`, stockage aplati, dimensions fixes, frontières vectorielles, évaluation de parcours, projections/sélections, scalarisation vue O(d) et matérialisée, lexicographie DAG |
| P5 | Pareto DAG complet, dominance stricte, un représentant ou tous les chemins égaux explicitement demandés, témoins ; fronts par mot via couches ; opérations multi préservant tous les vecteurs, composition additive ou ressources indépendantes, nettoyage et fusion sans filtrage de dominance |
| P6 | Déterminisation résiduelle et minimisation avec potentiels sur DAG à coûts entiers bornés ; K meilleurs chemins DAG/horizon ; compilation DFA pondérée sans déterminisation implicite, index réutilisable entre pondérations ; coût optimal sous préfixe |

### Précisions et limites conservées

- La déterminisation générale cyclique et la minimisation générale ne sont pas
  annoncées comme disponibles. Les transformations certifiées exigent un DAG et
  des coûts entiers dont la valeur absolue est au plus `2^48/(V+1)^2` ; un DFA
  déjà déterministe est retourné tel quel par la déterminisation. L’explosion
  exponentielle et l’arrêt explicite sur budget restent possibles.
- Pareto, lexicographie et K chemins certifient d’abord un domaine de sommes
  dyadiques exactement représentables ; hors de ce domaine, leurs comparaisons
  utilisent les sommes exactes des valeurs binaires via `BigDecimal`. Les
  vecteurs de sortie sont arrondis en `double`, avec accès `exactCost(d)` aux
  sommes non arrondies. Ce recours reste interne ; il ne crée pas de dépendance.
- Bellman–Ford utilise un domaine entier sûr sur tableaux, sinon un calcul
  exact incluant le signe des cycles. Les autres chemins scalaires rapides et
  la DP entre symboles conservent les limites IEEE 754 documentées.
- L’évaluateur réutilise ses buffers sur NFA sans ε et pour les fermetures ε
  non négatives. Les fermetures signées peuvent allouer pour le calcul exact.
- Les fronts cycliques sans horizon sont rejetés ; les ε-cycles utiles restent
  des cycles dans le graphe déployé. Aucun tri topologique fictif n’est utilisé.
- L’énumération de tous les chemins égaux est explicite et limitée aux DAG.
  K chemins porte sur les arcs logiques ; K mots distincts et l’énumération
  générale sur graphes cycliques ne sont pas confondus avec cette API.
- Les approximations Pareto et index sophistiqués 2D/3D restent étudiés dans le
  plan, sans implémentation ni tronquage implicite. La première version exacte
  utilise des listes ; les résultats ne prétendent pas mesurer l’intérêt d’un
  index qui n’a pas été développé.

### Validation

Les nouveaux tests s’ajoutent aux 47 tests 3.0 : petits chemins et vecteurs
énumérés indépendamment, Floyd–Warshall sur graphes cycliques, mots et
concaténations, ε-cycles, frontières, extrêmes, scalarisation et réutilisation,
normalisation pondérée, K chemins et annulations numériques. Voir les journaux
et rapports de `audit/weighted-extension/` pour l’exécution finale, les mesures
JMH par famille d’algorithmes et la mesure de mémoire par instrumentation JVM.


## 15. Advanced follow-up: cyclic graphs, distinct words and lazy enumeration

Section 14 records the initial implementation merged through PR #7. The following
extensions supersede its corresponding advanced restrictions. Julien explicitly
requested continuation and GitHub publication. No original Boolean source or
runtime dependency changes were needed.

- `KBestPaths.iterator` / `pathsByCost`: exact-potential best-first enumeration of
  accepting walks on signed cyclic graphs without useful negative cycles. Persistent
  prefixes, fair equal-cost ordering by edge count, including epsilon edges. DAG
  K-best retains its fast suffix-list implementation. Fixed horizons now allow
  nonnegative epsilon cycles without consuming a position.
- `WeightedWord`, `KBestWords`, `kBestWords`, `wordsByCost`: distinct concrete words
  at minimum accepting cost. NFA prefixes carry exact per-state costs and weighted
  epsilon closure, with no complete determinization prerequisite. Symbolic interval
  classes use lazy concrete-symbol cursors. Deterministic graphs use a direct fast
  path. Signed consuming cycles are supported at a fixed horizon.
- `determinize(input,maxStates)`: exact residuals and epsilon closures over binary
  double values; cyclic inputs succeed when construction completes within the
  explicit state budget (default 10,000). No partial result or nondeterminizability
  claim on exhaustion. Already deterministic inputs remain unchanged. The certified
  bounded-integer DAG implementation remains available and automatically reused.
- `minimize(input)`: deterministic epsilon-free cyclic graphs, including negative
  consuming cycles. Potentials are exact costs of shortest-length, then smallest-word
  accepted suffixes; normalized signatures are refined to a fixed point. This does
  not require a finite minimum-cost walk.

Transformations emit only finite exactly representable double weights/boundaries;
otherwise they fail explicitly. Exact sums/differences of valid doubles need not
fit in another double. Direct word enumeration emits no transformed edges and is
not restricted by this representability test. There is no approximate residual
comparison. Ordinary scalar/compiled-vector evaluation retains its IEEE contract.

Structural costs exclude exact-number arithmetic: suffix potentials take O(V+E)
on DAGs, O((V+E) log(V+E)) with nonnegative edges, otherwise O(VE). Search work
and storage depend on explored prefixes/frontier; no specialized sidetrack bound
is claimed. NFA prefixes need O(V) cost vectors and a per-class arc scan/weighted
closure. Explicit determinization can be exponential or infinite; a state budget
bounds the explored output, with O(RV) residual storage. Simple cyclic minimization
has worst-case O(V(V+E+I log I)) time and O(V+E+I) live storage.

Independent tests cover bounded exhaustive walks with completeness bounds, exact
Floyd–Warshall epsilon closure plus word DP, and pair-product DFA offset equivalence
for the minimal number of states. Tests also cover zero/improving/irrelevant cycles,
horizons, cancellation, budgets, nonrepresentable outputs and full-int intervals.
Proofs, measured results and remaining restrictions are in the
[advanced report](audit/weighted-advanced-cyclic/README.md).

No complete arbitrary-NFA determinization test or twins-property checker is
provided. A growing residual construction may exhaust its budget even for a small
representable function. Unrestricted increasing-cost enumeration rejects useful
negative cycles. Infinite equal-cost sets can prevent access to higher costs.
Simple paths without repeated vertices and general cyclic vector Pareto fronts
remain distinct, unimplemented problems. Existing exact DAG fronts remain intact.

Primary reference for the residual/normalization schemes:
[Mohri, Weighted Automata Algorithms, sections 6.2–6.4](https://cs.nyu.edu/~mohri/pub/hwa.pdf).
The canonical-suffix proof and implemented invariants are explained in the report;
this reference is not used to claim universal cyclic determinization convergence.

## 16. Compiled vectors and scalar-to-vector composition

Additional explicit request: compile multi-weighted automata directly, and construct
multi-weighted automata from several scalar weightings.

- `CompiledMultiWeightedDfa`: deterministic vector recognition with caller buffers,
  slices and explicit absence/overflow behavior. The existing scalar interval index
  is reused without modifying its lookup loop. Projections, compiled scalarizations
  and reweighting (including changed dimensions) share it. Reweightings do not retain
  a chain of previous weight stores.
- `MultiWeightedAutomaton.fromWeightings`: O(d) view of scalar weightings on the
  identical graph, preserving the correlation of one common path. Operand references
  are defensively snapshotted; graph and scalar tables are shared.
- `synchronizedProduct`: one dimension per independent scalar operand; possibly
  different structures, common words, full path/vector combinations. It reuses the
  existing independent-resource product and its epsilon filter. Output may have
  product size; interval alphabets are never expanded.

The correlated view and independent product have different semantics even when
operands physically share a graph. Independent tests use generated DFA transition
tables and full Cartesian products of exhaustively enumerated operand paths,
including epsilon and boundaries. JMH measures dimensions 2/8/32, caller-buffer
allocations, index reuse, views and product construction. Reachable-object memory
measurements verify shared indexes and absence of reweighting-history retention.

## 17. Complete English library documentation

Julien explicitly requested documentation of the entire library, in English to
match the project. The new [manual](docs/README.md) covers Boolean automata,
integer alphabets, expressions, operations, language queries, compiled search,
scalar/vector weights, horizons, advanced algorithms, numeric/concurrency contracts
and FAQ. Its generated reference lists all 39 public types, including nested types
and overloads, from the actual compiled API. Existing historical audit sections
retain their original language and snapshot facts.

Eleven source-linked snippets are checked against an executable Java tour, compiled
against this checkout, and run. A regeneration tool keeps the static signature
reference reproducible. Root README links to the manual; library sources and the
root POM remain unchanged by documentation work. Javadoc generation is verified
separately. Documentation validation and hashes accompany the final audit.
