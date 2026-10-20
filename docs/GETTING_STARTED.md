# Getting started

## Requirements and availability

Use a JDK 17 or later and Maven. The library has no production dependency;
JUnit is a test dependency. JMH belongs to the separate optional benchmark
module. Graphviz is optional and only needed to render DOT exports.

The root POM coordinates are:

```xml
<dependency>
    <groupId>fr.menana</groupId>
    <artifactId>automaton</artifactId>
    <version>3.0.0</version>
</dependency>
```

For Gradle: `implementation("fr.menana:automaton:3.0.0")`. This identifies the
3.0.0 artifact; it does not fetch unreleased weighted APIs from the Git checkout.
For the extension, build and install this checkout locally, or use its built JAR.
A local install under the unchanged version replaces that version in your local
Maven repository. A separate published release/version is not created by these
commands. Avoid confusing a local build with the Maven Central artifact.

```bash
java -version
mvn -version
mvn clean verify
mvn install
```

Both version commands should use the intended JDK. On systems with multiple
JDKs, set `JAVA_HOME` to the JDK 17 directory and prepend `$JAVA_HOME/bin` to
`PATH`. A JRE alone cannot compile examples or generate Javadoc.

## First automaton

<!-- example: construction -->
```java
Automaton a = new Automaton();
State start = a.addState();
State digits = a.addState();
a.setInitial(start);
a.setAccept(digits);
a.addTransition(start, digits, new Interval(0, 9));
a.addTransition(digits, digits, new Interval(0, 9));
check(a.run(4, 2));
check(!a.run());
Automaton copy = a.clone();
copy.setAccept(copy.getInitial());
check(copy.run() && !a.run());
```

States come from the same automaton's `addState`. The language above contains
all nonempty sequences of digits. Calling `run()` with no argument tests the
empty word; it is not a query about whether the automaton has any accepted word.
Use `isEmpty()` for that question. The clone can be edited independently.

To create the same pattern with a compact expression, use
`Automaton.dfaFromString("(0|1|2|3|4|5|6|7|8|9)+")`. To represent a wide range,
prefer an interval transition over enumerating symbols in an expression.

## Build products and API documentation

The main `target` directory contains the binary, source and Javadoc JARs after
`verify`. These include the extension when built from this checkout. The main
JAR does not include benchmark classes or JMH dependencies.

```bash
mvn javadoc:javadoc
```

This explicit goal writes browsable documentation to
`target/site/apidocs/index.html`. The attached-Javadoc build used by `verify`
uses `target/apidocs/index.html`. The online javadoc.io link in the root
README documents published artifacts and may lag behind this checkout.

## Run all documentation examples

After building the library:

```bash
python3 docs/check_examples.py
```

The checker uses `JAVA_HOME/bin/java` and `JAVA_HOME/bin/javac` when configured,
otherwise the tools on `PATH`. It checks snippet/source consistency before
compiling into `target/documentation-examples` and running the tour. Python 3
is documentation tooling, not a library runtime dependency.

To run the Java tour without Python on a Unix-like shell:

```bash
mkdir -p target/documentation-examples
javac --release 17 -cp target/classes -d target/documentation-examples docs/examples/AutomatonTour.java
java -cp target/classes:target/documentation-examples AutomatonTour
```

On Windows, use `;` as the Java classpath separator. With a built JAR, replace
`target/classes` with the path to that JAR.

[Documentation index](README.md) · [API reference](API_REFERENCE.md)
