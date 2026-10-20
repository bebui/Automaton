package audit;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.*;
import fr.menana.automaton.weighted.compiled.*;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

/** Signed cyclic graphs, ambiguity, epsilon closure and symbolic word enumeration. */
@org.openjdk.jmh.annotations.State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations=3,time=500,timeUnit=TimeUnit.MILLISECONDS)
@Measurement(iterations=5,time=500,timeUnit=TimeUnit.MILLISECONDS)
@Fork(value=2,jvmArgsAppend={"-Xms128m","-Xmx256m"})
public class CyclicAdvancedBench {
    @Param({"32", "128"}) public int states;
    WeightedAutomaton dfa, nfa, fullRange, signedHorizon;
    @Setup(Level.Trial) public void setup() {
        WeightedAutomaton.Builder d = WeightedAutomaton.builder(), n = WeightedAutomaton.builder();
        for (int s = 0; s < states; s++) { d.addState(); n.addState(); }
        d.setInitial(0); n.setInitial(0);
        for (int s = 0; s < states; s++) {
            double ps = (s % 8) / 4.0; d.setAccept(s, ps); n.setAccept(s, ps);
            n.addEpsilonTransition(s, s, 0);
            for (int c = 0; c < 2; c++) {
                int t = (s + (c == 0 ? 1 : 7)) % states; double cost = 1 + ps - (t % 8) / 4.0;
                d.addTransition(s, t, c, cost); n.addTransition(s, t, c, cost);
                if (c == 0) n.addTransition(s, t, c, cost + 0.25);
            }
        }
        dfa = d.build(); nfa = n.build();
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); b.addState(); b.addState();
        b.setInitial(0); b.setAccept(1); b.addTransition(0, 1, new Interval(Integer.MIN_VALUE, Integer.MAX_VALUE), 0.5);
        fullRange = b.build();
        b = WeightedAutomaton.builder(); b.addState(); b.setInitial(0); b.setAccept(0);
        b.addTransition(0, 0, new Interval(0, 1), -0.25); b.addEpsilonTransition(0, 0, 0); signedHorizon = b.build();
        if (dfa.minimize().graph().getNbStates() != 1 || nfa.determinize(512).cost(0,1) != 2
                || fullRange.wordsByCost().next().word()[0] != Integer.MIN_VALUE) throw new AssertionError();
    }
    @Benchmark public Object kCyclic16() { return dfa.kBestPaths(16); }
    @Benchmark public Object firstLazyPath() { return dfa.pathsByCost().next(); }
    @Benchmark public Object firstLazyWord() { return dfa.wordsByCost().next(); }
    @Benchmark public Object distinctWords16() { return nfa.kBestWords(16); }
    @Benchmark public Object residualCyclic() { return nfa.determinize(512); }
    @Benchmark public Object minimizeCyclic() { return dfa.minimize(); }
    @Benchmark public Object signedHorizonWords() { return KBestWords.ofLength(signedHorizon, 8, 16); }
    @Benchmark public Object firstFullRangeWord() { return fullRange.wordsByCost().next(); }
}
