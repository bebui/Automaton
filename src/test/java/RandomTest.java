import fr.menana.automaton.Automaton;
import fr.menana.automaton.Operation;
import fr.menana.automaton.State;
import org.junit.Test;

import java.util.Random;
import java.util.Stack;

import static org.junit.Assert.assertEquals;

/**
 * Created by Julien Menana on 11/05/2015.
 */
public class RandomTest {

    private static Automaton random(long seed) {
        Random r = new Random(seed);
        Automaton auto = new Automaton();
        int nbStates = 20;//r.nextInt(1000);
        for (int i = 0 ; i < nbStates;++i)   {
            auto.addState();
        }
        if (auto.getStates().isEmpty())
            return auto;
        int taille = auto.getStates().size();
        auto.setInitial(auto.getStates().get(0));

        int nb = 0;
        int nbTr = r.nextInt(1000);
        Stack<State> stack = new Stack<>();
        stack.add(auto.getInitial());
        while (!stack.isEmpty() && nb < nbTr) {
            State current = stack.pop();
            int trnb = 50;//r.nextInt(100);
            for (int i = 0 ; i < trnb ; ++i) {
                int val = r.nextInt(2);
                if (!current.hasTransitionWith(val)) {
                    State next = auto.getStates().get(r.nextInt(taille));
                    auto.setAccept(next, r.nextInt(2) == 0);
                    auto.addTransition(current, next, val);
                    stack.add(next);
                }
                ++nb;
            }
        }

        auto.removeDeadStates();
        return auto;

    }

    private static int[] randomBinaryWord(Random r) {
        String word = Integer.toBinaryString(r.nextInt());
        int[] symbols = new int[word.length()];
        for (int k = 0; k < word.length(); k++)
            symbols[k] = word.charAt(k) - '0';
        return symbols;
    }

    /**
     * Generates random automata which accept languages over the alphabet {0,1}.
     * Checks if minimized automaton with Brzozowski or Hopcroft accepts the same randomly generated 8000 words.
     * Seeds are fixed so that a failure can be reproduced
     */
    @Test
    public void randomBinaryMinTest() {
        Random r = new Random(1);
        for (int i = 0 ; i < 250; ++i) {
            String seed = "automaton seed " + i;
            Automaton auto = random(i);
            Automaton hop = Operation.minimizeHopcroft(auto);
            Automaton brz = Operation.minimizeBrzozowski(auto);
            assertEquals(seed, brz.getNbStates(), hop.getNbStates());
            for (int j = 0 ; j < 8000 ; ++j) {
                int[] word = randomBinaryWord(r);
                boolean expected = auto.run(word);
                assertEquals(seed, expected, hop.run(word));
                assertEquals(seed, expected, brz.run(word));
            }
        }
    }

    /**
     * Checks that the complement of random automata accepts exactly the words the automata reject
     */
    @Test
    public void randomComplementTest() {
        Random r = new Random(2);
        for (int i = 0; i < 250; ++i) {
            String seed = "automaton seed " + (1000 + i);
            Automaton auto = random(1000 + i);
            Automaton compHop = Operation.minimizeHopcroft(auto.complement());
            Automaton hopComp = auto.minimize().complement().minimize();
            assertEquals(seed, compHop.getNbStates(), hopComp.getNbStates());
            for (int j = 0; j < 8000; ++j) {
                int[] word = randomBinaryWord(r);
                boolean expected = !auto.run(word);
                assertEquals(seed, expected, compHop.run(word));
                assertEquals(seed, expected, hopComp.run(word));
            }
        }
    }
}
