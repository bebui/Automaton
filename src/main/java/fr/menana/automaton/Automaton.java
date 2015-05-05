package fr.menana.automaton;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

/**
 * Created by julien on 01/05/2015.
 */
public class Automaton implements Cloneable {



    private List<State> states;
    private int initIndex;
    private BitSet acceptIndexes;
    private boolean deterministic;

    private List<Transition> transitions;

    public Automaton(){
        this.initIndex = -1;
        this.states = new ArrayList<State>();
        this.transitions = new ArrayList<Transition>();
        this.acceptIndexes = new BitSet();
        this.deterministic = true;
    }

    public State addState() {
        State s = new State();
        this.states.add(s);
        s.index = this.states.size() - 1;
        return s;
    }

    public void setInitial(State s) {
        if (this.initIndex >= 0) {
            this.states.get(this.initIndex).initial = false;
        }
        this.initIndex = s.index;
        s.initial = true;
    }

    public void setAccept(State s) {
        this.setAccept(s,true);
    }
    public void setAccept(State s,boolean accept) {
        this.acceptIndexes.set(s.index,accept);
        s.accept = accept;
    }

    public boolean isInitial(State s) {
        return s.index == this.initIndex;
    }

    public State getInitial() {
        if (this.initIndex >= 0)
            return this.states.get(this.initIndex);
        return null;
    }

    public List<State> getAcceptList() {
        List<State> liste = new ArrayList<State>(this.acceptIndexes.cardinality());
        for (int i = this.acceptIndexes.nextSetBit(0) ; i >= 0 ; i = this.acceptIndexes.nextSetBit(i+1)) {
            liste.add(this.states.get(i));
        }
        return liste;
    }

    public void addTransition(State orig, State dest, IntervalSet values)
    {
        Transition t = new Transition(orig,dest,values);

        deterministic &= !orig.addTransition(t);
    }
    public void addTransition(State orig, State dest, Interval values)
    {
        Transition t = new Transition(orig,dest,values);
        deterministic &= !orig.addTransition(t);
    }

    public void addTransition(State orig, State dest, int... values) {
        Transition t = new Transition(orig,dest,values);

        deterministic &= !orig.addTransition(t);
    }
    public void addEpsilonTransition(State orig,State dest) {
        Transition t = new Transition(orig,dest);
        orig.addTransition(t);
        deterministic = false;
    }

    public boolean isDeterministic() {
        return deterministic;
    }

    public boolean run(int... word)
    {
        return this.getInitial().run(0,word);
    }

    public Automaton determinize()
    {
        return Operation.determinize(this);
    }

    public Automaton minimize() {
        return Operation.minimize(this);
    }

    public Automaton revert() {
        return Operation.revert(this);
    }

    public Automaton concatenate(Automaton other) { return Operation.concatenate(this, other);}


    public Automaton clone() {

        Automaton clone = null;
        try {
            clone = (Automaton) super.clone();
            clone.states = new ArrayList<State>(this.states.size());
            clone.acceptIndexes = (BitSet)this.acceptIndexes.clone();
            clone.deterministic = this.deterministic;
            clone.initIndex = this.initIndex;
            for (int i = 0 ; i < this.states.size(); ++i)
                clone.addState();
            for (State s : this.states) {
                int idx = s.index;
                State newS = clone.states.get(idx);
                if (s.initial)
                    clone.setInitial(newS);
                if (s.accept)
                    clone.setAccept(newS);
                for (Transition t : s.transitions.values()) {
                    Transition nt;
                    if (t.values != null)
                        nt = new Transition(newS,clone.states.get(t.dest.index),t.values.clone());
                    else
                        nt = new Transition(newS,clone.states.get(t.dest.index));
                    nt.epsilon = t.epsilon;
                    newS.transitions.put(nt.dest, nt);
                }
            }

        } catch (CloneNotSupportedException e) {
            System.err.println("ne devrait pas arriver");
        }

        return clone;


    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        for (State s : states) {
            for (Transition t : s.transitions.values())
                buffer.append(t).append("\n");
        }
        return buffer.toString();
    }


    public static void main(String[] args) {
        Automaton auto = new Automaton();
        State a = auto.addState();
        State b = auto.addState();
        State c = auto.addState();
        State d = auto.addState();
        State e = auto.addState();
        State f = auto.addState();
        State g = auto.addState();
        State h = auto.addState();

        auto.setInitial(a);

        auto.addTransition(a, b, 1);
        auto.addTransition(a, h, 0);


        auto.addTransition(b, a, 1);
        auto.addTransition(b, h, 0);

        auto.addTransition(h, c, 0,1);

        auto.addTransition(c, f, 1);
        auto.addTransition(c, e, 0);

        auto.addTransition(e, f, 0);
        auto.addTransition(e, g, 1);

        auto.addTransition(g, g, 0);
        auto.addTransition(g, f, 1);

         auto.addTransition(f, f, 0, 1);



        auto.setAccept(g);
        auto.setAccept(f);

        Automaton auto2 = auto.revert();
        Automaton auto3 = auto.minimize();

        Random r = new Random();
        int count = 0;
        for (long  i = 0; i < 100000; ++i)
        {
            String s = Integer.toBinaryString(r.nextInt());
            String[] tmp = s.split("");
            int size = r.nextInt(tmp.length);
            int[] word = new int[size];
            for (int k = 0 ; k < size ; ++k) {
                word[k] = Integer.parseInt(tmp[k]);

            }

             if (auto.run(word) != auto3.run(word))
                 System.out.println("PB");

        }
        System.out.println(auto3);


        System.out.println(auto);


        // auto.setAccept(h);

        //System.out.println(auto.revert().determinize().revert().determinize());


       /* auto = new fr.menana.automaton.Automaton();
        fr.menana.automaton.State s0 = auto.addState();
        fr.menana.automaton.State s1 = auto.addState();
        fr.menana.automaton.State s2 = auto.addState();

        auto.setInitial(s0);
        auto.setAccept(s2);

        auto.addEpsilonTransition(s0, s1);

        auto.addTransition(s1, s1, 1);
        auto.addTransition(s1, s2, 0);


        System.out.println(auto);

        System.out.println(auto.determinize());*/




    }


    public List<State> getStates() {
        return states;
    }
}
