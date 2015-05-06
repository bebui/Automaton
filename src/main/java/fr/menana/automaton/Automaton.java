package fr.menana.automaton;

import fr.menana.automaton.regexp.RegExpParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Main class to create and manipulate finite automaton
 * Created by Julien Menana on 01/05/2015.
 */
public class Automaton implements Cloneable {



    private List<State> states;
    private int initIndex;
    private BitSet acceptIndexes;
    private boolean deterministic;

    private List<Transition> transitions;

    public Automaton(){
        this.initIndex = -1;
        this.states = new ArrayList<>();
        this.transitions = new ArrayList<>();
        this.acceptIndexes = new BitSet();
        this.deterministic = true;
    }

    public List<State> getStates() {
        return states;
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
        this.acceptIndexes.set(s.index, accept);
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
        return this.getInitial().run(0, word);
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

    public Automaton union(Automaton other) { return Operation.union(this, other);}

    public Automaton complement() { return Operation.complement(this);}


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

    public void toDotty(String f) {
        String s = this.toDot();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(f)));
            bw.write(s);
            bw.close();
        } catch (IOException e) {
// System.err.println("Unable to write dotty file " + f);
        }
    }
    public String toDot() {
        StringBuilder b = new StringBuilder("digraph Automaton {\n");
        b.append(" rankdir = LR;\n");
        List<State> states = this.getStates();
// setStateNumbers(states);
        for (State s : states) {
            int idx = s.getIndex();
            b.append(" ").append(idx);
            if (s.isAccept())
                b.append(" [shape=doublecircle];\n");
            else
                b.append(" [shape=circle];\n");
            if (s.isInitial()) {
                b.append(" initial [shape=plaintext,label=\"\"];\n");
                b.append(" initial -> ").append(idx).append("\n");
            }
            for (Transition t : s.getTransitions().values()) {
                b.append(" ").append(idx);
                appendDot(t, b);
            }
        }
        return b.append("}\n").toString();
    }
    private void appendDot(Transition t, StringBuilder b) {
        int destIdx = t.dest.getIndex();
        b.append(" -> ").append(destIdx).append(" [label=\"");
        if (t.values == null && t.hasEpsilon())
            b.append("{eps}");
        else {
            b.append("{");
                for (Interval i : t.getIntervals()) {
                    b.append(i);
                    b.append(",");
                }
            if (b.charAt(b.length()-1) == ',')
                b.deleteCharAt(b.length()-1);

            b.append("}");

        }
        b.append("\"]\n");
    }


    public String toString() {
        StringBuffer buffer = new StringBuffer();
        for (State s : states) {
            for (Transition t : s.transitions.values())
                buffer.append(t).append("\n");
        }
        return buffer.toString();
    }

    public static Automaton nfaFromString(String regexp) {
        return new RegExpParser(regexp).parse().toNFA();
    }
    public static Automaton dfaFromString(String regexp) {
        return nfaFromString(regexp).minimize();
    }


    public static void main(String[] args) {
        Automaton auto =  dfaFromString("(1|3)*");


        System.out.println(auto);
        System.out.println(auto.complement());




    }


}
