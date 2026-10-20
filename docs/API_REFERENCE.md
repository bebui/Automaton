# Complete public API reference

Generated from the compiled Java 17 classes in this checkout. Regenerate with
`python3 docs/generate_api_reference.py` after building the library.
This lists declared public constructors, methods, overloads and constants,
including public nested types. Inherited `Object` methods are not repeated.
Java/JDK and library package prefixes are shortened for readability.
Parameter contracts, return/absence conventions and algorithm preconditions
are explained in the [topic guides](README.md) and source Javadoc.

**39 public types.**

| Type | Source |
|---|---|
| [Automaton](#automaton) | [Automaton.java](../src/main/java/fr/menana/automaton/Automaton.java) |
| [AutomatonGraph.Builder](#automatongraph-builder) | [AutomatonGraph.java](../src/main/java/fr/menana/automaton/AutomatonGraph.java) |
| [AutomatonGraph.ImportResult](#automatongraph-importresult) | [AutomatonGraph.java](../src/main/java/fr/menana/automaton/AutomatonGraph.java) |
| [AutomatonGraph](#automatongraph) | [AutomatonGraph.java](../src/main/java/fr/menana/automaton/AutomatonGraph.java) |
| [CompiledDfa](#compileddfa) | [CompiledDfa.java](../src/main/java/fr/menana/automaton/CompiledDfa.java) |
| [CompiledMultiWeightedDfa](#compiledmultiweighteddfa) | [CompiledMultiWeightedDfa.java](../src/main/java/fr/menana/automaton/CompiledMultiWeightedDfa.java) |
| [CompiledWeightedDfa](#compiledweighteddfa) | [CompiledWeightedDfa.java](../src/main/java/fr/menana/automaton/CompiledWeightedDfa.java) |
| [Interval](#interval) | [Interval.java](../src/main/java/fr/menana/automaton/Interval.java) |
| [IntervalSet](#intervalset) | [IntervalSet.java](../src/main/java/fr/menana/automaton/IntervalSet.java) |
| [KBestPaths](#kbestpaths) | [KBestPaths.java](../src/main/java/fr/menana/automaton/KBestPaths.java) |
| [KBestWords](#kbestwords) | [KBestWords.java](../src/main/java/fr/menana/automaton/KBestWords.java) |
| [LayeredAutomaton.Analysis](#layeredautomaton-analysis) | [LayeredAutomaton.java](../src/main/java/fr/menana/automaton/LayeredAutomaton.java) |
| [LayeredAutomaton.PositionWeights](#layeredautomaton-positionweights) | [LayeredAutomaton.java](../src/main/java/fr/menana/automaton/LayeredAutomaton.java) |
| [LayeredAutomaton](#layeredautomaton) | [LayeredAutomaton.java](../src/main/java/fr/menana/automaton/LayeredAutomaton.java) |
| [MultiWeightedAutomaton.Builder](#multiweightedautomaton-builder) | [MultiWeightedAutomaton.java](../src/main/java/fr/menana/automaton/MultiWeightedAutomaton.java) |
| [MultiWeightedAutomaton](#multiweightedautomaton) | [MultiWeightedAutomaton.java](../src/main/java/fr/menana/automaton/MultiWeightedAutomaton.java) |
| [MultiWeightedOperations](#multiweightedoperations) | [MultiWeightedOperations.java](../src/main/java/fr/menana/automaton/MultiWeightedOperations.java) |
| [MultiWeightedPath](#multiweightedpath) | [MultiWeightedPath.java](../src/main/java/fr/menana/automaton/MultiWeightedPath.java) |
| [MultiWeightedPaths](#multiweightedpaths) | [MultiWeightedPaths.java](../src/main/java/fr/menana/automaton/MultiWeightedPaths.java) |
| [Operation.MINIMIZATION_ALGO](#operation-minimization_algo) | [Operation.java](../src/main/java/fr/menana/automaton/Operation.java) |
| [Operation](#operation) | [Operation.java](../src/main/java/fr/menana/automaton/Operation.java) |
| [ParetoResult](#paretoresult) | [ParetoResult.java](../src/main/java/fr/menana/automaton/ParetoResult.java) |
| [Searcher.Match](#searcher-match) | [Searcher.java](../src/main/java/fr/menana/automaton/Searcher.java) |
| [Searcher](#searcher) | [Searcher.java](../src/main/java/fr/menana/automaton/Searcher.java) |
| [ShortestPathResult.Status](#shortestpathresult-status) | [ShortestPathResult.java](../src/main/java/fr/menana/automaton/ShortestPathResult.java) |
| [ShortestPathResult](#shortestpathresult) | [ShortestPathResult.java](../src/main/java/fr/menana/automaton/ShortestPathResult.java) |
| [State](#state) | [State.java](../src/main/java/fr/menana/automaton/State.java) |
| [Transition](#transition) | [Transition.java](../src/main/java/fr/menana/automaton/Transition.java) |
| [WeightedAutomaton.Builder](#weightedautomaton-builder) | [WeightedAutomaton.java](../src/main/java/fr/menana/automaton/WeightedAutomaton.java) |
| [WeightedAutomaton.Evaluator](#weightedautomaton-evaluator) | [WeightedAutomaton.java](../src/main/java/fr/menana/automaton/WeightedAutomaton.java) |
| [WeightedAutomaton](#weightedautomaton) | [WeightedAutomaton.java](../src/main/java/fr/menana/automaton/WeightedAutomaton.java) |
| [WeightedOperations](#weightedoperations) | [WeightedOperations.java](../src/main/java/fr/menana/automaton/WeightedOperations.java) |
| [WeightedPath](#weightedpath) | [WeightedPath.java](../src/main/java/fr/menana/automaton/WeightedPath.java) |
| [WeightedPaths.Analysis](#weightedpaths-analysis) | [WeightedPaths.java](../src/main/java/fr/menana/automaton/WeightedPaths.java) |
| [WeightedPaths](#weightedpaths) | [WeightedPaths.java](../src/main/java/fr/menana/automaton/WeightedPaths.java) |
| [WeightedTransformations](#weightedtransformations) | [WeightedTransformations.java](../src/main/java/fr/menana/automaton/WeightedTransformations.java) |
| [WeightedWord](#weightedword) | [WeightedWord.java](../src/main/java/fr/menana/automaton/WeightedWord.java) |
| [regexp.RegExp](#regexp-regexp) | [RegExp.java](../src/main/java/fr/menana/automaton/regexp/RegExp.java) |
| [regexp.RegExpParser](#regexp-regexpparser) | [RegExpParser.java](../src/main/java/fr/menana/automaton/regexp/RegExpParser.java) |

<a id="automaton"></a>

## `Automaton`

[Source and Javadoc](../src/main/java/fr/menana/automaton/Automaton.java)

```java
public class Automaton implements Cloneable {
  public Automaton();
  public Automaton(String);
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

<a id="automatongraph-builder"></a>

## `AutomatonGraph.Builder`

[Source and Javadoc](../src/main/java/fr/menana/automaton/AutomatonGraph.java)

```java
public final class AutomatonGraph.Builder {
  public int addState();
  public void setInitial(int);
  public void setAccept(int);
  public void setAccept(int, boolean);
  public int addTransition(int, int, IntervalSet);
  public int addTransition(int, int, Interval);
  public int addTransition(int, int, int);
  public int addEpsilonTransition(int, int);
  public AutomatonGraph build();
}
```

<a id="automatongraph-importresult"></a>

## `AutomatonGraph.ImportResult`

[Source and Javadoc](../src/main/java/fr/menana/automaton/AutomatonGraph.java)

```java
public final class AutomatonGraph.ImportResult {
  public AutomatonGraph graph();
  public Transition originalTransition(int);
}
```

<a id="automatongraph"></a>

## `AutomatonGraph`

[Source and Javadoc](../src/main/java/fr/menana/automaton/AutomatonGraph.java)

```java
public final class AutomatonGraph {
  public static AutomatonGraph.Builder builder();
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
  public boolean[] usefulStates();
  public static AutomatonGraph from(Automaton);
  public static AutomatonGraph.ImportResult importAutomaton(Automaton);
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

<a id="compiledmultiweighteddfa"></a>

## `CompiledMultiWeightedDfa`

[Source and Javadoc](../src/main/java/fr/menana/automaton/CompiledMultiWeightedDfa.java)

```java
public final class CompiledMultiWeightedDfa {
  public static CompiledMultiWeightedDfa of(MultiWeightedAutomaton);
  public int dimensions();
  public AutomatonGraph graph();
  public int transition(int, int);
  public CompiledMultiWeightedDfa reweight(MultiWeightedAutomaton);
  public CompiledWeightedDfa project(int);
  public CompiledWeightedDfa scalarize(double...);
  public double[] costs(int...);
  public double[] costs(int[], int, int);
  public boolean costs(int[], double[]);
  public boolean costs(int[], int, int, double[]);
}
```

<a id="compiledweighteddfa"></a>

## `CompiledWeightedDfa`

[Source and Javadoc](../src/main/java/fr/menana/automaton/CompiledWeightedDfa.java)

```java
public final class CompiledWeightedDfa {
  public static CompiledWeightedDfa of(WeightedAutomaton);
  public CompiledWeightedDfa reweight(WeightedAutomaton);
  public int transition(int, int);
  public double cost(int...);
  public double cost(int[], int, int);
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

<a id="kbestpaths"></a>

## `KBestPaths`

[Source and Javadoc](../src/main/java/fr/menana/automaton/KBestPaths.java)

```java
public final class KBestPaths {
  public static List<WeightedPath> accepting(WeightedAutomaton, int);
  public static Iterator<WeightedPath> iterator(WeightedAutomaton);
  public static List<WeightedPath> ofLength(WeightedAutomaton, int, int);
}
```

<a id="kbestwords"></a>

## `KBestWords`

[Source and Javadoc](../src/main/java/fr/menana/automaton/KBestWords.java)

```java
public final class KBestWords {
  public static Iterator<WeightedWord> iterator(WeightedAutomaton);
  public static List<WeightedWord> accepting(WeightedAutomaton, int);
  public static List<WeightedWord> ofLength(WeightedAutomaton, int, int);
}
```

<a id="layeredautomaton-analysis"></a>

## `LayeredAutomaton.Analysis`

[Source and Javadoc](../src/main/java/fr/menana/automaton/LayeredAutomaton.java)

```java
public final class LayeredAutomaton.Analysis {
  public double[] forwardDistances();
  public double[] backwardDistances();
  public double optimalCost();
  public WeightedPath bestPath();
  public double costAt(int, int);
}
```

<a id="layeredautomaton-positionweights"></a>

## `LayeredAutomaton.PositionWeights`

[Source and Javadoc](../src/main/java/fr/menana/automaton/LayeredAutomaton.java)

```java
public interface LayeredAutomaton.PositionWeights {
  public abstract double weight(int, int);
}
```

<a id="layeredautomaton"></a>

## `LayeredAutomaton`

[Source and Javadoc](../src/main/java/fr/menana/automaton/LayeredAutomaton.java)

```java
public final class LayeredAutomaton {
  public static LayeredAutomaton exactly(AutomatonGraph, int);
  public static LayeredAutomaton atMost(AutomatonGraph, int);
  public static LayeredAutomaton forWord(AutomatonGraph, int...);
  public AutomatonGraph baseGraph();
  public AutomatonGraph graph();
  public int horizon();
  public int originalState(int);
  public int position(int);
  public int originalEdge(int);
  public WeightedAutomaton evaluate(WeightedAutomaton);
  public WeightedAutomaton evaluate(WeightedAutomaton, LayeredAutomaton.PositionWeights);
  public MultiWeightedAutomaton evaluate(MultiWeightedAutomaton);
  public WeightedPath bestPath(WeightedAutomaton);
  public WeightedPath map(WeightedPath);
  public MultiWeightedPath map(MultiWeightedPath);
  public LayeredAutomaton.Analysis analyze(WeightedAutomaton);
  public LayeredAutomaton.Analysis analyze(WeightedAutomaton, LayeredAutomaton.PositionWeights);
}
```

<a id="multiweightedautomaton-builder"></a>

## `MultiWeightedAutomaton.Builder`

[Source and Javadoc](../src/main/java/fr/menana/automaton/MultiWeightedAutomaton.java)

```java
public final class MultiWeightedAutomaton.Builder {
  public int addState();
  public void setInitial(int, double...);
  public void setAccept(int, double...);
  public int addTransition(int, int, IntervalSet, double...);
  public int addTransition(int, int, Interval, double...);
  public int addTransition(int, int, int, double...);
  public int addEpsilonTransition(int, int, double...);
  public MultiWeightedAutomaton build();
}
```

<a id="multiweightedautomaton"></a>

## `MultiWeightedAutomaton`

[Source and Javadoc](../src/main/java/fr/menana/automaton/MultiWeightedAutomaton.java)

```java
public final class MultiWeightedAutomaton {
  public static MultiWeightedAutomaton of(AutomatonGraph, int, double[]);
  public static MultiWeightedAutomaton of(AutomatonGraph, int, double[], double[], double[]);
  public static MultiWeightedAutomaton fromWeightings(WeightedAutomaton...);
  public static MultiWeightedAutomaton synchronizedProduct(WeightedAutomaton...);
  public CompiledMultiWeightedDfa compile();
  public static MultiWeightedAutomaton.Builder builder(int);
  public AutomatonGraph graph();
  public int dimensions();
  public double weight(int, int);
  public double initialWeight(int);
  public double finalWeight(int, int);
  public double[] weights(int);
  public WeightedAutomaton project(int);
  public MultiWeightedAutomaton selectDimensions(int...);
  public WeightedAutomaton scalarize(double...);
  public WeightedAutomaton scalarizeMaterialized(double...);
  public double[] pathCosts(WeightedPath);
  public MultiWeightedPath lexicographicBestPath(int...);
  public ParetoResult paretoFront();
  public ParetoResult paretoFront(boolean);
  public ParetoResult paretoForWord(int...);
  public MultiWeightedAutomaton union(MultiWeightedAutomaton);
  public MultiWeightedAutomaton concatenate(MultiWeightedAutomaton);
  public MultiWeightedAutomaton intersection(MultiWeightedAutomaton);
  public MultiWeightedAutomaton revert();
  public MultiWeightedAutomaton simplify();
}
```

<a id="multiweightedoperations"></a>

## `MultiWeightedOperations`

[Source and Javadoc](../src/main/java/fr/menana/automaton/MultiWeightedOperations.java)

```java
public final class MultiWeightedOperations {
  public static MultiWeightedAutomaton union(MultiWeightedAutomaton, MultiWeightedAutomaton);
  public static MultiWeightedAutomaton concatenate(MultiWeightedAutomaton, MultiWeightedAutomaton);
  public static MultiWeightedAutomaton revert(MultiWeightedAutomaton);
  public static MultiWeightedAutomaton intersection(MultiWeightedAutomaton, MultiWeightedAutomaton);
  public static MultiWeightedAutomaton productIndependent(MultiWeightedAutomaton, MultiWeightedAutomaton);
  public static MultiWeightedAutomaton synchronizedProduct(WeightedAutomaton...);
  public static MultiWeightedAutomaton concatenateIndependent(MultiWeightedAutomaton, MultiWeightedAutomaton);
  public static MultiWeightedAutomaton trim(MultiWeightedAutomaton);
  public static MultiWeightedAutomaton simplify(MultiWeightedAutomaton);
}
```

<a id="multiweightedpath"></a>

## `MultiWeightedPath`

[Source and Javadoc](../src/main/java/fr/menana/automaton/MultiWeightedPath.java)

```java
public final class MultiWeightedPath {
  public AutomatonGraph graph();
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

<a id="multiweightedpaths"></a>

## `MultiWeightedPaths`

[Source and Javadoc](../src/main/java/fr/menana/automaton/MultiWeightedPaths.java)

```java
public final class MultiWeightedPaths {
  public static MultiWeightedPath lexicographic(MultiWeightedAutomaton, int...);
  public static boolean dominates(double[], double[]);
  public static ParetoResult pareto(MultiWeightedAutomaton, boolean);
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

<a id="paretoresult"></a>

## `ParetoResult`

[Source and Javadoc](../src/main/java/fr/menana/automaton/ParetoResult.java)

```java
public final class ParetoResult {
  public List<MultiWeightedPath> paths();
  public int size();
  public boolean includesAllPaths();
  public boolean isComplete();
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

<a id="shortestpathresult-status"></a>

## `ShortestPathResult.Status`

[Source and Javadoc](../src/main/java/fr/menana/automaton/ShortestPathResult.java)

```java
public final class ShortestPathResult.Status extends Enum<ShortestPathResult.Status> {
  public static final ShortestPathResult.Status FINITE;
  public static final ShortestPathResult.Status UNREACHABLE;
  public static final ShortestPathResult.Status UNBOUNDED;
  public static ShortestPathResult.Status[] values();
  public static ShortestPathResult.Status valueOf(String);
}
```

<a id="shortestpathresult"></a>

## `ShortestPathResult`

[Source and Javadoc](../src/main/java/fr/menana/automaton/ShortestPathResult.java)

```java
public final class ShortestPathResult {
  public double distanceTo(int);
  public double[] distances();
  public ShortestPathResult.Status statusTo(int);
  public WeightedPath pathTo(int);
  public double optimalCost();
  public WeightedPath bestPath();
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

<a id="weightedautomaton-builder"></a>

## `WeightedAutomaton.Builder`

[Source and Javadoc](../src/main/java/fr/menana/automaton/WeightedAutomaton.java)

```java
public final class WeightedAutomaton.Builder {
  public int addState();
  public void setInitial(int);
  public void setInitial(int, double);
  public void setAccept(int);
  public void setAccept(int, double);
  public int addTransition(int, int, IntervalSet, double);
  public int addTransition(int, int, Interval, double);
  public int addTransition(int, int, int, double);
  public int addEpsilonTransition(int, int, double);
  public WeightedAutomaton build();
}
```

<a id="weightedautomaton-evaluator"></a>

## `WeightedAutomaton.Evaluator`

[Source and Javadoc](../src/main/java/fr/menana/automaton/WeightedAutomaton.java)

```java
public final class WeightedAutomaton.Evaluator {
  public double cost(int...);
  public double optimalCostWithPrefix(int...);
}
```

<a id="weightedautomaton"></a>

## `WeightedAutomaton`

[Source and Javadoc](../src/main/java/fr/menana/automaton/WeightedAutomaton.java)

```java
public final class WeightedAutomaton {
  public static WeightedAutomaton of(AutomatonGraph, double[]);
  public static WeightedAutomaton of(AutomatonGraph, double[], double, double[]);
  public static WeightedAutomaton from(Automaton, function.ToDoubleFunction<Transition>);
  public static WeightedAutomaton.Builder builder();
  public AutomatonGraph graph();
  public double weight(int);
  public double initialWeight();
  public double finalWeight(int);
  public WeightedAutomaton withWeights(double[]);
  public double cost(int...);
  public WeightedAutomaton.Evaluator evaluator();
  public double optimalCost();
  public WeightedPath bestPath();
  public WeightedPath bestPath(int...);
  public WeightedAutomaton union(WeightedAutomaton);
  public WeightedAutomaton concatenate(WeightedAutomaton);
  public WeightedAutomaton intersection(WeightedAutomaton);
  public WeightedAutomaton revert();
  public WeightedAutomaton star();
  public WeightedAutomaton trim();
  public double costOfLength(int);
  public double costOfLengthAtMost(int);
  public WeightedPath bestPathOfLength(int);
  public int[] bestWordOfLength(int);
  public WeightedAutomaton removeEpsilon();
  public WeightedAutomaton determinize();
  public WeightedAutomaton minimize();
  public CompiledWeightedDfa compile();
  public List<WeightedPath> kBestPaths(int);
  public WeightedAutomaton determinize(int);
  public Iterator<WeightedPath> pathsByCost();
  public List<WeightedWord> kBestWords(int);
  public Iterator<WeightedWord> wordsByCost();
  public double optimalCostWithPrefix(int...);
}
```

<a id="weightedoperations"></a>

## `WeightedOperations`

[Source and Javadoc](../src/main/java/fr/menana/automaton/WeightedOperations.java)

```java
public final class WeightedOperations {
  public static WeightedAutomaton union(WeightedAutomaton, WeightedAutomaton);
  public static WeightedAutomaton concatenate(WeightedAutomaton, WeightedAutomaton);
  public static WeightedAutomaton intersection(WeightedAutomaton, WeightedAutomaton);
  public static WeightedAutomaton revert(WeightedAutomaton);
  public static WeightedAutomaton star(WeightedAutomaton);
  public static WeightedAutomaton repeat(WeightedAutomaton, int, int);
  public static WeightedAutomaton trim(WeightedAutomaton);
  public static WeightedAutomaton simplify(WeightedAutomaton);
}
```

<a id="weightedpath"></a>

## `WeightedPath`

[Source and Javadoc](../src/main/java/fr/menana/automaton/WeightedPath.java)

```java
public final class WeightedPath {
  public AutomatonGraph graph();
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

<a id="weightedpaths-analysis"></a>

## `WeightedPaths.Analysis`

[Source and Javadoc](../src/main/java/fr/menana/automaton/WeightedPaths.java)

```java
public final class WeightedPaths.Analysis {
  public ShortestPathResult forward();
  public double[] backward();
  public double costThroughState(int);
  public double costThroughTransition(int);
}
```

<a id="weightedpaths"></a>

## `WeightedPaths`

[Source and Javadoc](../src/main/java/fr/menana/automaton/WeightedPaths.java)

```java
public final class WeightedPaths {
  public static ShortestPathResult shortestPaths(WeightedAutomaton);
  public static ShortestPathResult shortestPaths(WeightedAutomaton, boolean);
  public static ShortestPathResult shortestPaths(WeightedAutomaton, int);
  public static ShortestPathResult dagShortestPaths(WeightedAutomaton);
  public static ShortestPathResult dagLongestPaths(WeightedAutomaton);
  public static ShortestPathResult dijkstra(WeightedAutomaton);
  public static ShortestPathResult bellmanFord(WeightedAutomaton);
  public static double[] backwardDistances(WeightedAutomaton);
  public static double[] distancesTo(WeightedAutomaton, int);
  public static WeightedPaths.Analysis analyze(WeightedAutomaton);
  public static double costThroughState(WeightedAutomaton, int);
  public static double costThroughTransition(WeightedAutomaton, int);
  public static double fixedLengthCost(WeightedAutomaton, int, boolean);
}
```

<a id="weightedtransformations"></a>

## `WeightedTransformations`

[Source and Javadoc](../src/main/java/fr/menana/automaton/WeightedTransformations.java)

```java
public final class WeightedTransformations {
  public static final int DEFAULT_MAX_STATES;
  public static WeightedAutomaton removeEpsilon(WeightedAutomaton);
  public static WeightedAutomaton determinizeDag(WeightedAutomaton);
  public static WeightedAutomaton determinizeDag(WeightedAutomaton, int);
  public static WeightedAutomaton minimizeDag(WeightedAutomaton);
  public static WeightedAutomaton determinize(WeightedAutomaton, int);
  public static WeightedAutomaton determinize(WeightedAutomaton);
  public static WeightedAutomaton minimize(WeightedAutomaton);
}
```

<a id="weightedword"></a>

## `WeightedWord`

[Source and Javadoc](../src/main/java/fr/menana/automaton/WeightedWord.java)

```java
public final class WeightedWord {
  public int[] word();
  public int length();
  public double cost();
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

[Documentation index](README.md)
