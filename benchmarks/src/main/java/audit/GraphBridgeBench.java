package audit;

import fr.menana.automaton.*;
import fr.menana.automaton.State;
import fr.menana.automaton.weighted.*;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** Common pre/post fixtures; uses only the published 4.0 API. */
@org.openjdk.jmh.annotations.State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations=3,time=500,timeUnit=TimeUnit.MILLISECONDS)
@Measurement(iterations=5,time=500,timeUnit=TimeUnit.MILLISECONDS)
@Fork(value=2,jvmArgsAppend={"-Xms128m","-Xmx256m"})
public class GraphBridgeBench {
    @Param({"64","4096"}) public int states;
    @Param({"2","16"}) public int degree;
    Automaton automaton;
    AutomatonGraph graph;
    @Setup public void setup() {
        automaton=fixture(states,degree);graph=AutomatonGraph.from(automaton);
    }
    static Automaton fixture(int n,int degree) {
        Automaton a=new Automaton();State[] nodes=new State[n];
        for(int s=0;s<n;s++){nodes[s]=a.addState();if(s%7==6)a.setAccept(nodes[s]);}
        a.setInitial(nodes[0]);int half=n/2;
        for(int s=0;s<n;s++)for(int c=0;c<degree;c++) {
            int base=s<half?0:half;
            a.addTransition(nodes[s],nodes[base+(s-base+c+1)%half],new Interval(c*1000,c*1000+999));
        }
        return a;
    }
    @Benchmark public Object legacyCoaccessible(){return automaton.getUseFulStates();}
    @Benchmark public Object graphUseful(){return graph.usefulStates();}
    @Benchmark public Object importScalar(){return WeightedAutomaton.from(automaton,t->2);}
}
