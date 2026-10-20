# Complete public API reference

Generated from the compiled Java 17 classes in this checkout. Regenerate with
`python3 docs/generate_api_reference.py` after building the library.
This lists declared public constructors, methods, overloads and constants,
including public nested types. Unsupported `internal` implementation helpers are excluded.
Inherited `Object` methods are not repeated.
Java/JDK and library package prefixes are shortened for readability.
Parameter contracts, return/absence conventions and algorithm preconditions
are explained in the [topic guides](README.md) and source Javadoc.

**43 public types.**

| Type | Source |
|---|---|
| [Alphabet](#alphabet) | [Alphabet.java](../src/main/java/fr/menana/automaton/Alphabet.java) |
| [Automaton](#automaton) | [Automaton.java](../src/main/java/fr/menana/automaton/Automaton.java) |
| [AutomatonSnapshot.TransitionWeight](#automatonsnapshot-transitionweight) | [AutomatonSnapshot.java](../src/main/java/fr/menana/automaton/AutomatonSnapshot.java) |
| [AutomatonSnapshot](#automatonsnapshot) | [AutomatonSnapshot.java](../src/main/java/fr/menana/automaton/AutomatonSnapshot.java) |
| [CompiledDfa](#compileddfa) | [CompiledDfa.java](../src/main/java/fr/menana/automaton/CompiledDfa.java) |
| [Interval](#interval) | [Interval.java](../src/main/java/fr/menana/automaton/Interval.java) |
| [IntervalSet](#intervalset) | [IntervalSet.java](../src/main/java/fr/menana/automaton/IntervalSet.java) |
| [Operation.MINIMIZATION_ALGO](#operation-minimization_algo) | [Operation.java](../src/main/java/fr/menana/automaton/Operation.java) |
| [Operation](#operation) | [Operation.java](../src/main/java/fr/menana/automaton/Operation.java) |
| [Searcher.Match](#searcher-match) | [Searcher.java](../src/main/java/fr/menana/automaton/Searcher.java) |
| [Searcher](#searcher) | [Searcher.java](../src/main/java/fr/menana/automaton/Searcher.java) |
| [State](#state) | [State.java](../src/main/java/fr/menana/automaton/State.java) |
| [Transition](#transition) | [Transition.java](../src/main/java/fr/menana/automaton/Transition.java) |
| [regexp.RegExp](#regexp-regexp) | [RegExp.java](../src/main/java/fr/menana/automaton/regexp/RegExp.java) |
| [regexp.RegExpParser](#regexp-regexpparser) | [RegExpParser.java](../src/main/java/fr/menana/automaton/regexp/RegExpParser.java) |
| [weighted.AutomatonGraph.Builder](#weighted-automatongraph-builder) | [AutomatonGraph.java](../src/main/java/fr/menana/automaton/weighted/AutomatonGraph.java) |
| [weighted.AutomatonGraph.ImportResult](#weighted-automatongraph-importresult) | [AutomatonGraph.java](../src/main/java/fr/menana/automaton/weighted/AutomatonGraph.java) |
| [weighted.AutomatonGraph](#weighted-automatongraph) | [AutomatonGraph.java](../src/main/java/fr/menana/automaton/weighted/AutomatonGraph.java) |
| [weighted.KBestPaths](#weighted-kbestpaths) | [KBestPaths.java](../src/main/java/fr/menana/automaton/weighted/KBestPaths.java) |
| [weighted.KBestWords](#weighted-kbestwords) | [KBestWords.java](../src/main/java/fr/menana/automaton/weighted/KBestWords.java) |
| [weighted.LayeredAutomaton.Analysis](#weighted-layeredautomaton-analysis) | [LayeredAutomaton.java](../src/main/java/fr/menana/automaton/weighted/LayeredAutomaton.java) |
| [weighted.LayeredAutomaton.PositionWeights](#weighted-layeredautomaton-positionweights) | [LayeredAutomaton.java](../src/main/java/fr/menana/automaton/weighted/LayeredAutomaton.java) |
| [weighted.LayeredAutomaton](#weighted-layeredautomaton) | [LayeredAutomaton.java](../src/main/java/fr/menana/automaton/weighted/LayeredAutomaton.java) |
| [weighted.MultiWeightedAutomaton.Builder](#weighted-multiweightedautomaton-builder) | [MultiWeightedAutomaton.java](../src/main/java/fr/menana/automaton/weighted/MultiWeightedAutomaton.java) |
| [weighted.MultiWeightedAutomaton](#weighted-multiweightedautomaton) | [MultiWeightedAutomaton.java](../src/main/java/fr/menana/automaton/weighted/MultiWeightedAutomaton.java) |
| [weighted.MultiWeightedOperations](#weighted-multiweightedoperations) | [MultiWeightedOperations.java](../src/main/java/fr/menana/automaton/weighted/MultiWeightedOperations.java) |
| [weighted.MultiWeightedPath](#weighted-multiweightedpath) | [MultiWeightedPath.java](../src/main/java/fr/menana/automaton/weighted/MultiWeightedPath.java) |
| [weighted.MultiWeightedPaths](#weighted-multiweightedpaths) | [MultiWeightedPaths.java](../src/main/java/fr/menana/automaton/weighted/MultiWeightedPaths.java) |
| [weighted.ParetoResult](#weighted-paretoresult) | [ParetoResult.java](../src/main/java/fr/menana/automaton/weighted/ParetoResult.java) |
| [weighted.ShortestPathResult.Status](#weighted-shortestpathresult-status) | [ShortestPathResult.java](../src/main/java/fr/menana/automaton/weighted/ShortestPathResult.java) |
| [weighted.ShortestPathResult](#weighted-shortestpathresult) | [ShortestPathResult.java](../src/main/java/fr/menana/automaton/weighted/ShortestPathResult.java) |
| [weighted.WeightedAutomaton.Builder](#weighted-weightedautomaton-builder) | [WeightedAutomaton.java](../src/main/java/fr/menana/automaton/weighted/WeightedAutomaton.java) |
| [weighted.WeightedAutomaton.Evaluator](#weighted-weightedautomaton-evaluator) | [WeightedAutomaton.java](../src/main/java/fr/menana/automaton/weighted/WeightedAutomaton.java) |
| [weighted.WeightedAutomaton](#weighted-weightedautomaton) | [WeightedAutomaton.java](../src/main/java/fr/menana/automaton/weighted/WeightedAutomaton.java) |
| [weighted.WeightedDfaIndex](#weighted-weighteddfaindex) | [WeightedDfaIndex.java](../src/main/java/fr/menana/automaton/weighted/WeightedDfaIndex.java) |
| [weighted.WeightedOperations](#weighted-weightedoperations) | [WeightedOperations.java](../src/main/java/fr/menana/automaton/weighted/WeightedOperations.java) |
| [weighted.WeightedPath](#weighted-weightedpath) | [WeightedPath.java](../src/main/java/fr/menana/automaton/weighted/WeightedPath.java) |
| [weighted.WeightedPaths.Analysis](#weighted-weightedpaths-analysis) | [WeightedPaths.java](../src/main/java/fr/menana/automaton/weighted/WeightedPaths.java) |
| [weighted.WeightedPaths](#weighted-weightedpaths) | [WeightedPaths.java](../src/main/java/fr/menana/automaton/weighted/WeightedPaths.java) |
| [weighted.WeightedTransformations](#weighted-weightedtransformations) | [WeightedTransformations.java](../src/main/java/fr/menana/automaton/weighted/WeightedTransformations.java) |
| [weighted.WeightedWord](#weighted-weightedword) | [WeightedWord.java](../src/main/java/fr/menana/automaton/weighted/WeightedWord.java) |
| [weighted.compiled.CompiledMultiWeightedDfa](#weighted-compiled-compiledmultiweighteddfa) | [CompiledMultiWeightedDfa.java](../src/main/java/fr/menana/automaton/weighted/compiled/CompiledMultiWeightedDfa.java) |
| [weighted.compiled.CompiledWeightedDfa](#weighted-compiled-compiledweighteddfa) | [CompiledWeightedDfa.java](../src/main/java/fr/menana/automaton/weighted/compiled/CompiledWeightedDfa.java) |

<a id="alphabet"></a>

## `Alphabet`

[Source and Javadoc](../src/main/java/fr/menana/automaton/Alphabet.java)

```java
public final class Alphabet {
  public static Alphabet of(Collection<IntervalSet>);
  public int size();
  public int min(int);
  public int max(int);
}
```

<a id="automaton"></a>

## `Automaton`

[Source and Javadoc](../src/main/java/fr/menana/automaton/Automaton.java)

```java
public class Automaton implements Cloneable {
  public Automaton();
  public Automaton(String);
  public AutomatonSnapshot snapshot();
  public weighted.WeightedAutomaton weighted(function.ToDoubleFunction<Transition>);
  public final weighted.MultiWeightedAutomaton multiWeighted(function.ToDoubleFunction<Transition>...);
  public List<State> getStates();
  public State getState(int);
  public int getNbStates();
  public State addState();
  public void setInitial(State);
  public void setAccept(State);
  public void setAccept(State, boolean);
  public boolean isInitial(State);
  public State getInitial();
  public List<State> getAcceptList();
  public List<State> getNonAcceptList();
  public List<Transition> getAllTransitions();
  public void addTransition(State, State, IntervalSet);
  public void addTransition(State, State, Interval);
  public void addTransition(State, State, int...);
  public void addEpsilonTransition(State, State);
  public boolean isDeterministic();
  public boolean run(int...);
  public Automaton determinize();
  public Automaton minimize();
  public Automaton revert();
  public Automaton concatenate(Automaton);
  public Automaton union(Automaton);
  public Automaton complement();
  public Automaton intersection(Automaton);
  public Automaton difference(Automaton);
  public Automaton removeEpsilon();
  public Automaton reduce();
  public CompiledDfa compile();
  public Searcher searcher();
  public Searcher.Match find(int...);
  public boolean contains(int...);
  public List<Searcher.Match> findAll(int...);
  public boolean isEmpty();
  public boolean isFinite();
  public boolean isUniversal();
  public boolean sameLanguage(Automaton);
  public boolean isSubsetOf(Automaton);
  public int[] shortestWord();
  public int[] counterExample(Automaton);
  public BigInteger countWords(int);
  public int[] randomWord(int, Random);
  public Iterable<int[]> words(int);
  public void removeDeadStates();
  public Set<State> getUseFulStates();
  public Automaton clone();
  public Set<State> delta(State, int);
  public Set<State> deltaEpsilon(State);
  public void reIndex();
  public void toDotty(String);
  public String toDot();
  public String toString();
  public boolean equals(Object);
  public int hashCode();
  public static Automaton nfaFromString(String);
  public static Automaton dfaFromString(String);
  public Object clone() throws CloneNotSupportedException;
}
```

<a id="automatonsnapshot-transitionweight"></a>

## `AutomatonSnapshot.TransitionWeight`

[Source and Javadoc](../src/main/java/fr/menana/automaton/AutomatonSnapshot.java)

```java
public interface AutomatonSnapshot.TransitionWeight {
  public abstract double weight(Transition, boolean);
}
```

<a id="automatonsnapshot"></a>

## `AutomatonSnapshot`

[Source and Javadoc](../src/main/java/fr/menana/automaton/AutomatonSnapshot.java)

```java
public final class AutomatonSnapshot {
  public static AutomatonSnapshot of(Automaton);
  public weighted.AutomatonGraph graph();
  public int stateId(State);
  public State originalState(int);
  public Transition originalTransition(int);
  public weighted.WeightedAutomaton weighted(function.ToDoubleFunction<Transition>);
  public weighted.WeightedAutomaton weighted(AutomatonSnapshot.TransitionWeight);
  public weighted.WeightedAutomaton weighted(function.ToDoubleFunction<Transition>, double, function.ToDoubleFunction<State>);
  public weighted.WeightedAutomaton weighted(AutomatonSnapshot.TransitionWeight, double, function.ToDoubleFunction<State>);
  public final weighted.MultiWeightedAutomaton multiWeighted(function.ToDoubleFunction<Transition>...);
  public boolean run(int...);
  public Automaton toAutomaton();
}
```

<a id="compileddfa"></a>

## `CompiledDfa`

[Source and Javadoc](../src/main/java/fr/menana/automaton/CompiledDfa.java)

```java
public final class CompiledDfa {
  public static final int DEAD;
  public static CompiledDfa of(Automaton);
  public int getInitialState();
  public int getNbStates();
  public boolean isAccept(int);
  public int step(int, int);
  public boolean run(int...);
  public boolean run(int[], int, int);
}
```

<a id="interval"></a>

## `Interval`

[Source and Javadoc](../src/main/java/fr/menana/automaton/Interval.java)

```java
public class Interval implements Comparable<Interval>, Cloneable {
  public int getMin();
  public int getMax();
  public Interval(int, int);
  public boolean contains(int);
  public boolean contains(Interval);
  public boolean intersects(Interval);
  public int compareTo(Interval) throws NullPointerException;
  public boolean equals(Object);
  public String toString();
  public Interval intersection(Interval);
  public int hashCode();
  public int size();
  public List<Interval> complement();
  public Interval clone();
  public Object clone() throws CloneNotSupportedException;
  public int compareTo(Object);
}
```

<a id="intervalset"></a>

## `IntervalSet`

[Source and Javadoc](../src/main/java/fr/menana/automaton/IntervalSet.java)

```java
public class IntervalSet implements Comparable<IntervalSet>, Cloneable {
  public static final IntervalSet ALL;
  public static final IntervalSet EMPTY;
  public IntervalSet();
  public int intervalCount();
  public int getIntervalMin(int);
  public int getIntervalMax(int);
  public static IntervalSet fromSortedIntervals(int[], int[], int, int);
  public boolean add(int...);
  public boolean add(int);
  public boolean add(IntervalSet);
  public boolean add(Interval);
  public void remove(IntervalSet);
  public boolean contains(int);
  public boolean isEmpty();
  public boolean intersects(IntervalSet);
  public boolean intersects(Interval);
  public IntervalSet intersection(IntervalSet);
  public IntervalSet complement();
  public IntervalSet minus(IntervalSet...);
  public IntervalSet union(IntervalSet...);
  public int getMin();
  public int getMax();
  public long size();
  public String toString();
  public IntervalSet clone();
  public static IntervalSet union(Collection<IntervalSet>);
  public TreeSet<Interval> getIntervals();
  public static IntervalSet intersection(Collection<IntervalSet>);
  public static IntervalSet fromIntArray(int...);
  public boolean equals(Object);
  public int hashCode();
  public static IntervalSet fromInterval(Interval);
  public int compareTo(IntervalSet);
  public Object clone() throws CloneNotSupportedException;
  public int compareTo(Object);
}
```

<a id="operation-minimization_algo"></a>

## `Operation.MINIMIZATION_ALGO`

[Source and Javadoc](../src/main/java/fr/menana/automaton/Operation.java)

```java
public final class Operation.MINIMIZATION_ALGO extends Enum<Operation.MINIMIZATION_ALGO> {
  public static final Operation.MINIMIZATION_ALGO Brzozowski;
  public static final Operation.MINIMIZATION_ALGO Hopcroft;
  public static Operation.MINIMIZATION_ALGO[] values();
  public static Operation.MINIMIZATION_ALGO valueOf(String);
}
```

<a id="operation"></a>

## `Operation`

[Source and Javadoc](../src/main/java/fr/menana/automaton/Operation.java)

```java
public class Operation {
  public static Operation.MINIMIZATION_ALGO getDefaultMinimization();
  public static void setDefaultMinimization(Operation.MINIMIZATION_ALGO);
  public static Automaton minimize(Automaton);
  public static Automaton minimize(Automaton, Operation.MINIMIZATION_ALGO);
  public static Automaton minimizeHopcroft(Automaton);
  public static Automaton minimizeBrzozowski(Automaton);
  public static Automaton determinize(Automaton);
  public static Automaton revert(Automaton);
  public static Automaton concatenate(Automaton, Automaton);
  public static Automaton concatenate(Automaton, Automaton, boolean);
  public static Automaton union(Automaton, Automaton);
  public static Automaton intersection(Automaton, Automaton);
  public static Automaton difference(Automaton, Automaton);
  public static Automaton removeEpsilon(Automaton);
  public static Automaton reduce(Automaton);
  public static Automaton complement(Automaton);
}
```

<a id="searcher-match"></a>

## `Searcher.Match`

[Source and Javadoc](../src/main/java/fr/menana/automaton/Searcher.java)

```java
public final class Searcher.Match extends Record {
  public Searcher.Match(int, int);
  public int length();
  public final String toString();
  public final int hashCode();
  public final boolean equals(Object);
  public int start();
  public int end();
}
```

<a id="searcher"></a>

## `Searcher`

[Source and Javadoc](../src/main/java/fr/menana/automaton/Searcher.java)

```java
public final class Searcher {
  public Searcher(Automaton);
  public Searcher.Match find(int...);
  public Searcher.Match find(int[], int);
  public boolean contains(int...);
  public List<Searcher.Match> findAll(int...);
}
```

<a id="state"></a>

## `State`

[Source and Javadoc](../src/main/java/fr/menana/automaton/State.java)

```java
public class State {
  public Map<State, Transition> getTransitions();
  public boolean isInitial();
  public boolean isAccept();
  public String toString();
  public int getIndex();
  public int compareTo(State);
  public boolean hasTransitionWith(int);
}
```

<a id="transition"></a>

## `Transition`

[Source and Javadoc](../src/main/java/fr/menana/automaton/Transition.java)

```java
public class Transition {
  public State getOrig();
  public State getDest();
  public IntervalSet getValues();
  public boolean hasValues();
  public boolean accepts(int);
  public boolean hasEpsilon();
  public boolean equals(Object);
  public int hashCode();
  public String toString();
}
```

<a id="regexp-regexp"></a>

## `regexp.RegExp`

[Source and Javadoc](../src/main/java/fr/menana/automaton/regexp/RegExp.java)

```java
public abstract class RegExp {
  public abstract String toString();
  public Automaton toNFA();
}
```

<a id="regexp-regexpparser"></a>

## `regexp.RegExpParser`

[Source and Javadoc](../src/main/java/fr/menana/automaton/regexp/RegExpParser.java)

```java
public class RegExpParser {
  public static RegExp toRegExp(String);
  public static Automaton toNFA(String);
  public static Automaton toDFA(String);
}
```

<a id="weighted-automatongraph-builder"></a>

## `weighted.AutomatonGraph.Builder`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/AutomatonGraph.java)

```java
public final class weighted.AutomatonGraph.Builder {
  public int addState();
  public void setInitial(int);
  public void setAccept(int);
  public void setAccept(int, boolean);
  public int addTransition(int, int, IntervalSet);
  public int addTransition(int, int, Interval);
  public int addTransition(int, int, int);
  public int addEpsilonTransition(int, int);
  public weighted.AutomatonGraph build();
}
```

<a id="weighted-automatongraph-importresult"></a>

## `weighted.AutomatonGraph.ImportResult`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/AutomatonGraph.java)

```java
public final class weighted.AutomatonGraph.ImportResult {
  public weighted.AutomatonGraph graph();
  public Transition originalTransition(int);
  public State originalState(int);
  public int stateId(State);
}
```

<a id="weighted-automatongraph"></a>

## `weighted.AutomatonGraph`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/AutomatonGraph.java)

```java
public final class weighted.AutomatonGraph {
  public static weighted.AutomatonGraph.Builder builder();
  public int getNbStates();
  public int getNbTransitions();
  public int getInitialState();
  public boolean isAccept(int);
  public int source(int);
  public int target(int);
  public boolean isEpsilon(int);
  public IntervalSet labels(int);
  public boolean accepts(int, int);
  public int[] outgoingEdges(int);
  public boolean isAcyclic();
  public int[] topologicalOrder();
  public boolean isDeterministic();
  public boolean run(int...);
  public Automaton toAutomaton();
  public boolean[] coaccessibleStates();
  public boolean[] usefulStates();
  public static weighted.AutomatonGraph from(Automaton);
  public static weighted.AutomatonGraph.ImportResult importAutomaton(Automaton);
}
```

<a id="weighted-kbestpaths"></a>

## `weighted.KBestPaths`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/KBestPaths.java)

```java
public final class weighted.KBestPaths {
  public static List<weighted.WeightedPath> accepting(weighted.WeightedAutomaton, int);
  public static Iterator<weighted.WeightedPath> iterator(weighted.WeightedAutomaton);
  public static List<weighted.WeightedPath> ofLength(weighted.WeightedAutomaton, int, int);
}
```

<a id="weighted-kbestwords"></a>

## `weighted.KBestWords`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/KBestWords.java)

```java
public final class weighted.KBestWords {
  public static Iterator<weighted.WeightedWord> iterator(weighted.WeightedAutomaton);
  public static List<weighted.WeightedWord> accepting(weighted.WeightedAutomaton, int);
  public static List<weighted.WeightedWord> ofLength(weighted.WeightedAutomaton, int, int);
}
```

<a id="weighted-layeredautomaton-analysis"></a>

## `weighted.LayeredAutomaton.Analysis`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/LayeredAutomaton.java)

```java
public final class weighted.LayeredAutomaton.Analysis {
  public double[] forwardDistances();
  public double[] backwardDistances();
  public double optimalCost();
  public weighted.WeightedPath bestPath();
  public double costAt(int, int);
}
```

<a id="weighted-layeredautomaton-positionweights"></a>

## `weighted.LayeredAutomaton.PositionWeights`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/LayeredAutomaton.java)

```java
public interface weighted.LayeredAutomaton.PositionWeights {
  public abstract double weight(int, int);
}
```

<a id="weighted-layeredautomaton"></a>

## `weighted.LayeredAutomaton`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/LayeredAutomaton.java)

```java
public final class weighted.LayeredAutomaton {
  public static weighted.LayeredAutomaton exactly(weighted.AutomatonGraph, int);
  public static weighted.LayeredAutomaton atMost(weighted.AutomatonGraph, int);
  public static weighted.LayeredAutomaton forWord(weighted.AutomatonGraph, int...);
  public weighted.AutomatonGraph baseGraph();
  public weighted.AutomatonGraph graph();
  public int horizon();
  public int originalState(int);
  public int position(int);
  public int originalEdge(int);
  public weighted.WeightedAutomaton evaluate(weighted.WeightedAutomaton);
  public weighted.WeightedAutomaton evaluate(weighted.WeightedAutomaton, weighted.LayeredAutomaton.PositionWeights);
  public weighted.MultiWeightedAutomaton evaluate(weighted.MultiWeightedAutomaton);
  public weighted.WeightedPath bestPath(weighted.WeightedAutomaton);
  public weighted.WeightedPath map(weighted.WeightedPath);
  public weighted.MultiWeightedPath map(weighted.MultiWeightedPath);
  public weighted.LayeredAutomaton.Analysis analyze(weighted.WeightedAutomaton);
  public weighted.LayeredAutomaton.Analysis analyze(weighted.WeightedAutomaton, weighted.LayeredAutomaton.PositionWeights);
}
```

<a id="weighted-multiweightedautomaton-builder"></a>

## `weighted.MultiWeightedAutomaton.Builder`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/MultiWeightedAutomaton.java)

```java
public final class weighted.MultiWeightedAutomaton.Builder {
  public int addState();
  public void setInitial(int, double...);
  public void setAccept(int, double...);
  public int addTransition(int, int, IntervalSet, double...);
  public int addTransition(int, int, Interval, double...);
  public int addTransition(int, int, int, double...);
  public int addEpsilonTransition(int, int, double...);
  public weighted.MultiWeightedAutomaton build();
}
```

<a id="weighted-multiweightedautomaton"></a>

## `weighted.MultiWeightedAutomaton`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/MultiWeightedAutomaton.java)

```java
public final class weighted.MultiWeightedAutomaton {
  public static weighted.MultiWeightedAutomaton of(weighted.AutomatonGraph, int, double[]);
  public static weighted.MultiWeightedAutomaton from(Automaton, function.ToDoubleFunction<Transition>...);
  public static weighted.MultiWeightedAutomaton of(weighted.AutomatonGraph, int, double[], double[], double[]);
  public static weighted.MultiWeightedAutomaton fromWeightings(weighted.WeightedAutomaton...);
  public static weighted.MultiWeightedAutomaton synchronizedProduct(weighted.WeightedAutomaton...);
  public weighted.compiled.CompiledMultiWeightedDfa compile();
  public static weighted.MultiWeightedAutomaton.Builder builder(int);
  public weighted.AutomatonGraph graph();
  public boolean run(int...);
  public Automaton toAutomaton();
  public int dimensions();
  public double weight(int, int);
  public double initialWeight(int);
  public double finalWeight(int, int);
  public double[] weights(int);
  public weighted.WeightedAutomaton project(int);
  public weighted.MultiWeightedAutomaton selectDimensions(int...);
  public weighted.WeightedAutomaton scalarize(double...);
  public weighted.WeightedAutomaton scalarizeMaterialized(double...);
  public double[] pathCosts(weighted.WeightedPath);
  public weighted.MultiWeightedPath lexicographicBestPath(int...);
  public weighted.ParetoResult paretoFront();
  public weighted.ParetoResult paretoFront(boolean);
  public weighted.ParetoResult paretoForWord(int...);
  public weighted.MultiWeightedAutomaton union(weighted.MultiWeightedAutomaton);
  public weighted.MultiWeightedAutomaton concatenate(weighted.MultiWeightedAutomaton);
  public weighted.MultiWeightedAutomaton intersection(weighted.MultiWeightedAutomaton);
  public weighted.MultiWeightedAutomaton revert();
  public weighted.MultiWeightedAutomaton simplify();
}
```

<a id="weighted-multiweightedoperations"></a>

## `weighted.MultiWeightedOperations`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/MultiWeightedOperations.java)

```java
public final class weighted.MultiWeightedOperations {
  public static weighted.MultiWeightedAutomaton union(weighted.MultiWeightedAutomaton, weighted.MultiWeightedAutomaton);
  public static weighted.MultiWeightedAutomaton concatenate(weighted.MultiWeightedAutomaton, weighted.MultiWeightedAutomaton);
  public static weighted.MultiWeightedAutomaton revert(weighted.MultiWeightedAutomaton);
  public static weighted.MultiWeightedAutomaton intersection(weighted.MultiWeightedAutomaton, weighted.MultiWeightedAutomaton);
  public static weighted.MultiWeightedAutomaton productIndependent(weighted.MultiWeightedAutomaton, weighted.MultiWeightedAutomaton);
  public static weighted.MultiWeightedAutomaton synchronizedProduct(weighted.WeightedAutomaton...);
  public static weighted.MultiWeightedAutomaton concatenateIndependent(weighted.MultiWeightedAutomaton, weighted.MultiWeightedAutomaton);
  public static weighted.MultiWeightedAutomaton trim(weighted.MultiWeightedAutomaton);
  public static weighted.MultiWeightedAutomaton simplify(weighted.MultiWeightedAutomaton);
}
```

<a id="weighted-multiweightedpath"></a>

## `weighted.MultiWeightedPath`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/MultiWeightedPath.java)

```java
public final class weighted.MultiWeightedPath {
  public weighted.AutomatonGraph graph();
  public int start();
  public int end();
  public int[] edges();
  public int[] word();
  public int length();
  public double[] costs();
  public double cost(int);
  public BigDecimal exactCost(int);
}
```

<a id="weighted-multiweightedpaths"></a>

## `weighted.MultiWeightedPaths`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/MultiWeightedPaths.java)

```java
public final class weighted.MultiWeightedPaths {
  public static weighted.MultiWeightedPath lexicographic(weighted.MultiWeightedAutomaton, int...);
  public static boolean dominates(double[], double[]);
  public static weighted.ParetoResult pareto(weighted.MultiWeightedAutomaton, boolean);
}
```

<a id="weighted-paretoresult"></a>

## `weighted.ParetoResult`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/ParetoResult.java)

```java
public final class weighted.ParetoResult {
  public List<weighted.MultiWeightedPath> paths();
  public int size();
  public boolean includesAllPaths();
  public boolean isComplete();
}
```

<a id="weighted-shortestpathresult-status"></a>

## `weighted.ShortestPathResult.Status`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/ShortestPathResult.java)

```java
public final class weighted.ShortestPathResult.Status extends Enum<weighted.ShortestPathResult.Status> {
  public static final weighted.ShortestPathResult.Status FINITE;
  public static final weighted.ShortestPathResult.Status UNREACHABLE;
  public static final weighted.ShortestPathResult.Status UNBOUNDED;
  public static weighted.ShortestPathResult.Status[] values();
  public static weighted.ShortestPathResult.Status valueOf(String);
}
```

<a id="weighted-shortestpathresult"></a>

## `weighted.ShortestPathResult`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/ShortestPathResult.java)

```java
public final class weighted.ShortestPathResult {
  public double distanceTo(int);
  public double[] distances();
  public weighted.ShortestPathResult.Status statusTo(int);
  public weighted.WeightedPath pathTo(int);
  public double optimalCost();
  public weighted.WeightedPath bestPath();
}
```

<a id="weighted-weightedautomaton-builder"></a>

## `weighted.WeightedAutomaton.Builder`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/WeightedAutomaton.java)

```java
public final class weighted.WeightedAutomaton.Builder {
  public int addState();
  public void setInitial(int);
  public void setInitial(int, double);
  public void setAccept(int);
  public void setAccept(int, double);
  public int addTransition(int, int, IntervalSet, double);
  public int addTransition(int, int, Interval, double);
  public int addTransition(int, int, int, double);
  public int addEpsilonTransition(int, int, double);
  public weighted.WeightedAutomaton build();
}
```

<a id="weighted-weightedautomaton-evaluator"></a>

## `weighted.WeightedAutomaton.Evaluator`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/WeightedAutomaton.java)

```java
public final class weighted.WeightedAutomaton.Evaluator {
  public double cost(int...);
  public double optimalCostWithPrefix(int...);
}
```

<a id="weighted-weightedautomaton"></a>

## `weighted.WeightedAutomaton`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/WeightedAutomaton.java)

```java
public final class weighted.WeightedAutomaton {
  public static weighted.WeightedAutomaton of(weighted.AutomatonGraph, double[]);
  public static weighted.WeightedAutomaton of(weighted.AutomatonGraph, double[], double, double[]);
  public static weighted.WeightedAutomaton from(Automaton, function.ToDoubleFunction<Transition>);
  public static weighted.WeightedAutomaton.Builder builder();
  public weighted.AutomatonGraph graph();
  public boolean run(int...);
  public Automaton toAutomaton();
  public double weight(int);
  public double initialWeight();
  public double finalWeight(int);
  public weighted.WeightedAutomaton withWeights(double[]);
  public double cost(int...);
  public weighted.WeightedAutomaton.Evaluator evaluator();
  public double optimalCost();
  public weighted.WeightedPath bestPath();
  public weighted.WeightedPath bestPath(int...);
  public weighted.WeightedAutomaton union(weighted.WeightedAutomaton);
  public weighted.WeightedAutomaton concatenate(weighted.WeightedAutomaton);
  public weighted.WeightedAutomaton intersection(weighted.WeightedAutomaton);
  public weighted.WeightedAutomaton revert();
  public weighted.WeightedAutomaton star();
  public weighted.WeightedAutomaton trim();
  public double costOfLength(int);
  public double costOfLengthAtMost(int);
  public weighted.WeightedPath bestPathOfLength(int);
  public int[] bestWordOfLength(int);
  public weighted.WeightedAutomaton removeEpsilon();
  public weighted.WeightedAutomaton determinize();
  public weighted.WeightedAutomaton minimize();
  public weighted.compiled.CompiledWeightedDfa compile();
  public List<weighted.WeightedPath> kBestPaths(int);
  public weighted.WeightedAutomaton determinize(int);
  public Iterator<weighted.WeightedPath> pathsByCost();
  public List<weighted.WeightedWord> kBestWords(int);
  public Iterator<weighted.WeightedWord> wordsByCost();
  public double optimalCostWithPrefix(int...);
}
```

<a id="weighted-weighteddfaindex"></a>

## `weighted.WeightedDfaIndex`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/WeightedDfaIndex.java)

```java
public final class weighted.WeightedDfaIndex {
  public static weighted.WeightedDfaIndex of(weighted.AutomatonGraph);
  public weighted.AutomatonGraph graph();
  public int transition(int, int);
}
```

<a id="weighted-weightedoperations"></a>

## `weighted.WeightedOperations`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/WeightedOperations.java)

```java
public final class weighted.WeightedOperations {
  public static weighted.WeightedAutomaton union(weighted.WeightedAutomaton, weighted.WeightedAutomaton);
  public static weighted.WeightedAutomaton concatenate(weighted.WeightedAutomaton, weighted.WeightedAutomaton);
  public static weighted.WeightedAutomaton intersection(weighted.WeightedAutomaton, weighted.WeightedAutomaton);
  public static weighted.WeightedAutomaton revert(weighted.WeightedAutomaton);
  public static weighted.WeightedAutomaton star(weighted.WeightedAutomaton);
  public static weighted.WeightedAutomaton repeat(weighted.WeightedAutomaton, int, int);
  public static weighted.WeightedAutomaton trim(weighted.WeightedAutomaton);
  public static weighted.WeightedAutomaton simplify(weighted.WeightedAutomaton);
}
```

<a id="weighted-weightedpath"></a>

## `weighted.WeightedPath`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/WeightedPath.java)

```java
public final class weighted.WeightedPath {
  public weighted.AutomatonGraph graph();
  public int start();
  public int end();
  public int edgeCount();
  public int length();
  public int edge(int);
  public int[] edges();
  public int[] word();
  public double cost();
}
```

<a id="weighted-weightedpaths-analysis"></a>

## `weighted.WeightedPaths.Analysis`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/WeightedPaths.java)

```java
public final class weighted.WeightedPaths.Analysis {
  public weighted.ShortestPathResult forward();
  public double[] backward();
  public double costThroughState(int);
  public double costThroughTransition(int);
}
```

<a id="weighted-weightedpaths"></a>

## `weighted.WeightedPaths`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/WeightedPaths.java)

```java
public final class weighted.WeightedPaths {
  public static weighted.ShortestPathResult shortestPaths(weighted.WeightedAutomaton);
  public static weighted.ShortestPathResult shortestPaths(weighted.WeightedAutomaton, boolean);
  public static weighted.ShortestPathResult shortestPaths(weighted.WeightedAutomaton, int);
  public static weighted.ShortestPathResult dagShortestPaths(weighted.WeightedAutomaton);
  public static weighted.ShortestPathResult dagLongestPaths(weighted.WeightedAutomaton);
  public static weighted.ShortestPathResult dijkstra(weighted.WeightedAutomaton);
  public static weighted.ShortestPathResult bellmanFord(weighted.WeightedAutomaton);
  public static double[] backwardDistances(weighted.WeightedAutomaton);
  public static double[] distancesTo(weighted.WeightedAutomaton, int);
  public static weighted.WeightedPaths.Analysis analyze(weighted.WeightedAutomaton);
  public static double costThroughState(weighted.WeightedAutomaton, int);
  public static double costThroughTransition(weighted.WeightedAutomaton, int);
  public static double fixedLengthCost(weighted.WeightedAutomaton, int, boolean);
}
```

<a id="weighted-weightedtransformations"></a>

## `weighted.WeightedTransformations`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/WeightedTransformations.java)

```java
public final class weighted.WeightedTransformations {
  public static final int DEFAULT_MAX_STATES;
  public static weighted.WeightedAutomaton removeEpsilon(weighted.WeightedAutomaton);
  public static weighted.WeightedAutomaton determinizeDag(weighted.WeightedAutomaton);
  public static weighted.WeightedAutomaton determinizeDag(weighted.WeightedAutomaton, int);
  public static weighted.WeightedAutomaton minimizeDag(weighted.WeightedAutomaton);
  public static weighted.WeightedAutomaton determinize(weighted.WeightedAutomaton, int);
  public static weighted.WeightedAutomaton determinize(weighted.WeightedAutomaton);
  public static weighted.WeightedAutomaton minimize(weighted.WeightedAutomaton);
}
```

<a id="weighted-weightedword"></a>

## `weighted.WeightedWord`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/WeightedWord.java)

```java
public final class weighted.WeightedWord {
  public int[] word();
  public int length();
  public double cost();
}
```

<a id="weighted-compiled-compiledmultiweighteddfa"></a>

## `weighted.compiled.CompiledMultiWeightedDfa`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/compiled/CompiledMultiWeightedDfa.java)

```java
public final class weighted.compiled.CompiledMultiWeightedDfa {
  public static weighted.compiled.CompiledMultiWeightedDfa of(weighted.MultiWeightedAutomaton);
  public static weighted.compiled.CompiledMultiWeightedDfa of(weighted.MultiWeightedAutomaton, weighted.WeightedDfaIndex);
  public weighted.WeightedDfaIndex index();
  public int dimensions();
  public weighted.AutomatonGraph graph();
  public int transition(int, int);
  public weighted.compiled.CompiledMultiWeightedDfa reweight(weighted.MultiWeightedAutomaton);
  public weighted.compiled.CompiledWeightedDfa project(int);
  public weighted.compiled.CompiledWeightedDfa scalarize(double...);
  public double[] costs(int...);
  public double[] costs(int[], int, int);
  public boolean costs(int[], double[]);
  public boolean costs(int[], int, int, double[]);
}
```

<a id="weighted-compiled-compiledweighteddfa"></a>

## `weighted.compiled.CompiledWeightedDfa`

[Source and Javadoc](../src/main/java/fr/menana/automaton/weighted/compiled/CompiledWeightedDfa.java)

```java
public final class weighted.compiled.CompiledWeightedDfa {
  public static weighted.compiled.CompiledWeightedDfa of(weighted.WeightedAutomaton);
  public static weighted.compiled.CompiledWeightedDfa of(weighted.WeightedAutomaton, weighted.WeightedDfaIndex);
  public weighted.WeightedDfaIndex index();
  public weighted.compiled.CompiledWeightedDfa reweight(weighted.WeightedAutomaton);
  public int transition(int, int);
  public double cost(int...);
  public double cost(int[], int, int);
}
```

[Documentation index](README.md)
