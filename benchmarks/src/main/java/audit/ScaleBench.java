package audit;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.*;
import fr.menana.automaton.weighted.compiled.*;
import org.openjdk.jmh.annotations.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** Symbolic-label and graph-size scaling; product/Pareto explosions are measured separately. */
@org.openjdk.jmh.annotations.State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations=3,time=500,timeUnit=TimeUnit.MILLISECONDS)
@Measurement(iterations=5,time=500,timeUnit=TimeUnit.MILLISECONDS)
@Fork(value=2,jvmArgsAppend={"-Xms128m","-Xmx256m"})
public class ScaleBench {
    @Param({"128","8192"})public int states;
    @Param({"2","16"})public int degree;
    @Param({"1","1000000"})public int width;
    WeightedAutomaton a, recognition;
    CompiledWeightedDfa compiled;
    int[] word=new int[64];
    @Setup(Level.Trial)public void setup(){
        WeightedAutomaton.Builder b=WeightedAutomaton.builder();
        for(int s=0;s<states;s++){b.addState();b.setAccept(s);}
        b.setInitial(0);
        for(int s=0;s<states-1;s++)for(int j=0;j<degree;j++)b.addTransition(s,Math.min(states-1,s+j+1),new Interval(j*width,(j+1)*width-1),j-1);
        a=b.build();
        WeightedAutomaton.Builder r=WeightedAutomaton.builder();
        for(int s=0;s<states;s++){r.addState();r.setAccept(s);}r.setInitial(0);
        for(int s=0;s<states;s++)for(int j=0;j<degree;j++)r.addTransition(s,(s+j+1)%states,new Interval(j*width,(j+1)*width-1),j-1);
        recognition=r.build();compiled=recognition.compile();
        for(int i=0;i<word.length;i++)word[i]=((i*37)%degree)*width;
        if(recognition.cost(word)!=compiled.cost(word))throw new AssertionError();
    }
    @Benchmark public Object dagDistances(){return WeightedPaths.dagShortestPaths(a);}
    @Benchmark public double recognizeLinear(){return recognition.cost(word);}
    @Benchmark public double recognizeCompiled(){return compiled.cost(word);}
}
