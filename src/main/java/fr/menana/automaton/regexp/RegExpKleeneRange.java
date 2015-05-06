package fr.menana.automaton.regexp;

import fr.menana.automaton.Automaton;
import fr.menana.automaton.State;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Julien Menana on 05/05/2015.
 */
public class RegExpKleeneRange extends RegExp {

    RegExp internal;
    int min;
    int max;

    public RegExpKleeneRange(RegExp internal, String s) {
        this.internal = internal;
        this.parse(s);
    }

    public void  parse(String s) {
        String[] splitted = s.split(",");
        this.min = Integer.parseInt(splitted[0]);
        if (splitted.length > 1)
            this.max = Integer.parseInt(splitted[1]);
        else
            this.max = this.min;
    }

    @Override
    public String toString() {
        return this.internal.toString()+"{"+(this.max==this.min?this.min:(this.min+","+this.max))+"}";
    }

    @Override
    public Automaton toNFA() {
        Automaton auto = new RegExpEpsilon().toNFA();
        for (int i = 0 ; i < this.min ; ++i) {
            auto = auto.concatenate(this.internal.toNFA());
        }
        List<Integer> finalIndexes = new ArrayList<>();
        for (int i = this.min ; i < this.max ; ++i) {
            for (State s : auto.getAcceptList())
                finalIndexes.add(s.getIndex());
            auto = auto.concatenate(this.internal.toNFA());
        }
        State accept = auto.addState();
        for (int i : finalIndexes)
            auto.addEpsilonTransition(auto.getStates().get(i),accept);
        for (State s : auto.getAcceptList()) {
            auto.addEpsilonTransition(s,accept);
            auto.setAccept(s,false);
        }
        auto.setAccept(accept);

        return auto;

    }


}
