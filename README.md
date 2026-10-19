# Automaton

A Java library to create and manipulate deterministic and non-deterministic finite automata whose symbols are integers.

Transitions are labelled with sets of integer intervals, so an automaton can work on any `int` alphabet — from `Integer.MIN_VALUE` to `Integer.MAX_VALUE` — without enumerating it.

- Build automata by hand or from a regular expression
- Determinization (subset construction)
- Minimization (Hopcroft or Brzozowski)
- Union, concatenation, complement, reversal
- Word recognition and Graphviz export

## Installation

Version 1.8 is available on Maven Central:

```xml
<dependency>
    <groupId>fr.menana</groupId>
    <artifactId>automaton</artifactId>
    <version>1.8</version>
</dependency>
```

Version 2.0.0, which is much faster and fixes several bugs, is not published yet. Build it from source (requires Java 17 and Maven):

```bash
git clone https://github.com/bebui/Automaton.git
cd Automaton
mvn install
```

then depend on `fr.menana:automaton:2.0.0`.

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
| `a{n}`, `a{n,m}` | between n and m repetitions |
| `( )` | grouping |

Each character is a symbol: `12` is the symbol 1 followed by the symbol 2, while `<12>` is the single symbol 12. Single digit symbols must be written without angle brackets (`5`, not `<5>`).

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

`addTransition` also accepts single values (`auto.addTransition(s0, s1, 1, 2, 3)`) or an `IntervalSet`. `addEpsilonTransition(s0, s1)` adds an ε-transition and makes the automaton non-deterministic.

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

Union and concatenation of two deterministic automata return a minimal deterministic automaton.

`minimize()` uses Hopcroft's algorithm by default. Brzozowski's algorithm can be selected per call or globally:

```java
Operation.minimize(d, Operation.MINIMIZATION_ALGO.Brzozowski);
Operation.minimization_method = Operation.MINIMIZATION_ALGO.Brzozowski;
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
| Determinization | proportional to the number of states of the resulting DFA (exponential in the worst case) |
| Hopcroft minimization | O(k · n log n), n states, k symbol classes |
| Brzozowski minimization | exponential in the worst case, often fast in practice |
| `run` on a deterministic automaton | O(length · t), t outgoing transitions per state |
| `run` on a non-deterministic automaton | O(length · number of states) |

Symbol classes are the maximal intervals of values that every transition treats in the same way, so k stays small even when transitions cover large ranges.

## Building and testing

```bash
mvn clean install
```

The test suite compares determinization and both minimization algorithms against an independent simulation of thousands of random non-deterministic automata.

## License

[GNU Lesser General Public License v3.0](LICENSE)
