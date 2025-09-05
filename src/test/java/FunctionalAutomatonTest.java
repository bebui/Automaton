import fr.menana.automaton.Automaton;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FunctionalAutomatonTest {

    @Test
    public void testRegexpScenario() {
        // 1. Create an automaton from a regular expression
        // This regexp should match any sequence of 1s and 2s that contains "12"
        Automaton auto = Automaton.dfaFromString("(1|2)*12(1|2)*");

        // 2. Test with words that should be accepted
        assertTrue(auto.run(toIntArray("12")));
        assertTrue(auto.run(toIntArray("112")));
        assertTrue(auto.run(toIntArray("122")));
        assertTrue(auto.run(toIntArray("1122")));
        assertTrue(auto.run(toIntArray("2121")));

        // 3. Test with words that should be rejected
        assertFalse(auto.run(toIntArray("")));
        assertFalse(auto.run(toIntArray("1")));
        assertFalse(auto.run(toIntArray("2")));
        assertFalse(auto.run(toIntArray("21")));
        assertFalse(auto.run(toIntArray("2211")));
    }

    private int[] toIntArray(String s) {
        if (s.isEmpty()) {
            return new int[0];
        }
        String[] parts = s.split("");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
    }
}
