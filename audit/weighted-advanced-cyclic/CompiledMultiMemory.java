package audit;

import fr.menana.automaton.*;
import java.util.Arrays;

/** Reproducible reachable-object measurement using the existing JVM agent/walker. */
public class CompiledMultiMemory {
    public static void main(String[] args) throws Exception {
        int states=1024, dimensions=8, count=16;
        AutomatonGraph.Builder builder=AutomatonGraph.builder();
        for(int s=0;s<states;s++){builder.addState();builder.setAccept(s);}builder.setInitial(0);
        for(int s=0;s<states;s++)for(int c=0;c<4;c++)builder.addTransition(s,(s+c+1)%states,new Interval(c*20,c*20+19));
        AutomatonGraph graph=builder.build();int size=graph.getNbTransitions()*dimensions;
        MultiWeightedAutomaton multi=MultiWeightedAutomaton.of(graph,dimensions,new double[size]);
        CompiledMultiWeightedDfa compiled=multi.compile();
        double[] coefficients=new double[dimensions];Arrays.fill(coefficients,1);
        Object[] shared=new Object[count+1], separate=new Object[count+1];shared[0]=separate[0]=compiled;
        for(int i=1;i<=count;i++){
            shared[i]=compiled.scalarize(coefficients);
            separate[i]=multi.scalarize(coefficients).compile();
        }
        System.out.println("states="+states+" edges="+graph.getNbTransitions()+" dimensions="+dimensions+" scalarizations="+count);
        System.out.println("compiled_multi_bytes="+RetainedMemory.size(compiled));
        System.out.println("compiled_plus_16_shared_scalar_indexes_bytes="+RetainedMemory.size(shared));
        System.out.println("compiled_plus_16_separate_scalar_indexes_bytes="+RetainedMemory.size(separate));
        CompiledMultiWeightedDfa last=compiled;
        for(int i=0;i<100;i++)last=last.reweight(MultiWeightedAutomaton.of(graph,dimensions,new double[size]));
        long retained=RetainedMemory.size(last);
        long fresh=RetainedMemory.size(MultiWeightedAutomaton.of(graph,dimensions,new double[size]).compile());
        System.out.println("after_100_reweightings_bytes="+retained);
        System.out.println("fresh_compiled_multi_bytes="+fresh);
        if(retained!=fresh)throw new AssertionError("Reweight history retained");
    }
}
