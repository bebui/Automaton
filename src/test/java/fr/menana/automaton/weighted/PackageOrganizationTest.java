package fr.menana.automaton.weighted;

import fr.menana.automaton.*;
import fr.menana.automaton.weighted.compiled.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PackageOrganizationTest {
    @Test public void oneIndexServesIndependentScalarAndVectorWeightings() {
        WeightedAutomaton.Builder b = WeightedAutomaton.builder(); b.addState(); b.addState();
        b.setInitial(0,0.25); b.setAccept(1,0.5);
        int high = b.addTransition(0,1,new Interval(1,Integer.MAX_VALUE),2);
        int low = b.addTransition(0,1,new Interval(Integer.MIN_VALUE,0),3);
        WeightedAutomaton a=b.build(), changed=a.withWeights(new double[]{5,7});
        WeightedDfaIndex index=WeightedDfaIndex.of(a.graph());
        assertEquals(low,index.transition(0,Integer.MIN_VALUE)); assertEquals(high,index.transition(0,Integer.MAX_VALUE));
        assertEquals(-1,index.transition(-1,0)); assertSame(a.graph(),index.graph());
        CompiledWeightedDfa scalar=CompiledWeightedDfa.of(a,index);
        CompiledMultiWeightedDfa vector=CompiledMultiWeightedDfa.of(MultiWeightedAutomaton.fromWeightings(a,changed),index);
        assertSame(index,scalar.index()); assertSame(index,vector.index());
        assertSame(index,vector.project(1).index()); assertSame(index,vector.scalarize(1,2).index());
        assertSame(index,scalar.reweight(changed).index());
        MultiWeightedAutomaton other=MultiWeightedAutomaton.of(a.graph(),3,new double[]{1,2,3,4,5,6});
        CompiledMultiWeightedDfa reweighted=vector.reweight(other); assertSame(index,reweighted.index());
        assertArrayEquals(new double[]{4,5,6},reweighted.costs(0),0);
        assertArrayEquals(new double[]{3.75,7.75},vector.costs(0),0);
        assertEquals(3.75,scalar.cost(0),0);
        assertThrows(IllegalArgumentException.class,()->CompiledWeightedDfa.of(b.build(),index));
        assertThrows(IllegalArgumentException.class,()->CompiledMultiWeightedDfa.of(MultiWeightedAutomaton.fromWeightings(b.build()),index));
        b.addEpsilonTransition(0,0,0); assertThrows(IllegalArgumentException.class,()->WeightedDfaIndex.of(b.build().graph()));
    }
    @Test public void primitiveIntervalInteropMatchesIndependentSetInspectionAndCopiesInputs() {
        Random random=new Random(99125);
        for(int round=0;round<120;round++) {
            IntervalSet set=new IntervalSet();for(int i=0;i<20;i++)if(random.nextBoolean())set.add(random.nextInt(61)-30);
            List<Interval> reference=new ArrayList<>(set.getIntervals());
            assertEquals(reference.size(),set.intervalCount());
            int[] minima=new int[reference.size()],maxima=new int[minima.length];
            for(int i=0;i<minima.length;i++) {
                minima[i]=set.getIntervalMin(i);maxima[i]=set.getIntervalMax(i);
                assertEquals(reference.get(i).getMin(),minima[i]);assertEquals(reference.get(i).getMax(),maxima[i]);
            }
            IntervalSet copy=IntervalSet.fromSortedIntervals(minima,maxima,0,minima.length);
            for(int symbol=-35;symbol<=35;symbol++)assertEquals(set.contains(symbol),copy.contains(symbol));
            Arrays.fill(minima,99);Arrays.fill(maxima,99);assertEquals(set,copy);
            assertThrows(IndexOutOfBoundsException.class,()->set.getIntervalMin(-1));
            assertThrows(IndexOutOfBoundsException.class,()->set.getIntervalMax(set.intervalCount()));
        }
        IntervalSet all=IntervalSet.fromSortedIntervals(new int[]{Integer.MIN_VALUE,0},new int[]{-1,Integer.MAX_VALUE},0,2);
        assertEquals(IntervalSet.ALL,all);assertEquals(1,all.intervalCount());
        assertTrue(IntervalSet.fromSortedIntervals(new int[0],new int[0],0,0).isEmpty());
        assertThrows(IllegalArgumentException.class,()->IntervalSet.fromSortedIntervals(new int[]{2},new int[]{1},0,1));
        assertThrows(IllegalArgumentException.class,()->IntervalSet.fromSortedIntervals(new int[]{0,1},new int[]{1,2},0,2));
        assertThrows(IllegalArgumentException.class,()->IntervalSet.fromSortedIntervals(new int[]{0},new int[0],0,0));
        assertThrows(IndexOutOfBoundsException.class,()->IntervalSet.fromSortedIntervals(new int[]{0},new int[]{1},1,0));
    }
    @Test public void publicAlphabetPartitionIsAnImmutableSnapshotAcrossExtremeBounds() {
        IntervalSet a=IntervalSet.fromInterval(new Interval(-2,4)),b=IntervalSet.fromInterval(new Interval(2,8));
        Alphabet alphabet=Alphabet.of(Arrays.asList(a,b,null));
        assertEquals(3,alphabet.size());
        assertEquals(-2,alphabet.min(0));assertEquals(1,alphabet.max(0));
        assertEquals(2,alphabet.min(1));assertEquals(4,alphabet.max(1));
        assertEquals(5,alphabet.min(2));assertEquals(8,alphabet.max(2));
        a.add(Integer.MIN_VALUE);b.add(Integer.MAX_VALUE);
        assertEquals(-2,alphabet.min(0));assertEquals(8,alphabet.max(2));
        Alphabet extreme=Alphabet.of(List.of(a,b,IntervalSet.ALL));
        assertEquals(Integer.MIN_VALUE,extreme.min(0));assertEquals(Integer.MAX_VALUE,extreme.max(extreme.size()-1));
        assertEquals(0,Alphabet.of(List.of()).size());
    }
}
