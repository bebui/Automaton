package fr.menana.automaton;

import java.util.*;

/**
 * Created by julien on 01/05/2015.
 */
public class Interval implements Comparable<Interval>,Cloneable {

    int min;
    int max;


    public Interval(int min, int max)
    {
        this.min = min;
        this.max = max;
        if (this.min > this.max) {
            System.err.println("min > max, on echange les valeurs");
            this.min = max;
            this.max = min;
        }
    }

    public boolean contains(int value)
    {
        return value >= this.min && value <= this.max;
    }
    public boolean contains(Interval value)
    {
        return this.contains(value.min) && this.contains(value.max) ||
                this.equals(value);
    }

    public boolean intersects(Interval o) {
        if (o == null)
            return false;
        return !(this.max < o.min || o.max < this.min);

    }

    public int compareTo(Interval o) {
        if (this == o || this.equals(o))
            return 0;
        if (this.min < o.min)
            return -1;
        else if (this.min > o.min)
            return 1;
        else
            return this.max - o.max;

    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        else if (o != null && o instanceof Interval) {
            Interval i = (Interval) o;
            return this.min == i.min && this.max == i.max;
        }
        return false;
    }

    public String toString() {
        String min = Integer.toString(this.min);
        String max = Integer.toString(this.max);
        if (this.min == Integer.MIN_VALUE)
            min = "-"+Character.toString('\u221e');
        else if (this.min == Integer.MAX_VALUE)
            min = "-"+Character.toString('\u221e');
        if (this.max == Integer.MIN_VALUE)
            max = "-"+Character.toString('\u221e');
        if (this.max == Integer.MAX_VALUE)
            max = Character.toString('\u221e');

        return "["+min+(this.max==this.min ? "]" : ","+max+"]");
    }

    public Interval intersection(Interval o) {
        if (! intersects(o))
            return null;
        Interval ret = new Interval(Math.max(this.min,o.min),Math.min(this.max, o.max));
        return ret;
    }

    public List<Interval> complement()
    {
        List<Interval> out = new ArrayList<>(2);
        if (this.min != Integer.MIN_VALUE && this.max != Integer.MAX_VALUE) {
            out.add(new Interval(Integer.MIN_VALUE,this.min - 1));
            out.add(new Interval(this.max + 1 , Integer.MAX_VALUE));
        }
        else if (this.min == Integer.MIN_VALUE && this.max != Integer.MAX_VALUE) {
            out.add(new Interval(this.max +1, Integer.MAX_VALUE));
        }
        else if (this.min != Integer.MIN_VALUE && this.max == Integer.MAX_VALUE) {
            out.add(new Interval(Integer.MIN_VALUE,this.min - 1));
        }
        return out;
    }

    public Interval clone() {
        Interval clone = null;
        try {
            clone = (Interval) super.clone();
            clone.max = this.max;
            clone.min = this.min;
        } catch (CloneNotSupportedException e) {}
        return clone;
    }



    public static void main(String[] args) {
        IntervalSet set = new IntervalSet();

        set.add(1, 4,7);

        System.out.println(set);

        IntervalSet set2 = new IntervalSet();
        set2.add(IntervalSet.ALL);

        System.out.println(set2);

        System.out.println(set.intersection(set2));
        System.out.println(set2.intersection(set));



       /* for (int i = 0 ; i <= 10;++i)
            set2.add(i);

        for (int i = 20 ; i <= 30;++i)
            set2.add(i);

        for (int i = 40 ; i <= 50;++i)
            set2.add(i);
        for (int i = 60 ; i <= 70;++i)
            set2.add(i);



        //set2.add(new fr.menana.automaton.Interval(0, 10));
       // set2.add(new fr.menana.automaton.Interval(20,30));
      //  set2.add(new fr.menana.automaton.Interval(40,50));
     //
     //   System.transitions.println(set2);

        set2.add(new fr.menana.automaton.Interval(55, 200));
        //set2.add(new fr.menana.automaton.Interval(29, 100));
       // set2.add(new fr.menana.automaton.Interval(Integer.MIN_VALUE,-2));
       // set2.add(new fr.menana.automaton.Interval(15,19));

        set2.remove(1);
        for (int i = 40 ; i <= 50 ; ++i)
            set2.remove(i);
        System.out.println(set2);

        fr.menana.automaton.IntervalSet set = new fr.menana.automaton.IntervalSet();

        set.add(new fr.menana.automaton.Interval(5,25));
        set.add(new fr.menana.automaton.Interval(35,40));
        set.add(new fr.menana.automaton.Interval(-1, 2));
       // set.add(new fr.menana.automaton.Interval(-100, 53));
        //System.out.println(set);

        //System.out.println(set.intersection(set2));
        //System.out.println(set);

        System.out.println();
        System.out.println("######");
        System.out.println(set);
        System.out.println(set2);


        System.out.println(fr.menana.automaton.IntervalSet.union(Arrays.asList(set, set2)));  */






    }




}
