# Language queries, counting and generation

<!-- example: languages -->
```java
Automaton a = Automaton.dfaFromString("(1|2)*1");
Automaton b = Automaton.dfaFromString("(1|2)*1(1|2)*");
check(a.isSubsetOf(b) && !a.sameLanguage(b));
check(!a.isFinite());
check(a.countWords(3).equals(BigInteger.valueOf(4)));
int[] witness = a.counterExample(b);
check(a.run(witness) != b.run(witness));
int[] sampled = a.randomWord(3, new Random(42));
check(sampled.length == 3 && a.run(sampled));
int count = 0;
for (int[] word : a.words(2)) { check(a.run(word)); count++; }
check(count == 3);
Automaton joined = Automaton.dfaFromString("12").concatenate(Automaton.dfaFromString("34"));
check(joined.run(1, 2, 3, 4));
check(Automaton.dfaFromString("12").revert().run(2, 1));
```

## Predicates and witnesses

| API | Result |
|---|---|
| `isEmpty()` | No accepted word exists, including the empty word |
| `isFinite()` | Only finitely many words are accepted |
| `isUniversal()` | Every finite word on the int alphabet is accepted |
| `sameLanguage(other)` | Equal word sets, independently of graph structure |
| `isSubsetOf(other)` | Every word of this automaton is in the other |
| `shortestWord()` | A shortest accepted `int[]`, or null if empty |
| `counterExample(other)` | A shortest word accepted by exactly one operand, or null if equal |

A useful consuming cycle makes a Boolean language infinite; epsilon cycles
alone do not create longer words. The implementation performs the required
language transformations rather than treating every structural cycle as a
proof of infiniteness. Inclusion uses antichains and need not first build a
full DFA. Other queries may compile/determinize internally.

`equals` is not language equivalence. A shortest witness is not a contract to
choose a particular word among every possible tie unless the API explicitly
specifies the order.

## Exact-length counting and sampling

`countWords(n)` counts **distinct words** of exactly n symbols as a `BigInteger`.
Ambiguous NFA paths do not multiply the count. Determinization can be expensive;
then a dynamic-programming table of size O((n+1)V) counts suffixes on the DFA.
Each interval contributes its cardinality without enumerating its symbols.
Arithmetic cost grows with the bit length of the counts.

`randomWord(n,random)` draws uniformly from these distinct words, using the same
suffix counts and interval multiplicities. It returns null if no such word
exists. A supplied seeded `Random` makes a chosen run reproducible. Repeated
calls recompute their preparation; the API does not expose a prepared sampler.
Use nonnegative lengths and a non-null random generator.

## Bounded-length enumeration

`words(maxLength)` returns an `Iterable<int[]>`, ordered by increasing consumed
length then signed-integer lexicographic order. It includes the empty word if
accepted. It prepares a DFA and feasibility table, then generates words lazily;
it does not eagerly materialize every word. An enormous interval or bound can
still produce an enormous output. Each iteration has its own iterator.

This order is about words and length, not weights. Use `wordsByCost()` for
weighted optimization and `pathsByCost()` for logical-edge walks. A maximum
length parameter is inclusive; `countWords` and `randomWord` use exact length.

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
