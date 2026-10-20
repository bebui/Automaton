package audit;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.*;
import java.util.Arrays;

/** Reachable-object checks using the existing optional instrumentation agent. */
public final class SnapshotMemory {
    public static void main(String[] args) throws Exception {
        Automaton source=GraphBridgeBench.fixture(256,4);
        AutomatonSnapshot snapshot=source.snapshot();AutomatonGraph graph=snapshot.graph();
        WeightedAutomaton captured=snapshot.weighted(t->2);
        double[] values=new double[graph.getNbTransitions()];Arrays.fill(values,2);
        WeightedAutomaton direct=WeightedAutomaton.of(graph,values);
        MultiWeightedAutomaton capturedMulti=snapshot.multiWeighted(t->2,t->7);
        double[] vectors=new double[2*values.length];
        for(int e=0;e<values.length;e++){vectors[2*e]=2;vectors[2*e+1]=7;}
        MultiWeightedAutomaton directMulti=MultiWeightedAutomaton.of(graph,2,vectors);
        long capturedBytes=RetainedMemory.size(captured),directBytes=RetainedMemory.size(direct);
        long capturedMultiBytes=RetainedMemory.size(capturedMulti),directMultiBytes=RetainedMemory.size(directMulti);
        System.out.println("states=256 edges="+graph.getNbTransitions());
        System.out.println("snapshot_with_original_identity_metadata_bytes="+RetainedMemory.size(snapshot));
        System.out.println("captured_scalar_bytes="+capturedBytes+" direct_scalar_bytes="+directBytes);
        System.out.println("captured_vector_bytes="+capturedMultiBytes+" direct_vector_bytes="+directMultiBytes);
        if(capturedBytes!=directBytes||capturedMultiBytes!=directMultiBytes)
            throw new AssertionError("Captured weightings retain unexpected metadata");
    }
}
