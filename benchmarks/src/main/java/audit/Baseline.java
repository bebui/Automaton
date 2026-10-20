package audit;

import fr.menana.automaton.Automaton;
import fr.menana.automaton.CompiledDfa;
import fr.menana.automaton.Interval;
import org.openjdk.jmh.annotations.*;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 2, jvmArgsAppend = {"-Xms128m", "-Xmx256m"})
public class Baseline {
    @State(Scope.Thread)
    public static class Graph {
        @Param({"sparse-unit", "sparse-interval", "dense-unit", "dense-interval"})
        public String family;
        Automaton a, b;
        CompiledDfa compiled;
        int[] word;
        int states, degree, width;

        @Setup(Level.Trial)
        public void setup() {
            states = family.endsWith("unit") ? 64 : 256;
            degree = family.startsWith("sparse") ? 2 : 16;
            width = family.endsWith("unit") ? 1 : 1000000;
            a = graph(states, degree, width, 42);
            b = graph(16, degree, width, 43);
            compiled = a.compile();
            Random r = new Random(44);
            word = new int[64];
            for (int i = 0; i < word.length; i++)
                word[i] = r.nextInt(degree) * width + r.nextInt(width);
            if (a.run(word) != compiled.run(word))
                throw new AssertionError("Fixture recognition mismatch");
            if (!a.isDeterministic()) throw new AssertionError("Fixture must be DFA");
        }
    }

    @State(Scope.Thread)
    public static class Nfa {
        Automaton a;
        int[] word;
        @Setup(Level.Trial)
        public void setup() {
            a = Automaton.nfaFromString("(1|2)*1(1|2){3}");
            word = new int[64];
            for (int i = 0; i < word.length; i++) word[i] = 1 + (i % 2);
            if (a.isDeterministic()) throw new AssertionError("Fixture must be NFA");
        }
    }

    static Automaton graph(int n, int degree, int width, int seed) {
        Automaton a = new Automaton();
        fr.menana.automaton.State[] s = new fr.menana.automaton.State[n];
        for (int i = 0; i < n; i++) s[i] = a.addState();
        a.setInitial(s[0]);
        Random r = new Random(seed);
        for (int i = 0; i < n; i++) {
            if (i % 7 == 6) a.setAccept(s[i]);
            for (int j = 0; j < degree; j++) {
                int target = j == 0 ? (i + 1) % n : r.nextInt(n);
                a.addTransition(s[i], s[target], new Interval(j * width, (j + 1) * width - 1));
            }
        }
        return a;
    }

    @Benchmark public boolean run(Graph g) { return g.a.run(g.word); }
    @Benchmark public boolean compiledRun(Graph g) { return g.compiled.run(g.word); }
    @Benchmark public Automaton construct(Graph g) { return graph(g.states, g.degree, g.width, 42); }
    @Benchmark public Automaton copy(Graph g) { return g.a.clone(); }
    @Benchmark public CompiledDfa compile(Graph g) { return g.a.compile(); }
    @Benchmark public Automaton minimize(Graph g) { return g.a.minimize(); }
    @Benchmark public Automaton intersection(Graph g) { return g.a.intersection(g.b); }
    @Benchmark public BigInteger countWords(Graph g) { return g.a.countWords(16); }
    @Benchmark public int[] shortestWord(Graph g) { return g.a.shortestWord(); }
    @Benchmark public boolean nfaRun(Nfa n) { return n.a.run(n.word); }
    @Benchmark public Automaton determinize(Nfa n) { return n.a.determinize(); }
    @Benchmark public Automaton removeEpsilon(Nfa n) { return n.a.removeEpsilon(); }
}
