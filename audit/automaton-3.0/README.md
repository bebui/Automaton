# Pièces de l’audit Automaton 3.0.0

Base : `f7532436cbe964758988e13f8c5872c43bebe58b`, branche `master`.
Date : 20 octobre 2026, Europe/Paris. Aucun fichier suivi de la base n’a été modifié.

Le [plan technique](../../WEIGHTED_AUTOMATA_PLAN.md) distingue les observations,
les propositions à valider, les limites numériques et les lots futurs.

## Documents et résultats

| Pièce | Rôle |
|---|---|
| [inventory.txt](inventory.txt) | Les 38 fichiers suivis du commit audité, sans les artefacts de build |
| [tracked-files.sha256](tracked-files.sha256) | Empreintes SHA-256 de ces fichiers ; référence de non-modification |
| [public-api.txt](public-api.txt) | API publique extraite avec `javap -public`, y compris `Match` et l’enum de minimisation |
| [tests.log](tests.log) | Exécution intégrale de `mvn -B test`, 47 tests réussis |
| [build.log](build.log) | Packaging et génération sources/Javadocs, `mvn -B -DskipTests verify` réussi |
| [BASELINE.md](BASELINE.md) | Méthode JMH, source intégral du harnais et reproduction |
| [environment.json](environment.json) | JDK, OS, CPU, paramètres de mesure et limites |
| [probes.log](probes.log) | Observations ciblées confirmées par le programme ci-dessous |

Campagne JMH terminée : **39 combinaisons**, 2 forks et 10 échantillons mesurés
par combinaison, aucune erreur. Voir [RESULTS.md](RESULTS.md),
[jmh-results.json](jmh-results.json), [jmh.log](jmh.log) et
[fixtures.log](fixtures.log). Les dépendances JMH restent extérieures au POM
de la bibliothèque.

## Suite de tests existante

| Classe | Tests | Validation principale |
|---|---:|---|
| IntervalSetTest | 5 | Opérations sur ensembles d’intervalles |
| FunctionalAutomatonTest | 1 | Scénario de reconnaissance depuis une expression |
| BugFixTest | 17 | Régressions, erreurs de syntaxe, copies, constantes, cas vides |
| IntervalTest | 4 | Contenance, intersection et complément |
| LanguageTest | 11 | Interpréteur indépendant, mots exhaustifs, opérations et requêtes |
| RandomTest | 2 | Minimisation et complément sur DFA aléatoires |
| NfaEquivalenceTest | 7 | Simulation indépendante NFA/ε, extrêmes et oracle BitSet |
| **Total** | **47** | **0 échec, 0 erreur, 0 ignoré** |

Les fichiers de tests ont été lus en entier. Il n’y a pas de test pondéré dans
la base, ni de harnais JMH suivi. Le nombre de tests JUnit ne représente pas
le nombre de cas aléatoires/exhaustifs effectivement vérifiés.

## Localisation des constats importants

- `src/main/java/fr/menana/automaton/State.java`, `addToTransitionMap` : fusion
  par destination, union des étiquettes et combinaison du drapeau ε.
- `Automaton.java`, `getStates`, et `State.java`, `getTransitions` : collections
  mutables exposées ; `Transition.java`, `getValues` : copie défensive des labels.
- `Automaton.java`, `reIndex` : renumérotation BFS et suppression des inaccessibles.
- `Automaton.java`, `clone` : copie profonde du graphe et des ensembles d’étiquettes.
- `CompiledDfa.java`, `of` : déterminisation NFA implicite puis tableaux
  d’intervalles, sans conservation des parcours ou des identités d’arcs originaux.
- `Operation.java`, `removeEpsilon`, et `Automaton.java`, `epsilonClosure` :
  fermeture booléenne, pas de somme/minimum de poids.
- `Languages.java`, `shortestWord` : coût 0 pour ε et 1 pour symbole ;
  `countTable` : programmation dynamique de comptage de mots distincts après
  compilation déterministe.
- `Interval.java`, `size`, et `IntervalSet.java`, `compareTo` : limites existantes
  reproduites ci-dessous, à éviter dans les nouvelles implémentations.

## Vérification de la base inchangée

Depuis la racine du dépôt :

```bash
sha256sum -c audit/automaton-3.0/tracked-files.sha256
git diff --exit-code HEAD -- src pom.xml README.md .github .gitignore LICENSE
```

Les sources et tests n’ont pas été modifiés. Les seuls ajouts dans le dépôt sont
le plan et les pièces d’audit. La copie de travail est sous
`/home/julien/codex_ha/Automaton`, distincte du dépôt de domotique parent.
Aucun push, changement de branche distante ou publication n’a été effectué.

## Sonde d’observation

Programme exécuté une fois hors des sources du projet. Il confirme des
comportements existants ; ce n’est pas une nouvelle suite de tests de régression.
Sortie conservée dans [probes.log](probes.log).

```java
import fr.menana.automaton.*;

public class AuditProbe {
    public static void main(String[] args) {
        Automaton a = new Automaton();
        State discarded = a.addState(), start = a.addState(), end = a.addState();
        a.setInitial(start);
        a.setAccept(end);
        a.addTransition(start, end, 1);
        a.addTransition(start, end, 2);
        a.addEpsilonTransition(start, end);
        Transition t = start.getTransitions().get(end);
        System.out.println("arcs after 2 symbolic additions + epsilon: " + start.getTransitions().size());
        System.out.println("merged arc: accepts 1=" + t.accepts(1) + ", accepts 2=" + t.accepts(2) + ", epsilon=" + t.hasEpsilon());
        int before = start.getIndex();
        a.reIndex();
        System.out.println("initial index before/after reIndex: " + before + "/" + start.getIndex());
        System.out.println("states after reIndex: " + a.getNbStates());
        Automaton snapshotSource = a.clone();
        start.getTransitions().clear();
        System.out.println("clear exposed transitions removes recognition: " + !a.run(1));
        System.out.println("clone independent of source changes: " + snapshotSource.run(1));
        System.out.println("Interval ALL int size: " + new Interval(Integer.MIN_VALUE, Integer.MAX_VALUE).size());
        System.out.println("IntervalSet ALL long size: " + IntervalSet.ALL.size());
        IntervalSet x=IntervalSet.fromIntArray(0,1,4), y=IntervalSet.fromIntArray(0,3,4);
        System.out.println("distinct sets compare equal: " + (x.compareTo(y)==0 && !x.equals(y)));
        Automaton duplicate = new Automaton();
        State p=duplicate.addState(), q=duplicate.addState();
        duplicate.setInitial(p);
        duplicate.setAccept(q);
        duplicate.addTransition(p,q,1);
        duplicate.addTransition(p,q,1);
        System.out.println("duplicate DFA arc flag: " + duplicate.isDeterministic());
    }
}
```

Compilation et exécution depuis la racine du dépôt, après avoir enregistré le
source sous `/tmp/automaton-3.0-audit/AuditProbe.java` :

```bash
/usr/lib/jvm/java-17-openjdk-amd64/bin/javac -proc:none -cp target/classes \
  -d /tmp/automaton-3.0-audit/classes /tmp/automaton-3.0-audit/AuditProbe.java
/usr/lib/jvm/java-17-openjdk-amd64/bin/java \
  -cp target/classes:/tmp/automaton-3.0-audit/classes AuditProbe
```
