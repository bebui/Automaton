# Référence JMH — Automaton 3.0.0

## Périmètre

Code de production inchangé au commit `f7532436cbe964758988e13f8c5872c43bebe58b`.
Harnais isolé compilé sous `/tmp/automaton-3.0-audit/` contre `target/classes`.
Aucun plugin ni dépendance ajouté au projet. JMH 1.37 est le
[harnais OpenJDK](https://github.com/openjdk/jmh).

Paramètres effectifs : un thread, deux forks, trois échauffements de 500 ms,
cinq mesures de 500 ms par fork, mode temps moyen, microsecondes par opération,
profilage GC, heap de 128 à 256 Mio. Une itération peut dépasser 500 ms si une
opération individuelle dure plus longtemps. Les résultats JSON conservent les
échantillons et l’erreur JMH. Matériel/JVM dans [environment.json](environment.json).

La machine est partagée : aucune isolation CPU, fréquence non fixée,
échauffement court. Une autre campagne JMH a initialement retenu le verrou ;
son achèvement a été attendu, sans contourner le verrou ni arrêter de processus.
Les données sont une **référence préliminaire locale**, pas un seuil fiable de
régression à elles seules. Les allocations incluent ce que le profiler attribue
aux threads du processus et ne sont pas une mesure de mémoire retenue.

## Jeux de données et opérations

| Famille | États | Degré demandé avant fusion | Largeur d’un intervalle |
|---|---:|---:|---:|
| sparse-unit | 64 | 2 | 1 |
| sparse-interval | 256 | 2 | 1 000 000 |
| dense-unit | 64 | 16 | 1 |
| dense-interval | 256 | 16 | 1 000 000 |

Tous ces graphes sont des DFA cycliques, complets sur leur alphabet utilisé,
avec une chaîne cyclique visitant tous les états, et des arcs supplémentaires
pseudo-aléatoires. Graines 42/43 ; états finaux d’index congru à 6 modulo 7.
Les arcs de même destination sont fusionnés comme dans la bibliothèque réelle :
le degré demandé n’est donc pas le nombre exact d’objets `Transition` stockés.
Les familles unitaires et larges ont des tailles différentes ; ne pas attribuer
leurs écarts de temps à la seule largeur des intervalles.

- `construct` inclut états, intervalles, validation du déterminisme et génération
  pseudo-aléatoire de la fixture ; pas seulement l’insertion dans une structure.
- `run` et `compiledRun` lisent le même tableau préalloué de 64 symboles valides,
  sans reconstruction ni génération du mot dans la mesure. Les DFA sont complets
  sur ces symboles, donc les 64 symboles sont effectivement consommés.
- `copy`, `compile`, `minimize` et `intersection` incluent les allocations du
  résultat. Le second opérande du produit a 16 états ; le produit est ensuite
  minimisé par l’API publique 3.0.
- `countWords` appelle l’API publique pour 16 symboles, compilation incluse.
- `shortestWord` utilise la même fixture et cherche un final (initial non final).
- Les trois opérations NFA utilisent `(1|2)*1(1|2){3}` construit par le parseur
  3.0. Le mot préalloué de `nfaRun` alterne 1 et 2 sur 64 symboles.

Les tailles stockées réelles et la reconnaissance des fixtures sont détaillées
séparément ; le source ci-dessous définit intégralement les données.

## Reproduction

Prérequis : Java 17, classes 3.0 compilées, et ces quatre JAR dans le cache Maven :
`org.openjdk.jmh:jmh-core:1.37`, `org.openjdk.jmh:jmh-generator-annprocess:1.37`,
`net.sf.jopt-simple:jopt-simple:5.0.4`, `org.apache.commons:commons-math3:3.6.1`.
Ils étaient déjà disponibles sur la machine d’audit. Si nécessaire, les récupérer
avec `mvn dependency:get -Dartifact=GROUP:ARTIFACT:VERSION` sans modifier le POM.

Depuis la racine du dépôt, enregistrer le bloc Java ci-dessous dans
`/tmp/automaton-3.0-audit/src/audit/Baseline.java`, puis :

```bash
mkdir -p /tmp/automaton-3.0-audit/classes
AUTOMATON_BENCH_CP="target/classes:/tmp/automaton-3.0-audit/classes:$HOME/.m2/repository/org/openjdk/jmh/jmh-core/1.37/jmh-core-1.37.jar:$HOME/.m2/repository/org/openjdk/jmh/jmh-generator-annprocess/1.37/jmh-generator-annprocess-1.37.jar:$HOME/.m2/repository/net/sf/jopt-simple/jopt-simple/5.0.4/jopt-simple-5.0.4.jar:$HOME/.m2/repository/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar"
/usr/lib/jvm/java-17-openjdk-amd64/bin/javac -cp "$AUTOMATON_BENCH_CP" \
  -d /tmp/automaton-3.0-audit/classes /tmp/automaton-3.0-audit/src/audit/Baseline.java
/usr/lib/jvm/java-17-openjdk-amd64/bin/java -Xms128m -Xmx256m \
  -cp "$AUTOMATON_BENCH_CP" org.openjdk.jmh.Main 'audit.Baseline.*' \
  -prof gc -rf json -rff /tmp/automaton-3.0-audit/results.json -foe true
```

Attendre la disponibilité de la machine et du verrou JMH ; ne pas lancer en
concurrence avec les tests, une compilation ou une autre mesure intensive.
Pour une comparaison décisive, prolonger l’échauffement et les mesures et
utiliser au moins trois forks, sur base puis candidat dans les mêmes conditions.

## Source du harnais exécuté

```java
package audit;

import fr.menana.automaton.Automaton;
import fr.menana.automaton.CompiledDfa;
import fr.menana.automaton.Interval;
import org.openjdk.jmh.annotations.*;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 2, jvmArgsAppend = {"-Xms128m", "-Xmx256m"})
public class Baseline {
    @State(Scope.Thread)
    public static class Graph {
        @Param({"sparse-unit", "sparse-interval", "dense-unit", "dense-interval"})
        public String family;
        Automaton a, b;
        CompiledDfa compiled;
        int[] word;
        int states, degree, width;

        @Setup(Level.Trial)
        public void setup() {
            states = family.endsWith("unit") ? 64 : 256;
            degree = family.startsWith("sparse") ? 2 : 16;
            width = family.endsWith("unit") ? 1 : 1000000;
            a = graph(states, degree, width, 42);
            b = graph(16, degree, width, 43);
            compiled = a.compile();
            Random r = new Random(44);
            word = new int[64];
            for (int i = 0; i < word.length; i++)
                word[i] = r.nextInt(degree) * width + r.nextInt(width);
            if (a.run(word) != compiled.run(word))
                throw new AssertionError("Fixture recognition mismatch");
            if (!a.isDeterministic()) throw new AssertionError("Fixture must be DFA");
        }
    }

    @State(Scope.Thread)
    public static class Nfa {
        Automaton a;
        int[] word;
        @Setup(Level.Trial)
        public void setup() {
            a = Automaton.nfaFromString("(1|2)*1(1|2){3}");
            word = new int[64];
            for (int i = 0; i < word.length; i++) word[i] = 1 + (i % 2);
            if (a.isDeterministic()) throw new AssertionError("Fixture must be NFA");
        }
    }

    static Automaton graph(int n, int degree, int width, int seed) {
        Automaton a = new Automaton();
        fr.menana.automaton.State[] s = new fr.menana.automaton.State[n];
        for (int i = 0; i < n; i++) s[i] = a.addState();
        a.setInitial(s[0]);
        Random r = new Random(seed);
        for (int i = 0; i < n; i++) {
            if (i % 7 == 6) a.setAccept(s[i]);
            for (int j = 0; j < degree; j++) {
                int target = j == 0 ? (i + 1) % n : r.nextInt(n);
                a.addTransition(s[i], s[target], new Interval(j * width, (j + 1) * width - 1));
            }
        }
        return a;
    }

    @Benchmark public boolean run(Graph g) { return g.a.run(g.word); }
    @Benchmark public boolean compiledRun(Graph g) { return g.compiled.run(g.word); }
    @Benchmark public Automaton construct(Graph g) { return graph(g.states, g.degree, g.width, 42); }
    @Benchmark public Automaton copy(Graph g) { return g.a.clone(); }
    @Benchmark public CompiledDfa compile(Graph g) { return g.a.compile(); }
    @Benchmark public Automaton minimize(Graph g) { return g.a.minimize(); }
    @Benchmark public Automaton intersection(Graph g) { return g.a.intersection(g.b); }
    @Benchmark public BigInteger countWords(Graph g) { return g.a.countWords(16); }
    @Benchmark public int[] shortestWord(Graph g) { return g.a.shortestWord(); }
    @Benchmark public boolean nfaRun(Nfa n) { return n.a.run(n.word); }
    @Benchmark public Automaton determinize(Nfa n) { return n.a.determinize(); }
    @Benchmark public Automaton removeEpsilon(Nfa n) { return n.a.removeEpsilon(); }
}
```

## Résultats obtenus

Campagne terminée sans erreur ; 39 combinaisons, 2 forks par combinaison,
10 échantillons mesurés par combinaison. Voir [la table de mesures](RESULTS.md),
[les données JSON](jmh-results.json) et [le journal brut](jmh.log).

Tailles réelles mesurées après fusion des arcs :

```text
sparse-unit states=64 transitions=128 consuming_modes=128 epsilon_modes=0 intervals=128 finals=9 deterministic=true
sparse-unit word_length=64 accepted=true
sparse-unit-right-product-operand states=16 transitions=31 consuming_modes=31 epsilon_modes=0 intervals=31 finals=2 deterministic=true
sparse-interval states=256 transitions=512 consuming_modes=512 epsilon_modes=0 intervals=512 finals=36 deterministic=true
sparse-interval word_length=64 accepted=false
sparse-interval-right-product-operand states=16 transitions=31 consuming_modes=31 epsilon_modes=0 intervals=31 finals=2 deterministic=true
dense-unit states=64 transitions=922 consuming_modes=922 epsilon_modes=0 intervals=1013 finals=9 deterministic=true
dense-unit word_length=64 accepted=false
dense-unit-right-product-operand states=16 transitions=158 consuming_modes=158 epsilon_modes=0 intervals=237 finals=2 deterministic=true
dense-interval states=256 transitions=3986 consuming_modes=3986 epsilon_modes=0 intervals=4082 finals=36 deterministic=true
dense-interval word_length=64 accepted=false
dense-interval-right-product-operand states=16 transitions=158 consuming_modes=158 epsilon_modes=0 intervals=237 finals=2 deterministic=true
nfa states=15 transitions=19 consuming_modes=9 epsilon_modes=10 intervals=9 finals=1 deterministic=false
nfa word_length=64 accepted=true
nfa-determinized states=16 transitions=32 consuming_modes=32 epsilon_modes=0 intervals=32 finals=8 deterministic=true
```

Les deux forks de certaines opérations montrent une dérive visible ; cette
référence ne justifie aucun facteur d’accélération général ni aucun seuil
automatique de non-régression. Les algorithmes pondérés ne sont pas encore
implémentés et ne sont donc pas mesurés ici.
