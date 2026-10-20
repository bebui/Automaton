# Frequently asked questions

## Why does `12` behave differently from `<12>`?

The former is a two-symbol expression, the latter one integer symbol. Java
`run(12)` consumes one symbol, while `run(1,2)` consumes two. The library does
not implicitly tokenize strings into decimal numbers.

## Is epsilon the empty language?

No. Epsilon is one word of length zero. An automaton whose initial state is
accepting can recognize it. A newly created `Automaton` with no initial state
recognizes no words. `run()` tests epsilon; `isEmpty()` tests the language.

## Why does a complement accept unexpected integers?

Complement is taken over all finite words on the full signed-int alphabet.
Restrict the result with an explicit alphabet language if needed.

## Can Boolean determinization/minimization preserve my costs?

It preserves word sets. Weighted paths can collapse under those algorithms.
Use the scalar weighted transformations and their exactness/budget preconditions.
Vector relations are not implicitly scalar-determinized.

## Is `bestPath()` the same query as `bestPath(new int[0])`?

No. No-argument `bestPath()` is the global accepting optimum. The explicit empty
array requests a path that reads epsilon. `cost()` still evaluates epsilon.

## Do K paths mean K different words?

No. NFA ambiguity, parallel edges and epsilon loops can produce many paths for
the same word. Use `kBestWords`/`wordsByCost` for distinct concrete words at their
minimum accepting cost. Path APIs return a representative symbol for an interval;
word APIs lazily enumerate its concrete symbols.

## Can I combine several scalar automata into one vector automaton?

Use `fromWeightings` for multiple cost stores on the exact same graph and common
path. Use `synchronizedProduct` for independently chosen paths, possibly on different
graphs, reading common words. The [vector guide](MULTI_WEIGHTED_AUTOMATA.md) shows
why their attainable vector sets differ on an ambiguous NFA.

## Why can a scalarization use Bellman–Ford?

Negative coefficients or signed components can make scalar edges negative. The
automatic solver inspects resulting weights. Explicit Dijkstra rejects negative
edges. The shared structure does not imply shared nonnegativity assumptions.

## Can a finite horizon still have an unbounded optimum?

Yes. Epsilon consumes no position, so a useful negative epsilon cycle remains
repeatable within one layer. Consuming negative cycles are bounded by the horizon.

## Why is my Pareto request rejected after layering?

The current exact vector propagation requires a DAG. A useful epsilon cycle
remains cyclic in a layered graph. Scalar cyclic search has broader support;
that does not turn a general vector front into a finite DAG problem.

## Why does determinization exhaust its budget on a simple-looking language?

Exact residual construction may create many or infinitely many residual vectors,
even when another representation is small. Exhaustion is a resource failure, not
a mathematical nondeterminizability test. Distinct-word search explores prefixes
on demand and does not require completing that construction.

## Why can exact normalization reject ordinary doubles?

An exact sum or difference of two representable doubles may not fit in one double.
The transformed automaton stores doubles, so it rejects such emitted weights
instead of silently rounding an exact invariant. Fast numeric evaluation has a
different, explicitly documented IEEE contract.

## Why do Automaton and the weighted types use different representations?

The original Automaton is mutable and merges transitions by destination. The
weighted engine uses immutable logical-edge IDs and preserves parallel paths.
Since 4.1, `Automaton.weighted`, `multiWeighted` and `snapshot` connect these
representations directly; `toAutomaton` projects the language back explicitly.
The shared snapshot captures one graph for multiple cost stores. See
[the integration guide](AUTOMATON_AND_WEIGHTS.md) for the complete workflow.

## How do I publish or install the extension?

Build/install the checkout locally for development. The POM develops `4.1.0-SNAPSHOT`;
the published 4.0.0 artifact does not include subsequent Git changes. Signing
and publishing artifacts is a separate release action. See the
[4.0 migration guide](MIGRATION_4.md) for packages and compatibility.
The library remains LGPL-3.0; see [LICENSE](../LICENSE).

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
