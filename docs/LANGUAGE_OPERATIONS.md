# Boolean language operations

These operations act on recognized words and return new automata. Their
operands remain unchanged. They do not preserve weighted path costs or the
identity/multiplicity of accepting paths.

| API | Language of the result |
|---|---|
| `a.union(b)` | Words accepted by at least one operand |
| `a.intersection(b)` | Words accepted by both operands |
| `a.difference(b)` | Words accepted by a and rejected by b |
| `a.concatenate(b)` | Every word `uv` with u in a and v in b |
| `a.complement()` | Every finite integer word outside a |
| `a.revert()` | Reverse of each accepted word |
| `a.determinize()` | Same language, DFA subset construction |
| `a.minimize()` | Minimal DFA for the language |
| `a.removeEpsilon()` | Same language without epsilon transitions |
| `a.reduce()` | NFA reduction by bisimulation; not general minimal NFA construction |

Union and concatenation normally minimize when both inputs are deterministic.
Intersection and difference build reachable products and minimize. The product
implementation uses deterministic compiled snapshots, so NFA operands may cause
determinization internally. Complement determinizes an NFA before complementing.
Reversal can introduce nondeterminism. Repetition is available through expression
syntax; Boolean `Automaton` has no scalar-weighted `star()` method.

`Operation` exposes static forms of these transformations. To avoid the optional
final minimization of a Boolean concatenation, call
`Operation.concatenate(a,b,false)`; this does not suppress all other algorithmic
work and the result may contain epsilon transitions.

## Minimization selection

The default `Operation.MINIMIZATION_ALGO.Hopcroft` name selects the existing
Valmari–Lehtinen partition-refinement implementation. Use
`Operation.minimize(a, Operation.MINIMIZATION_ALGO.Brzozowski)` to select
Brzozowski for one call. `setDefaultMinimization` changes a process-wide setting;
prefer per-call selection in code shared by several applications. Brzozowski
can be exponential. NFA minimization also requires determinization.

## Complement and alphabet restriction

The implicit alphabet is every signed integer, not the set of labels encountered
in a particular automaton. To complement inside `{1,2}*`, construct
`Automaton.dfaFromString("(1|2)*").difference(a)`. `a.complement()` alone also
accepts rejected words containing other integers.

For scalar/vector operations, read [advanced weighted algebra](ADVANCED_ALGORITHMS.md).
Calling Boolean reduction/minimization on an unweighted copy can destroy the
relation between words and costs. Boolean complement and difference have no
implicitly defined weighted counterpart in this library.

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
