package audit;
import fr.menana.automaton.*;
import java.lang.reflect.*;
import java.util.*;

/** Counts the reachable object graph once by identity, using JVM instrumentation sizes. */
public class RetainedMemory {
    static long size(Object root) throws Exception {
        IdentityHashMap<Object,Boolean> visited=new IdentityHashMap<>();
        ArrayDeque<Object> todo=new ArrayDeque<>(); todo.add(root); long bytes=0;
        while(!todo.isEmpty()) {
            Object o=todo.removeLast(); if(visited.put(o,Boolean.TRUE)!=null)continue;
            bytes+=MemoryAgent.instrumentation.getObjectSize(o);
            Class<?> c=o.getClass();
            if(c.isArray()) {
                if(!c.getComponentType().isPrimitive()) for(Object value:(Object[])o)if(value!=null)todo.add(value);
            } else for(Class<?> type=c;type!=null;type=type.getSuperclass()) for(Field f:type.getDeclaredFields()) {
                if(Modifier.isStatic(f.getModifiers())||f.getType().isPrimitive())continue;
                if(!f.trySetAccessible())throw new IllegalStateException("Cannot inspect "+f);
                Object value=f.get(o);if(value!=null)todo.add(value);
            }
        }
        return bytes;
    }
    public static void main(String[] args) throws Exception {
        int n=1025,d=8,count=16;
        AutomatonGraph g=WeightedBench.chain(n).graph();
        double[] weights=new double[g.getNbTransitions()*d]; Arrays.fill(weights,1);
        MultiWeightedAutomaton multi=MultiWeightedAutomaton.of(g,d,weights);
        double[] coefficients=new double[d]; Arrays.fill(coefficients,0.5);
        Object[] views=new Object[count+1], materialized=new Object[count+1]; views[0]=materialized[0]=multi;
        for(int i=1;i<=count;i++){views[i]=multi.scalarize(coefficients); materialized[i]=multi.scalarizeMaterialized(coefficients);}
        System.out.println("states="+n+" edges="+g.getNbTransitions()+" dimensions="+d+" scalarizations="+count);
        System.out.println("graph_bytes="+size(g));
        System.out.println("multi_including_graph_bytes="+size(multi));
        System.out.println("multi_plus_16_views_bytes="+size(views));
        System.out.println("multi_plus_16_materialized_bytes="+size(materialized));
        WeightedAutomaton reweighted=multi.project(0);
        for(int i=0;i<100;i++)reweighted=reweighted.withWeights(new double[g.getNbTransitions()]);
        System.out.println("after_100_reweightings_bytes="+size(reweighted));
        System.out.println("fresh_scalar_bytes="+size(WeightedAutomaton.of(g,new double[g.getNbTransitions()])));
    }
}
