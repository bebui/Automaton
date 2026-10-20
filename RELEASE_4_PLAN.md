# Automaton 4.0 preparation and package refactoring

Base: merged PR #8 (`d9c9225`). The user explicitly requested a major version and
less congestion in the main Java package. Version 4.0.0 is prepared locally and
submitted through a PR; this work does not publish Maven artifacts or create a release tag.

## Scope and invariants

- Preserve all public 3.0 Boolean class names/packages and method signatures.
- Move the unreleased weighted API into `fr.menana.automaton.weighted`.
- Place scalar/vector compiled evaluators in `weighted.compiled`, and shared numeric
  implementation helpers in `weighted.internal` (unsupported internal API).
- Keep tightly coupled graph algorithms together so primitive CSR arrays remain
  package-private. Do not expose writable arrays merely to create more directories.
- Extract an immutable `WeightedDfaIndex`, independent of weights, from the scalar
  compiler. Scalar and vector evaluators can share it directly, including explicit
  factories with an existing index. Reweighting retains no previous cost store.
- Reuse the existing interval partition algorithm by making `Alphabet` a public,
  read-only facade with its existing private constructor. Add allocation-free indexed
  interval reads and a defensive sorted-range factory to `IntervalSet` for cross-package
  graph construction/copying. Existing Boolean algorithm bodies remain unchanged.
- Update tests, benchmark imports, executable examples, Javadoc, the English manual,
  migration notes, release notes and API-reference generation. Historical audit logs
  remain records of their original snapshots.

## Verification

Run the complete Java 17 build, independent interval/index sharing tests and all
executable documentation examples. Compare the historical Boolean API and unchanged
class files with the baseline; explicitly identify the additive Alphabet/IntervalSet
changes. Check final production and benchmark bytecode match. Use JMH with baseline
and candidate variants of the same harness to assess scalar/vector recognition,
compilation, reweighting, interval-heavy operations and Boolean non-regression.
Record memory/allocations for shared indexes and repeated reweighting. No changes
to solver integrations, Pareto semantics or path algorithm families are intended.
