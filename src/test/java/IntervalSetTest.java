import fr.menana.automaton.Interval;
import fr.menana.automaton.IntervalSet;
import org.junit.Test;

import static org.junit.Assert.*;

public class IntervalSetTest {

    @Test
    public void testAdd() {
        IntervalSet set = new IntervalSet();
        set.add(5);
        set.add(7);
        set.add(6);
        assertEquals("[[|5,7|]]", set.toString());
    }

    @Test
    public void testRemove() {
        IntervalSet set = new IntervalSet();
        set.add(new Interval(1, 10));
        IntervalSet toRemove = new IntervalSet();
        toRemove.add(new Interval(4, 6));
        set.remove(toRemove);
        assertEquals("[[|1,3|], [|7,10|]]", set.toString());
    }

    @Test
    public void testIntersection() {
        IntervalSet set1 = new IntervalSet();
        set1.add(new Interval(1, 5));
        set1.add(new Interval(8, 10));

        IntervalSet set2 = new IntervalSet();
        set2.add(new Interval(4, 9));

        IntervalSet intersection = set1.intersection(set2);
        assertEquals("[[|4,5|], [|8,9|]]", intersection.toString());
    }

    @Test
    public void testUnion() {
        IntervalSet set1 = new IntervalSet();
        set1.add(new Interval(1, 5));
        IntervalSet set2 = new IntervalSet();
        set2.add(new Interval(6, 10));

        IntervalSet union = set1.union(set2);
        assertEquals("[[|1,10|]]", union.toString());
    }

    @Test
    public void testComplement() {
        IntervalSet set = new IntervalSet();
        set.add(new Interval(5, 10));
        IntervalSet complement = set.complement();
        assertEquals("[[|-∞,4|], [|11,∞|]]", complement.toString());
    }
}
