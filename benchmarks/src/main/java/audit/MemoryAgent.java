package audit;
import java.lang.instrument.Instrumentation;
public class MemoryAgent {
    static Instrumentation instrumentation;
    public static void premain(String args, Instrumentation value){instrumentation=value;}
}
