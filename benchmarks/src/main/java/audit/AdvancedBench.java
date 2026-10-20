package audit;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.*;
import fr.menana.automaton.weighted.compiled.*;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

/** Small certified transformations and deliberately unsafe-double cancellation fixtures. */
@org.openjdk.jmh.annotations.State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations=3,time=500,timeUnit=TimeUnit.MILLISECONDS)
@Measurement(iterations=5,time=500,timeUnit=TimeUnit.MILLISECONDS)
@Fork(value=2,jvmArgsAppend={"-Xms128m","-Xmx256m"})
public class AdvancedBench {
    WeightedAutomaton nfa, dfa, epsilon, decimal;
    MultiWeightedAutomaton vectors;
    @Setup(Level.Trial) public void setup() {
        nfa=WeightedBench.chain(33); dfa=nfa.determinize();
        epsilon=WeightedAutomaton.from(Automaton.nfaFromString("1(2|3){2}"),t->1);
        WeightedAutomaton.Builder b=WeightedAutomaton.builder(); for(int i=0;i<5;i++)b.addState();
        b.setInitial(0);b.setAccept(4);
        b.addEpsilonTransition(0,1,1e16);b.addEpsilonTransition(1,2,1);
        b.addEpsilonTransition(2,3,-1e16);b.addEpsilonTransition(3,0,-1);b.addTransition(3,4,7,0);
        decimal=b.build();
        MultiWeightedAutomaton.Builder m=MultiWeightedAutomaton.builder(2);for(int i=0;i<4;i++)m.addState();
        m.setInitial(0);m.setAccept(3);m.addTransition(0,1,7,1e16,0);m.addTransition(1,2,7,1,1);
        m.addTransition(2,3,7,-1e16,0);m.addTransition(0,3,7,0,2);vectors=m.build();
        if(decimal.optimalCost()!=1||vectors.paretoFront().size()!=2)throw new AssertionError();
    }
    @Benchmark public Object residualDeterminize(){return nfa.determinize();}
    @Benchmark public Object weightedMinimize(){return dfa.minimize();}
    @Benchmark public Object weightedCompile(){return dfa.compile();}
    @Benchmark public Object eliminateEpsilon(){return epsilon.removeEpsilon();}
    @Benchmark public Object exactBellmanFord(){return WeightedPaths.bellmanFord(decimal);}
    @Benchmark public Object exactPareto(){return vectors.paretoFront();}
}
