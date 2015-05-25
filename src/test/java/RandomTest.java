import fr.menana.automaton.Automaton;
import fr.menana.automaton.Operation;
import fr.menana.automaton.State;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Julien Menana on 11/05/2015.
 */
public class RandomTest {

    private static Automaton random() {
        return random(new Random().nextLong());
    }

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

    private void printBar(int cur, int max) {
        StringBuilder builder = new StringBuilder("[");
        for (int l = 1 ; l <= cur ; ++l) {
            builder.append('*');
        }
        for (int l = cur + 1 ; l<= max ; ++l) {
            builder.append(" ");
        }
        builder.append("]\r");
        System.out.print(builder);

    }

    /**
     * Generates thousand random automaton which accept language composed with the alphabet {0,1}
     * Checks if minimized automaton with Brzozowski or Hopcroft accepts the same randomly generated 10000 words
     */
    @Test
    public void randomBinaryMinTest() {
        long totalhop = 0;
        long totalbrz = 0;
        Random r = new Random();

        for (int i = 0 ; i < 500; ++i) {


           printBar((i+1)/50,10);

            Automaton auto = random();//.determinize();


            // System.out.print(i+"("+auto.getNbStates()+")");
            //System.out.print(" => HOP : ");
            long tick = System.currentTimeMillis();
            Automaton hop = Operation.minimizeHopcroft(auto);
            long thop = System.currentTimeMillis() - tick;
            totalhop+= thop;
            // System.out.print(thop + "ms");
            // hop.toDotty("hop.dot");

            //System.out.print(", BRZ : ");
            tick = System.currentTimeMillis();
            Automaton brz = Operation.minimizeBrzozowski(auto);
            long tbrz = System.currentTimeMillis()-tick;
            totalbrz += tbrz;
            //System.out.print(tbrz + "ms");

            //   boolean eq = brz.equals(hop);

            //  System.out.println(", nbS : " + hop.getNbStates() + " " + (hop.getNbStates() == brz.getNbStates()));


            boolean eq = true;

            for (int j = 0 ; j < 10000 ; ++j) {

                System.out.print("");

                String word = Integer.toBinaryString(r.nextInt());
                char[] parts = word.toCharArray();
                int[] iParts = new int[parts.length];
                int k = 0;
                for (char s : parts)
                    iParts[k++] = Integer.parseInt(s + "");

                boolean bhop = hop.run(iParts);
                boolean bbrz = brz.run(iParts);
                boolean baut = auto.run(iParts);
                eq &= bhop == bbrz;
                assertTrue(eq);

                eq &= bhop == baut;
                assertTrue(eq);
            }
            if (!eq) {
                System.out.println("PB: "+i);
                auto.toDotty("det" + i + ".dot");
                brz.toDotty("brz" + i + ".dot");
                hop.toDotty("hop" + i + ".dot");
            }

        }


    }

   @Test
   public void randomComplementTest() {

       Random r = new Random();

       for (int i = 0 ; i < 500; ++i) {

           printBar((i+1)/50,10);
           Automaton auto = random();//.determinize();
           Automaton comp = auto.complement();

           Automaton hop = Operation.minimizeHopcroft(auto);
           Automaton compHop = Operation.minimizeHopcroft(comp);
           Automaton hopComp = hop.complement().minimize();


           boolean eq = true;

           for (int j = 0 ; j < 10000 ; ++j) {

               System.out.print("");

               String word = Integer.toBinaryString(r.nextInt());
               char[] parts = word.toCharArray();
               int[] iParts = new int[parts.length];
               int k = 0;
               for (char s : parts)
                   iParts[k++] = Integer.parseInt(s + "");

               boolean bhop = hop.run(iParts);
               boolean bcompHop = compHop.run(iParts);
               boolean bhopComp = hopComp.run(iParts);
               eq &= bhop != bcompHop;
               assertTrue(eq);

               eq &= bcompHop == bhopComp;
               assertTrue(eq);
           }
           if (!eq) {
               System.out.println("PB: "+i);
               auto.toDotty("det" + i + ".dot");
               hop.toDotty("hop" + i + ".dot");
               compHop.toDotty("chop" + i + ".dot");
               hopComp.toDotty("hcop" + i + ".dot");

           }

       }

   }

}

