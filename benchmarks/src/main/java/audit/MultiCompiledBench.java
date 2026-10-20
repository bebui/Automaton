package audit;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.*;
import fr.menana.automaton.weighted.compiled.*;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

/** Direct compiled vector recognition, index reuse and scalar-to-vector composition. */
@org.openjdk.jmh.annotations.State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations=3,time=500,timeUnit=TimeUnit.MILLISECONDS)
@Measurement(iterations=5,time=500,timeUnit=TimeUnit.MILLISECONDS)
@Fork(value=2,jvmArgsAppend={"-Xms128m","-Xmx256m"})
public class MultiCompiledBench {
    @Param({"2", "8", "32"}) public int dimensions;
    MultiWeightedAutomaton multi, alternative;
    CompiledMultiWeightedDfa compiled;
    WeightedAutomaton[] weightings;
    WeightedAutomaton productLeft, productRight;
    int[] word; double[] output, coefficients;
    @Setup(Level.Trial) public void setup() {
        AutomatonGraph.Builder graph = AutomatonGraph.builder();
        for (int s = 0; s < 128; s++) { graph.addState(); graph.setAccept(s); } graph.setInitial(0);
        for (int s = 0; s < 128; s++) for (int c = 0; c < 16; c++)
            graph.addTransition(s,(s+c+1)%128,new Interval(c*1_000_000,(c+1)*1_000_000-1));
        AutomatonGraph g = graph.build(); double[] values = new double[g.getNbTransitions()*dimensions];
        for (int e = 0; e < g.getNbTransitions(); e++) for (int d = 0; d < dimensions; d++) values[e*dimensions+d]=(e%7+d)/4.0;
        multi=MultiWeightedAutomaton.of(g,dimensions,values); compiled=multi.compile();
        alternative=MultiWeightedAutomaton.of(g,dimensions,new double[values.length]);
        weightings=new WeightedAutomaton[dimensions]; for(int d=0;d<dimensions;d++)weightings[d]=multi.project(d);
        word=new int[64];for(int i=0;i<64;i++)word[i]=((i*37)%16)*1_000_000;
        output=new double[dimensions];coefficients=new double[dimensions];java.util.Arrays.fill(coefficients,0.25);
        productLeft=WeightedBench.chain(9);productRight=WeightedBench.chain(9);
        if(!compiled.costs(word,output)||compiled.scalarize(coefficients).cost(word)!=multi.scalarize(coefficients).cost(word))throw new AssertionError();
    }
    @Benchmark public boolean vectorBuffered(){return compiled.costs(word,output);}
    @Benchmark public Object vectorAllocated(){return compiled.costs(word);}
    @Benchmark public Object sharedWeightings(){return MultiWeightedAutomaton.fromWeightings(weightings);}
    @Benchmark public Object compileVector(){return multi.compile();}
    @Benchmark public Object reweightVector(){return compiled.reweight(alternative);}
    @Benchmark public Object scalarizeCompiled(){return compiled.scalarize(coefficients);}
    @Benchmark public Object scalarizeAndCompile(){return multi.scalarize(coefficients).compile();}
    @Benchmark public Object productScalars(){return MultiWeightedAutomaton.synchronizedProduct(productLeft,productRight);}
}
