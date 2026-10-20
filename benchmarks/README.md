# JMH and retained-memory measurements

This is an optional standalone build. It compiles the local library sources
into a benchmark executable, so it cannot accidentally resolve the older 3.0
library from Maven Central. It does not install an artifact, alter the main POM
or add a runtime dependency to the library.

Use Java 17 and Maven:

```bash
mvn -f benchmarks/pom.xml clean package
java -jar benchmarks/target/benchmarks.jar 'audit.WeightedBench.*' \
  -prof gc -rf json -rff weighted-results.json -foe true
java -jar benchmarks/target/benchmarks.jar 'audit.Baseline.*' \
  -prof gc -rf json -rff unweighted-results.json -foe true
```

Default annotations use two forks, one thread, three 500 ms warmups and five
500 ms measurements per fork. These are preliminary local settings. For
decisions, use at least three forks, longer warmup/measurement, an idle machine,
the same JVM/heap and alternating baseline/candidate runs. Respect the JMH lock;
do not bypass it to run competing measurements. Keep JSON raw samples and errors.

`Baseline` preserves the initial Boolean 3.0 fixtures. `WeightedBench` measures
scalar DFA/NFA recognition (including reusable epsilon closure buffers),
construction, forward/backward DAG distances, Dijkstra, Bellman–Ford,
reconstruction, union/concatenation/product, unfolding/reuse/conditional costs,
views and materialized scalarization at dimensions 2/8/32, lexicography, Pareto
with strong dominance/tradeoffs and K best paths.
`AdvancedBench` covers certified residual/minimization transforms, epsilon
elimination and exact numerical fallbacks. `ScaleBench` varies 128/8192 states,
degree 2/16 and interval width 1/1000000. Its DFA recognition word visits all
outgoing symbol classes, while DAG distance measurements use an acyclic graph.

For the separate object-graph memory measurement:

```bash
java -javaagent:benchmarks/target/benchmarks.jar \
  -cp benchmarks/target/benchmarks.jar audit.RetainedMemory
```

The instrumentation agent obtains JVM shallow object sizes and the walker
visits reachable reference fields/arrays by identity, counting shared objects
once. It compares one eight-dimensional weighting with 16 scalar views or 16
materialized scalarizations, and checks memory after 100 reweightings. Counts
include graph, arrays, view metadata and boundary costs. They exclude VM class
metadata, native memory, collector overhead and unreachable temporary objects.
They are reachable object sizes, not a heap-dump dominator retained-size analysis.
`gc.alloc.rate.norm` from JMH instead measures bytes allocated per operation.

Raw audit runs and conclusions are under `audit/weighted-extension/`. The
executed audit harness was compiled separately against the same library
classes; its source matches the benchmark sources here. Boolean bytecode and
API compatibility checks accompany the measurements.

Build-only tooling: [JMH](https://github.com/openjdk/jmh),
[Build Helper](https://www.mojohaus.org/build-helper-maven-plugin/plugin-info.html)
and [Maven Shade](https://maven.apache.org/plugins/maven-shade-plugin/plugin-info.html).
