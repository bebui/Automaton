# Automaton

A Java library to create and manipulate deterministic and non-deterministic finite automata whose symbols are integers.

Transitions are labelled with sets of integer intervals, so an automaton can work on any `int` alphabet — from `Integer.MIN_VALUE` to `Integer.MAX_VALUE` — without enumerating it.

- Build automata by hand or from a regular expression, with intersection and complement
- Determinization, minimization (Hopcroft-style partition refinement or Brzozowski), ε-removal, reduction of NFAs
- Union, intersection, difference, concatenation, complement, reversal
- Language queries: emptiness, finiteness, equivalence, inclusion, shortest word, counter-examples
- Counting, uniform random generation and enumeration of words
- Fast word recognition with compiled automata, search inside sequences
- Graphviz export
- Scalar/vector weighted automata: scalar/vector weights, shortest paths, finite horizons, lexicographic and Pareto optimization — see [weighted automata](WEIGHTED_AUTOMATA.md)

## Documentation

Read the [complete library manual](docs/README.md) for installation, Boolean and
weighted automata, algorithms, contracts and runnable examples. The
[public API reference](docs/API_REFERENCE.md) lists every current public type and
overload, including the weighted API. The 12 documented examples are
checked against executable source with `python3 docs/check_examples.py` after a
local build. Published Javadoc and Maven artifacts may lag behind this checkout.
Maintainers can follow [release preparation and publication](RELEASING.md) to
build and verify a signed Central bundle before uploading it.

## Installation

The published **4.0.0** release is available on Maven Central (Java 17 required).
This checkout develops **4.1.0-SNAPSHOT**, adding direct integration between
`Automaton` and its weighted variants. Build/install the checkout locally for
those new APIs; the coordinates below select the published 4.0.0 release.

```xml
<dependency>
    <groupId>fr.menana</groupId>
    <artifactId>automaton</artifactId>
    <version>4.0.0</version>
</dependency>
```

With Gradle: `implementation("fr.menana:automaton:4.0.0")`.

