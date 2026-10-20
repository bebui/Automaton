# Compiled recognition and factor search

<!-- example: compilation -->
```java
Automaton a = Automaton.dfaFromString("12+");
CompiledDfa compiled = a.compile();
int[] text = {3, 1, 2, 2, 4};
check(compiled.run(text, 1, 4));
int state = compiled.step(compiled.getInitialState(), 1);
state = compiled.step(state, 2);
check(compiled.isAccept(state));
Searcher search = a.searcher();
Searcher.Match match = search.find(text);
check(match.start() == 1 && match.end() == 4 && match.length() == 3);
check(search.findAll(1, 2, 0, 1, 2, 2).size() == 2);
```

## Boolean compiled snapshots

`a.compile()` / `CompiledDfa.of(a)` build an immutable array-based deterministic
snapshot. An NFA is determinized during compilation. Editing the source later
does not change the snapshot. Creation can be exponential for an NFA; reusing
one compiled instance avoids repeating it.

`run(int...)` recognizes the entire input, while `run(text,from,to)` recognizes
the half-open slice `[from,to)`. Supply valid slice bounds; the Boolean legacy
API should not be treated as a uniform checked-slice validator. `step` advances
from a current state. Missing transitions give `CompiledDfa.DEAD` (-1), which
remains dead under subsequent steps and is never accepting. `getInitialState`
can itself be dead for an empty automaton.

Transition lookup is binary search over sorted interval segments. For a word
of length L and at most I outgoing segments, recognition costs O(L log I).
Existing arrays/slices avoid varargs-array creation at the call site. Actual
speed depends on interval degree and symbol distribution; binary lookup is not
universally faster than a very short linear scan. Share compiled instances
between threads; use independent caller-owned mutable input buffers.

## Searching for factors

`Searcher` recognizes factors inside a larger sequence. `find` returns the
match with the smallest start position and, among those, the largest end.
`Match.start()` is inclusive, `end()` exclusive, and `length()` is their difference.
No match gives null. `contains` answers existence. `find(text,from)` restricts
where a match may start; use `0 <= from <= text.length`.

`findAll` returns successive **nonoverlapping** leftmost-longest matches. It
resumes at the previous end; after an empty match it moves one symbol forward
so it cannot loop forever. If the language accepts epsilon, empty matches can
occur, including at the end of the text. The coordinates refer to the original
array and count integer symbols.

A `Searcher` prepares forward/reverse compiled structures and may incur expensive
language operations. Build one and reuse it for many texts. Instances are
immutable and thread-safe. Convenience methods `a.find/contains/findAll` prepare
a searcher per call. The scanning algorithm does backward/forward passes and
interval lookups; account for both text length and lookup cost, rather than
assuming preparation or interval searches take no time.

## Weighted compiled evaluators

`CompiledWeightedDfa` requires an already deterministic epsilon-free weighted
graph. Its `transition` returns a logical **edge ID**, unlike Boolean `step`,
which returns a **state ID**. Get the target from `a.graph().target(edge)`.
Its scalar `cost(text,from,to)` includes initial/final weights and returns
`+Infinity` for rejection. It validates slices. `reweight` requires the identical
graph and shares the interval index.

`CompiledMultiWeightedDfa` also requires a DFA without epsilon. It returns a
vector for the unique accepting path, or fills a caller buffer without allocation.
`project`, `scalarize` and `reweight` reuse its index. See the
[multi-weighted guide](MULTI_WEIGHTED_AUTOMATA.md). No compiled API silently
collapses an NFA's costs or vector relation through Boolean determinization.

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
