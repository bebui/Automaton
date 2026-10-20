package audit;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.*;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** New API setup/reuse and Boolean membership, separate from old/new comparisons. */
@org.openjdk.jmh.annotations.State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations=3,time=500,timeUnit=TimeUnit.MILLISECONDS)
@Measurement(iterations=5,time=500,timeUnit=TimeUnit.MILLISECONDS)
@Fork(value=2,jvmArgsAppend={"-Xms128m","-Xmx256m"})
public class SnapshotApiBench {
    @Param({"dfa","epsilon-nfa"}) public String family;
    Automaton automaton;
    AutomatonSnapshot snapshot;
    WeightedAutomaton weighted;
    int[] word;
    @Setup public void setup() {
        automaton=GraphBridgeBench.fixture(256,4);
        if(family.equals("epsilon-nfa"))automaton.addEpsilonTransition(automaton.getInitial(),automaton.getState(1));
        snapshot=automaton.snapshot();weighted=snapshot.weighted(t->2);
        word=new int[64];
        if(automaton.run(word)!=snapshot.run(word))throw new AssertionError();
    }
    @Benchmark public Object capture(){return automaton.snapshot();}
    @Benchmark public Object scalarFromMutable(){return automaton.weighted(t->2);}
    @Benchmark public Object scalarFromSnapshot(){return snapshot.weighted(t->2);}
    @Benchmark public Object vectorFromSnapshot(){return snapshot.multiWeighted(t->2,t->7);}
    @Benchmark public boolean membership(){return weighted.run(word);}
    @Benchmark public Object booleanProjection(){return weighted.toAutomaton();}
}
