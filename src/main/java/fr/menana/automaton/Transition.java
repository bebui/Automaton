package fr.menana.automaton;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by julien on 01/05/2015.
 */
public class Transition  {

    public State orig;
    public State dest;
    public IntervalSet values;
    boolean epsilon;

    public Transition(State orig, State dest, IntervalSet values)
    {
        this.orig = orig;
        this.dest = dest;
        this.values = values;
    }

    public Transition(State orig, State dest, int[] values)
    {
        this(orig,dest,IntervalSet.fromIntArray(values));
    }
    public Transition(State orig, State dest, Interval values)
    {
        this(orig,dest,IntervalSet.fromInterval(values));
    }

    public Transition(State orig, State dest, int value)
    {
        this(orig,dest,new int[]{value}) ;
    }

    public Transition(State orig,State dest)
    {
        this(orig,dest,(IntervalSet) null);
        this.epsilon = true;
    }

    public boolean hasEpsilon() {
        return this.epsilon;
    }

    public void addEpsilon() {
        this.epsilon = true;
    }

    public List<Interval> getIntervals() {
        if (values != null)
            return new ArrayList<>(this.values.getIntervals());
        else
            return new ArrayList<>(0);
    }


    public String toString() {
        return orig + " -> " + (values != null ?values:"") + (epsilon?"e":"")+" -> "+dest;
    }



}
