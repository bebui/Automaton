package audit;

import fr.menana.automaton.*;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations=3, time=500, timeUnit=TimeUnit.MILLISECONDS)
@Measurement(iterations=5, time=500, timeUnit=TimeUnit.MILLISECONDS)
@Fork(value=2, jvmArgsAppend={"-Xms128m", "-Xmx256m"})
public class WeightedBench {
    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class Scalar {
        WeightedAutomaton dag, dfa, cyclic, epsilon;
        WeightedAutomaton.Evaluator evaluator, epsilonEvaluator;
        LayeredAutomaton layered;
        LayeredAutomaton.Analysis analysis;
        CompiledWeightedDfa compiled;
        int[] word = new int[64];
        @Setup(Level.Trial) public void setup() {
            java.util.Arrays.fill(word,1);
            dag=chain(129); evaluator=dag.evaluator();
            layered=LayeredAutomaton.exactly(dag.graph(),64); analysis=layered.analyze(dag);
            WeightedAutomaton.Builder b=WeightedAutomaton.builder();
            for(int i=0;i<128;i++){ b.addState(); b.setAccept(i); }
            b.setInitial(0);
            for(int i=0;i<128;i++) for(int j=0;j<8;j++) b.addTransition(i,(i+j+1)%128,new Interval(j*100,j*100+99),j);
            dfa=b.build(); cyclic=dfa; compiled=dfa.compile();
            b=WeightedAutomaton.builder(); b.addState(); b.addState(); b.setInitial(0); b.setAccept(1);
            b.addEpsilonTransition(0,0,0); b.addTransition(0,1,1,1); b.addTransition(1,1,1,1); epsilon=b.build(); epsilonEvaluator=epsilon.evaluator();
            if(dag.cost(word)!=dag.bestPath(word).cost())throw new AssertionError();
        }
    }
    static WeightedAutomaton chain(int n) {
        WeightedAutomaton.Builder b=WeightedAutomaton.builder();
        for(int s=0;s<n;s++)b.addState(); b.setInitial(0); b.setAccept(n-1);
        for(int s=0;s<n-1;s++) {
            b.addTransition(s,s+1,new Interval(0,9),(s%5)-2);
            if(s+2<n)b.addTransition(s,s+2,new Interval(0,9),(s%7)-3);
        }
        return b.build();
    }
    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class Vectors {
        @Param({"2","8","32"}) public int dimensions;
        MultiWeightedAutomaton multi;
        WeightedAutomaton scalar;
        double[] coefficients;
        @Setup(Level.Trial) public void setup() {
            AutomatonGraph g=chain(129).graph();
            double[] w=new double[g.getNbTransitions()*dimensions];
            for(int i=0;i<w.length;i++)w[i]=(i%7)-3;
            multi=MultiWeightedAutomaton.of(g,dimensions,w);
            coefficients=new double[dimensions]; java.util.Arrays.fill(coefficients,0.5);
            scalar=multi.scalarize(coefficients);
            if(scalar.graph()!=g)throw new AssertionError();
        }
    }
    @org.openjdk.jmh.annotations.State(Scope.Thread)
    public static class Front {
        @Param({"strong","tradeoff"}) public String dominance;
        MultiWeightedAutomaton a;
        @Setup(Level.Trial) public void setup() {
            MultiWeightedAutomaton.Builder b=MultiWeightedAutomaton.builder(2);
            for(int s=0;s<19;s++)b.addState(); b.setInitial(0); b.setAccept(18);
            for(int s=0;s<18;s++) { b.addTransition(s,s+1,1,1,dominance.equals("strong")?1:2); b.addTransition(s,s+1,1,2,dominance.equals("strong")?2:1); }
            a=b.build();
            if(a.paretoFront().size()!=(dominance.equals("strong")?1:19))throw new AssertionError();
        }
    }
    @Benchmark public WeightedAutomaton construction(){return chain(129);}
    @Benchmark public double scalarDfa(Scalar s){return s.dfa.cost(s.word);}
    @Benchmark public double compiledDfa(Scalar s){return s.compiled.cost(s.word);}
    @Benchmark public double nfaCost(Scalar s){return s.dag.cost(s.word);}
    @Benchmark public double nfaReusable(Scalar s){return s.evaluator.cost(s.word);}
    @Benchmark public double epsilonCost(Scalar s){return s.epsilon.cost(s.word);}
    @Benchmark public double epsilonReusable(Scalar s){return s.epsilonEvaluator.cost(s.word);}
    @Benchmark public Object dagDistances(Scalar s){return WeightedPaths.dagShortestPaths(s.dag);}
    @Benchmark public Object backwardDistances(Scalar s){return WeightedPaths.backwardDistances(s.dag);}
    @Benchmark public Object dijkstra(Scalar s){return WeightedPaths.dijkstra(s.cyclic);}
    @Benchmark public Object bellmanFord(Scalar s){return WeightedPaths.bellmanFord(s.cyclic);}
    @Benchmark public Object reconstructWord(Scalar s){return s.dag.bestPath(s.word);}
    @Benchmark public Object union(Scalar s){return s.dag.union(s.dag);}
    @Benchmark public Object concatenate(Scalar s){return s.dag.concatenate(s.dag);}
    @Benchmark public Object product(Scalar s){return s.dag.intersection(s.dag);}
    @Benchmark public Object unfolding(Scalar s){return LayeredAutomaton.exactly(s.dag.graph(),64);}
    @Benchmark public Object reuseUnfolding(Scalar s){return s.layered.evaluate(s.dag);}
    @Benchmark public double conditionalCost(Scalar s){return s.analysis.costAt(32,1);}
    @Benchmark public Object scalarizeView(Vectors s){return s.multi.scalarize(s.coefficients);}
    @Benchmark public Object scalarizeMaterialized(Vectors s){return s.multi.scalarizeMaterialized(s.coefficients);}
    @Benchmark public double scalarizedShortest(Vectors s){return s.scalar.optimalCost();}
    @Benchmark public Object lexicographic(Vectors s){return s.multi.lexicographicBestPath();}
    @Benchmark public Object pareto(Front f){return f.a.paretoFront();}
    @Benchmark public Object kBest(Scalar s){return s.dag.kBestPaths(10);}
}
