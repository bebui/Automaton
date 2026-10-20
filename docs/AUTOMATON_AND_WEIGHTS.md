# Automaton and its weighted variants

This integration API is **new in 4.1**, currently `4.1.0-SNAPSHOT`. Build this
checkout locally. The published 4.0.0 weighted factories/builders remain valid.

## Start from the same Automaton

Build an `Automaton` using the existing state API or integer regular expressions,
then attach scalar or vector costs. You do not need to rebuild its states in a
separate weighted builder.

<!-- example: integration -->
```java
Automaton source = Automaton.dfaFromString("12+");
WeightedAutomaton direct = source.weighted(transition -> 2);
equal(6, direct.cost(1, 2, 2));
AutomatonSnapshot snapshot = source.snapshot();
WeightedAutomaton cost = snapshot.weighted(transition -> 2);
WeightedAutomaton duration = snapshot.weighted(transition -> 5);
MultiWeightedAutomaton multi = snapshot.multiWeighted(transition -> 2, transition -> 5);
check(cost.graph() == duration.graph() && cost.graph() == multi.graph());
check(cost.run(1, 2) && multi.run(1, 2));
equal(21, multi.scalarize(1, 1).cost(1, 2, 2));
Automaton language = cost.toAutomaton();
check(source.sameLanguage(language));
source.setAccept(source.getInitial());
check(source.run() && !snapshot.run());
```

`source.weighted(transition -> value)` is the direct scalar entry point.
`source.multiWeighted(callback1, callback2, ...)` attaches one dimension per
callback. The callbacks receive the original `Transition` objects, so existing
cost tables keyed by transition identity remain usable. Scalar and vector
boundaries default to zero. All supplied costs must be finite.

## Capture once when several costs share a structure

A convenience call on a mutable `Automaton` creates a fresh snapshot. Two calls
to `source.weighted(...)` therefore do **not** share graph identity. Use one
`AutomatonSnapshot` when several weightings, scalarizations, compiled indexes or
horizon structures must refer to the same logical edge IDs.

The snapshot holds an immutable `AutomatonGraph` plus mappings to original objects.
`stateId(originalState)` keeps its captured value after the source is reindexed;
`originalState(id)` and `originalTransition(edgeId)` expose the corresponding
identity metadata. `graph()` is the same object for every weighting made from
that snapshot. `WeightedAutomaton.from(source, callback)` uses this same bridge;
`MultiWeightedAutomaton.from(source, callbacks...)` is its vector counterpart.

Callbacks are evaluated at cost capture, once per logical edge and dimension.
The snapshot's topology and resulting weight arrays do not follow later edits.
Keeping the snapshot retains the original object graph as identity metadata;
completed weightings retain only their immutable graph and captured costs.
Original metadata objects are still mutable references, not frozen copies:
callbacks must not race mutations, and a callback inspecting later-edited source
fields sees those edits. Prefer identity-keyed cost tables when retaining a
snapshot while editing the source. No automatic mutable-Automaton snapshot cache
is used, because callers can edit the exposed state/transition collections.

## Initial, final and epsilon costs

For nonzero boundaries:

```java
WeightedAutomaton priced = snapshot.weighted(
    transition -> transitionCosts.get(transition),
    1.5,
    state -> terminalCosts.get(state));
```

`transitionCosts` and `terminalCosts` above are caller-owned identity-keyed cost
maps. The final callback runs only for states accepting in the captured topology.
The initial and final values are copied into the weighting; later map edits do
not change them. One scalar weighting per dimension, combined with
`MultiWeightedAutomaton.fromWeightings(...)`, handles distinct vector boundaries
while retaining this same graph.

A Boolean `Transition` can have both consuming labels and an epsilon mode.
The graph splits it into two logical edges with the same original identity.
The ordinary one-argument callback runs separately for both modes. To assign
different costs, use the snapshot's two-argument callback:

```java
WeightedAutomaton priced = snapshot.weighted(
    (transition, epsilon) -> epsilon ? 0.0 : transitionCosts.get(transition));
```

Both callback forms also have a boundary overload. Direct construction with
`WeightedAutomaton.builder()` remains available for parallel logical edges that
the mutable Boolean representation would merge.

## Membership and explicit Boolean projection

`run(word)` on an `AutomatonSnapshot`, `AutomatonGraph`, `WeightedAutomaton` or
`MultiWeightedAutomaton` always answers the Boolean membership question: does
an accepting path read the word? It does not add costs, scalarize vectors, or
construct a best path. Negative epsilon cycles and finite arithmetic overflow
in a weighting cannot change that answer. For example, a word can have Boolean
membership true and scalar cost `-Infinity`, with no finite best path.

`toAutomaton()` makes a fresh mutable Boolean automaton. It preserves the language
and explicitly forgets costs, vector relations and distinctions between parallel
logical paths. Editing it cannot modify the immutable source graph. Converting
back to a weighting does not recover the discarded costs. This projection gives
access to all existing Boolean operations, regular-expression workflows,
compilation, searches, counting and language comparisons when those semantics
are intended. Weighted algebra continues to preserve its own cost/vector contracts.

## Shared algorithms and representation boundaries

The original `Automaton` remains mutable, using `State` and `Transition` objects.
The shared immutable graph stores primitive IDs and interval arrays, preserving
parallel paths. Snapshotting connects these representations without changing the
existing mutable API or retaining writable arrays from a builder.

Legacy coaccessibility, immutable useful-state analysis and Boolean epsilon
closure now share one primitive reachability kernel. `getUseFulStates()` keeps
its historical **coaccessible-only** criterion; `graph.coaccessibleStates()` uses
the same coaccessibility criterion on logical edges. A legacy transition record
with neither symbols nor epsilon is followed by the historical map-based query,
but contributes no logical edge to an imported snapshot. Its language is empty
and is preserved by that import. `graph.usefulStates()` additionally requires reachability
from the captured initial state. Their names do not make those criteria identical.

Snapshot construction is O(V+E+I), scalar cost capture O(E+V), and d vector
components O(dE+dV). The graph is stored once for reused snapshots. Boolean
projection copies labels/states and may merge arcs through the existing API.
The existing mutable recognizer and `CompiledDfa` are retained.

[Documentation index](README.md) · [Weighted costs](WEIGHTED_AUTOMATA.md) · [API reference](API_REFERENCE.md)
