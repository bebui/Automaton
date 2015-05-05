import fr.menana.automaton.Automaton;
import fr.menana.automaton.State;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by julien on 03/05/2015.
 */
public class Test {
    public static void main(String[] args) {

        Automaton auto = new Automaton();

        State s0 = auto.addState();
        State s1 = auto.addState();


        Set<State> a = new HashSet<State>();
        a.add(s0);
        a.add(s1);
        Set<State> b = new HashSet<State>();
        b.add(s0);
        b.add(s1);

        System.out.println(a.equals(b));
        System.out.println(a == b);

        Map<Set<State>,Integer> mp =  new HashMap<Set<State>, Integer>();
        mp.put(b,10);

        System.out.println(mp.get(a));
    }
}
