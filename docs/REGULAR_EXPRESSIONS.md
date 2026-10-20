# Regular expressions over integers

`new Automaton(expression)` and `Automaton.dfaFromString(expression)` build a
minimal DFA. `Automaton.nfaFromString` builds the expression automaton without
requesting global determinization/minimization. Intersection and complement
inside the expression can themselves require language operations.

<!-- example: regex -->
```java
Automaton sequence = Automaton.dfaFromString("12");
Automaton symbol = Automaton.dfaFromString("<12>");
check(sequence.run(1, 2) && !sequence.run(12));
check(symbol.run(12) && !symbol.run(1, 2));
RegExp expression = RegExpParser.toRegExp("<10><-3>.*");
Automaton nfa = expression.toNFA();
check(nfa.run(10, -3, Integer.MAX_VALUE));
check(Automaton.dfaFromString("").run());
check(Automaton.dfaFromString("(1|2){2,3}").run(1, 2));
```

## Syntax

| Expression | Meaning |
|---|---|
| `0` … `9` | One symbol equal to the digit |
| `<42>`, `<-3>`, `<2147483647>` | One signed integer symbol |
| `.` | Any one signed integer |
| empty expression, `()` | Empty word |
| `ab` | Concatenation |
| `a\|b` | Union |
| `a&b` | Intersection |
| `~a` | Complement over all finite words on the full int alphabet |
| `a*`, `a+`, `a?` | Zero or more, one or more, zero or one repetitions |
| `a{n}`, `a{n,m}` | Exact or inclusive bounded repetition |
| `a{n,}`, `a{,m}` | At least n or at most m repetitions |
| `(a)` | Grouping |

Precedence, strongest first: repetition, complement, concatenation,
intersection, union. Thus `~12` means `(~1)2`; use `~(12)` to complement the
whole word `1,2`. Parentheses make mixed operators easier to review.

This is not Java's character-regex syntax. `12` consumes two symbols;
`<12>` consumes one. Whitespace is not ignored. `[0-9]`, character classes,
backreferences, anchors and backslash escapes are not provided. Construct
large numeric ranges directly with interval transitions. Angle-bracket numbers
accept an optional minus sign and decimal digits within the signed int range;
`<+3>` is invalid. The complement includes words containing integers absent
from the expression. Intersect with an explicit alphabet language to restrict it.

## Parser API

`RegExpParser.toRegExp` returns a public `RegExp` tree facade. Its `toNFA()`
builds an automaton and `toString()` prints an equivalent expression.
`RegExpParser.toNFA/toDFA` are convenience factories. Concrete nodes such as
`RegExpInt` are package-private in 3.0 and should not be constructed by clients.

Malformed expressions throw `IllegalArgumentException`, including the error
position and original expression. For example `(1`, `<2147483648>`, `<x>` and
an unmatched repetition brace fail explicitly. Repetitions with large bounds
can build large automata; Boolean operators can trigger exponential
constructions. The simple Thompson construction is linear in the expanded
expression fragment, not in a numeric repetition bound's character count.

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