The API documentation is on [javadoc.io](https://javadoc.io/doc/fr.menana/automaton).

### Adding weights in the 4.1 development API

Start from an existing `Automaton` with `weighted(transition -> cost)` or
`multiWeighted(...)`. Reuse `snapshot()` when several weightings must share one
structure. Weighted objects provide `run(...)` for Boolean membership and
`toAutomaton()` for an explicit projection forgetting costs. See
[Automaton and its weighted variants](docs/AUTOMATON_AND_WEIGHTS.md) for the
executable example, identity mappings and mutation contracts.

### Upgrading

From 3.0 to 4.0: the existing Boolean API keeps its packages and signatures.
The new weighted API lives in `fr.menana.automaton.weighted`, with compiled
evaluators in `weighted.compiled`. See the [4.0 migration guide](docs/MIGRATION_4.md)
and [release notes](CHANGELOG.md).

From 2.x to 3.0: the classes of the nodes of regular expressions (`RegExpInt`, `RegExpOr`, ...) and the `Util` class are no longer public; use `RegExpParser` and `RegExp` instead.

From 1.x: version 2.0 is much faster, fixes many bugs and adds many features, but changes part of the API. Transitions are read with `getOrig()`, `getDest()` and `getValues()`, the default minimization is set with `Operation.setDefaultMinimization(...)`, `IntervalSet.size()` returns a `long` and `new Interval(min, max)` requires `min <= max`.

## Quick start

```java
import fr.menana.automaton.Automaton;

// Words over {1, 2} containing the factor 1 2
Automaton auto = new Automaton("(1|2)*12(1|2)*");

auto.run(1, 1, 2);   // true
auto.run(2, 1);      // false
```

`new Automaton(regexp)` and `Automaton.dfaFromString(regexp)` return a minimal deterministic automaton; `Automaton.nfaFromString(regexp)` returns the non-deterministic one built from the expression.

## Regular expression syntax

| Syntax | Meaning |
|---|---|
| `5` | the symbol 5 (a single digit) |
| `<42>`, `<-3>` | a symbol written with several characters |
| `.` | any integer |
| `ab` | concatenation |
| `a\|b` | alternative |
| `a*` | zero or more |
| `a+` | one or more |
| `a?` | zero or one |
| `a{n}` | exactly n repetitions |
| `a{n,m}` | between n and m repetitions |
| `a{n,}`, `a{,m}` | at least n, at most m repetitions |
| `a&b` | intersection: words matching both |
| `~a` | complement: every word not matching `a` |
| `( )` | grouping |

Priorities, from the strongest: repetitions (`* + ? {}`), complement `~`, concatenation, intersection `&`, alternative `|`. For instance `~12` is `(~1)2` and `1&1|2` is `(1&1)|2`.

Each character is a symbol: `12` is the symbol 1 followed by the symbol 2, while `<12>` is the single symbol 12. Any integer can be written between angle brackets, `<5>` is the same as `5`.

An invalid expression throws an `IllegalArgumentException` giving the position of the error, for instance `Expected ')' but reached the end at position 2 in "(1"`.

```java
Automaton a = Automaton.dfaFromString("<10><-3>.*");
a.run(10, -3);       // true
a.run(10, -3, 99);   // true
```

## Building an automaton by hand

```java
import fr.menana.automaton.*;

// One or more symbols between 0 and 9
Automaton auto = new Automaton();
State s0 = auto.addState();
State s1 = auto.addState();
auto.setInitial(s0);
auto.setAccept(s1);
auto.addTransition(s0, s1, new Interval(0, 9));
auto.addTransition(s1, s1, new Interval(0, 9));

auto.run(4, 2);      // true
auto.run();          // false
```

`addTransition` also accepts single values (`auto.addTransition(s0, s1, 1, 2, 3)`) or an `IntervalSet`, which is copied. `addEpsilonTransition(s0, s1)` adds an ε-transition and makes the automaton non-deterministic.

Transitions are read with `state.getTransitions()`; `Transition.getValues()` returns a copy, so an automaton can only be changed through its own methods. `IntervalSet.ALL` and `IntervalSet.EMPTY` are read-only: call `clone()` to get a modifiable set.

## Operations

Every operation returns a new automaton and leaves its operands unchanged.

```java
Automaton a = Automaton.dfaFromString("12");
Automaton b = Automaton.dfaFromString("34");

Automaton u = a.union(b);                // 12 | 34
Automaton c = u.concatenate(Automaton.dfaFromString("5"));   // (12 | 34) 5
Automaton n = a.complement();            // every word except 1 2
Automaton r = a.revert();                // 2 1 (non-deterministic)

Automaton d = Automaton.nfaFromString("(1|2)*1(1|2)(1|2)").determinize();
Automaton m = d.minimize();
```

Union and concatenation of two deterministic automata, intersection and difference return a minimal deterministic automaton. Union, intersection and difference are built as products of deterministic automata, exploring only the reachable pairs of states.

```java
Automaton both = a.intersection(b);      // words of a and b
Automaton only = a.difference(b);        // words of a that are not in b
Automaton noEps = Automaton.nfaFromString("(1|2)*1").removeEpsilon();   // position automaton, no ε
Automaton small = Automaton.nfaFromString("(1|2)*1").reduce();          // merges bisimilar states
```

`minimize()` uses Hopcroft-style partition refinement by default. Brzozowski's algorithm can be selected per call or for the whole application:

```java
Operation.minimize(d, Operation.MINIMIZATION_ALGO.Brzozowski);
Operation.setDefaultMinimization(Operation.MINIMIZATION_ALGO.Brzozowski);
```

`complement()` works on deterministic and non-deterministic automata: the latter are determinized first.

## Language queries

```java
Automaton a = Automaton.dfaFromString("(1|2)*1");
Automaton b = Automaton.dfaFromString("(1|2)*1(1|2)*");

a.isEmpty();             // false
a.isFinite();            // false
a.isUniversal();         // false
a.isSubsetOf(b);         // true, without determinizing
a.sameLanguage(b);       // false, whatever the structure of the automata
a.counterExample(b);     // [1, 2]: a shortest word accepted by only one of them
a.shortestWord();        // [1]
```

`equals` compares the structure of two automata, `sameLanguage` compares their languages.

## Counting and generating words

```java
Automaton a = Automaton.dfaFromString("(1|2)*1");

a.countWords(3);                   // 4, as a BigInteger
a.randomWord(3, new Random());     // one of the 4 words, uniformly
for (int[] word : a.words(2))      // [1], [1, 1], [2, 1]: by length, then lexicographic
    System.out.println(Arrays.toString(word));
```

## Fast recognition

`compile()` turns an automaton into an immutable deterministic automaton stored in arrays. Reading a symbol is a binary search among the interval segments of the current state; performance depends on degree and symbol distribution. The result can be shared between threads.

```java
CompiledDfa dfa = Automaton.dfaFromString("(1|2)*12").compile();
dfa.run(2, 1, 2);                  // true

int state = dfa.getInitialState(); // or read symbols one by one
state = dfa.step(state, 1);
dfa.isAccept(state);
```

## Searching

A `Searcher` finds the factors of a sequence that belong to a language. Matches are leftmost-longest, and each search is linear in the length of the sequence.

```java
Searcher searcher = Automaton.dfaFromString("12+").searcher();

searcher.find(3, 1, 2, 2, 4);      // Match[start=1, end=4]
searcher.contains(3, 1, 3);        // false
searcher.findAll(1, 2, 0, 1, 2, 2);// [Match[start=0, end=2], Match[start=3, end=6]]
```

## Visualization

`toDot()` returns a [Graphviz](https://graphviz.org/) description of the automaton, `toDotty(file)` writes it to a file:

```java
Automaton.dfaFromString("1(2|3)").toDotty("auto.dot");
```

```bash
dot -Tpng auto.dot -o auto.png
```

## Complexity

| Operation | Complexity |
|---|---|
| Regular expression to NFA | Linear in simple Thompson fragments; repetition expansion and embedded Boolean operations add their own costs |
| Determinization | proportional to the number of states of the resulting DFA (exponential in the worst case) |
| Minimization (default) | O(m log n), n states, m transitions per symbol class (Valmari–Lehtinen) |
| Brzozowski minimization | exponential in the worst case, often fast in practice |
| Union, intersection, difference | proportional to the reachable pairs of states of the product |
| `sameLanguage` | almost linear in the size of the deterministic automata (Hopcroft–Karp) |
| `isSubsetOf` | antichains, without determinization |
| `reduce` | O(m log n) (Paige–Tarjan) |
| `CompiledDfa.run` | O(length · log t), t outgoing transitions per state |
| `run` on a deterministic automaton | O(length · t · log(k+1)), k intervals per label |
| `run` on a non-deterministic automaton | O(length · (V+E) · log(k+1)) upper bound for set simulation and ε-closure |
| `Searcher.find` | O(length of the text + length of the match) |

The refinement/minimization bounds exclude any preceding NFA determinization, which can be exponential.

Symbol classes are the maximal intervals of values that every transition treats in the same way, so their number stays small even when transitions cover large ranges.

## Building and testing

```bash
mvn clean install
```

The test suite compares the library with independent references: a simulation of thousands of random non-deterministic automata, an interpreter of regular expressions written in the tests, and the enumeration of all short words.

## License

[GNU Lesser General Public License v3.0](LICENSE)
