# Connecting Automaton and its weighted API

Baseline: published 4.0.0 / merged PR #10 (`13d8ba6`). The new additive API is
prepared as `4.1.0-SNAPSHOT`; this work does not publish or replace 4.0.0.

## Scope

Make the existing mutable `Automaton` the normal entry point for Boolean, scalar
and vector work. Preserve its public types, mutable-state behavior and existing
recognizer/compiler. Keep immutable graph identity and parallel logical paths
required by weighted algorithms. No inheritance between a mutable Boolean class
and immutable cost views: inherited Boolean operations would otherwise silently
forget weighted semantics and inherited mutations would invalidate shared IDs.

## Implementation

- Add `Automaton.snapshot()`, `weighted(callback)` and `multiWeighted(callbacks)`.
- Add a reusable root-package `AutomatonSnapshot`: capture topology once, retain
  original state/transition identity mappings, then create scalar/vector weightings
  sharing the exact graph. State mappings survive source reindexing. Boundary
  callbacks and mode-aware epsilon callbacks cover costs beyond simple edges.
- Route the existing `WeightedAutomaton.from(Automaton, callback)` through this
  shared snapshot path. Add the corresponding vector factory.
- Add `run` and `toAutomaton` to graph/snapshot/scalar/vector objects. Boolean
  recognition never inspects numeric costs; a negative epsilon cycle or numeric
  overflow cannot change language membership. Boolean projection is a fresh mutable
  copy, explicitly forgetting costs and merging parallel transitions as the
  existing representation requires. No Boolean quotient is used for cost operations.
- Share the primitive reachability kernel between legacy coaccessibility and the
  immutable graph's reachability, useful-state analysis and epsilon closure.
  Adapt the legacy object graph through reverse CSR without copying labels or
  creating a complete immutable snapshot for an existing Boolean query.
- Keep `getUseFulStates()` coaccessible-only, including unreachable states. Keep
  `AutomatonGraph.usefulStates()` reachable AND coaccessible. Expose explicit
  `coaccessibleStates()` to express their shared criterion accurately.

## Ownership and costs

Original objects in snapshot metadata remain mutable identity references; they
are not frozen copies. The immutable graph and captured weights do not follow
later source edits. Callbacks must not race source edits. Separate convenience
calls make separate snapshots; explicitly reuse one snapshot when graph identity
matters. No automatic cache is attached to Automaton: its exposed mutable state
collections cannot reliably invalidate such a cache. Existing Automaton fields
and Boolean recognition hot paths remain unchanged.

Snapshot metadata retains original objects; completed weightings do not retain
that metadata. The legacy adapter keeps its historical treatment of empty-label
records, while imports retain only consuming/epsilon logical modes.

Snapshot/import construction remains O(V+E+I); scalar cost capture O(E+V), d
vector dimensions O(dE+dV). Reachability is O(V+E), word simulation uses primitive
state buffers and weighted recognition preserves its previous numeric contracts.
Boolean projection is O(V+E+I), plus existing destination-map label merging.
Public weighted package names and 4.0 signatures remain compatible.

## Verification

Use independent epsilon/transitive-closure matrix oracles for generated cyclic
NFAs, parallel arcs, missing initials and unreachable finals. Check direct regex
construction, source identity mappings after reindexing/mutation, boundary costs,
mode-aware epsilon weights, correlated vector paths, defensive Boolean projection,
empty inputs, nonfinite costs and overflow-independent Boolean membership.
Run every existing test and the executable English examples. Check published 4.0
public signatures and an unrecompiled Boolean client against the new classes.
Use the same JMH fixtures against both snapshots for legacy coaccessibility,
immutable useful states, scalar imports, Boolean recognition and minimization;
measure reusable-snapshot APIs separately. Preserve raw errors/allocation samples
and distinguish shared-host noise from a repeatable regression.
