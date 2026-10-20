# Automata and integer alphabets

## Words, states and transitions

A word is an `int[]`. Its length is the number of consumed integers, not the
number of decimal characters used to print them. The empty word is `new int[0]`.
An epsilon transition changes state without consuming a symbol. A DFA has a
single possible transition for each state/symbol and no epsilon transition;
an NFA can have several accepting paths for a word.

`Automaton` starts without states or initial state and recognizes nothing.
`addState` creates states; `setInitial` replaces the previous initial state;
`setAccept(s)` and `setAccept(s,false)` change acceptance. Making the initial
state accepting recognizes the empty word, even if there are no transitions.
`getInitial()` returns null if absent; `getState(index)` returns null outside
the current state range. Always use states created by the target automaton.

`addTransition` accepts an `Interval`, an `IntervalSet`, or `int...` values.
`addEpsilonTransition` adds epsilon. Boolean transitions are keyed by destination:
adding several arcs between the same states merges their labels and epsilon
flag. Such a transition can have both symbol and epsilon modes. It cannot
represent distinct costs or distinct parallel edge identities. Use the weighted
multigraph when those identities matter.

The stored determinism flag is conservative after some repeated additions.
A false flag need not imply that two distinct destinations are possible for a
symbol. `determinize()` produces a deterministic equivalent. External mutation
of exposed collections can invalidate invariants, including that flag.

## Intervals and interval sets

An `Interval(min,max)` includes both bounds, which must satisfy `min <= max`.
It is read through `getMin/getMax`, `contains`, `intersects`, `intersection` and
`complement`. A disjoint interval intersection returns null. `Interval.size()`
returns an `int`, so large cardinalities can overflow that legacy API. Use a
`long` difference or `IntervalSet.size()` for the full integer domain.

<!-- example: intervals -->
```java
IntervalSet symbols = IntervalSet.fromInterval(new Interval(-5, 5));
symbols.add(20, 30);
check(symbols.contains(-3));
check(symbols.size() == 13L);
IntervalSet withoutZero = symbols.minus(IntervalSet.fromIntArray(0));
check(!withoutZero.contains(0) && symbols.contains(0));
check(IntervalSet.ALL.size() == 4_294_967_296L);
Interval wide = new Interval(Integer.MIN_VALUE, Integer.MAX_VALUE);
long cardinality = (long) wide.getMax() - wide.getMin() + 1;
check(cardinality == 4_294_967_296L);
```

`IntervalSet` stores canonical sorted, nonoverlapping, nonadjacent intervals.
`add` and `remove` mutate the receiver. `union`, `intersection`, `minus` and
`complement` return sets; the operand sets remain usable independently.
`contains` uses binary search. `getMin/getMax` throw `NoSuchElementException`
on an empty set. `getIntervals()` returns interval objects in sorted order,
not the internal primitive storage. `ALL` and `EMPTY` reject mutation; clone
one to obtain a mutable copy. The alphabet is the entire signed `int` range,
with cardinality 2^32, unless your own construction restricts it.

## Inspection and mutation boundaries

`getStates()` exposes the live state list and `State.getTransitions()` the live
map. Treat them as read-only while inspecting an automaton; edit through the
construction API. `Transition.getValues()` returns a defensive copy, so editing
that set does not alter the transition. Inspect epsilon through `hasEpsilon`
and symbols through `hasValues/accepts`. `getAllTransitions`, acceptance lists
and interval copies are derived objects, not stable edge-ID tables.

`delta(state,symbol)` and `deltaEpsilon(state)` return immediate destinations;
they are not complete reflexive/transitive epsilon closures. `run` performs the
needed closure during recognition. `getUseFulStates()` (existing spelling)
returns coaccessible states, including possibly unreachable ones. In contrast,
`AutomatonGraph.usefulStates()` requires both reachability and coaccessibility.

`reIndex()` traverses from the initial state, drops unreachable states, and can
change every state index. `removeDeadStates()` prunes destinations without an
accepting continuation and reindexes; it can retain an initial dead state.
Do not store long-lived weighted data indexed by mutable `State.getIndex()`.

`clone()` copies states, labels, acceptance and transitions. Boolean operations
return independent automata. `equals` checks structure; `sameLanguage` checks
recognized words. Mutable automata/interval sets are not safe for concurrent
edits. The weighted snapshot types have separate immutable contracts.

## Export

`toDot()` returns a Graphviz DOT string. `toDotty("automaton.dot")` writes a
file using the existing convenience API. You can also write `toDot()` with
Java's `Files.writeString` when you want explicit I/O exception handling.
Run `dot -Tsvg automaton.dot -o automaton.svg` with Graphviz to render it.

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
