import fr.menana.automaton.Interval;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class IntervalTest {

    @Test
    public void testContains() {
        Interval interval = new Interval(5, 10);
        assertTrue(interval.contains(7));
        assertFalse(interval.contains(4));
        assertFalse(interval.contains(11));
        assertTrue(interval.contains(5));
        assertTrue(interval.contains(10));
    }

    @Test
    public void testIntersects() {
        Interval interval1 = new Interval(5, 10);
        Interval interval2 = new Interval(8, 12);
        Interval interval3 = new Interval(1, 4);
        Interval interval4 = new Interval(11, 15);

        assertTrue(interval1.intersects(interval2));
        assertFalse(interval1.intersects(interval3));
        assertFalse(interval1.intersects(interval4));
    }

    @Test
    public void testIntersection() {
        Interval interval1 = new Interval(5, 10);
        Interval interval2 = new Interval(8, 12);
        Interval interval3 = new Interval(1, 4);

        Interval intersection1 = interval1.intersection(interval2);
        assertEquals(8, intersection1.getMin());
        assertEquals(10, intersection1.getMax());

        assertNull(interval1.intersection(interval3));
    }

    @Test
    public void testComplement() {
        Interval interval = new Interval(5, 10);
        List<Interval> complement = interval.complement();
        assertEquals(2, complement.size());
        assertEquals(Integer.MIN_VALUE, complement.get(0).getMin());
        assertEquals(4, complement.get(0).getMax());
        assertEquals(11, complement.get(1).getMin());
        assertEquals(Integer.MAX_VALUE, complement.get(1).getMax());
    }
}
