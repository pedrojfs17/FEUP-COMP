import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;

import java.util.*;

public class RegisterAllocationOptimizer {
    private ClassUnit classUnit;

    public RegisterAllocationOptimizer(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public ClassUnit allocateRegisters(int n) {
        for (Method method: classUnit.getMethods()) {
            allocateRegisters(n, method);
        }

        return classUnit;
    }

    private void allocateRegisters(int n, Method method) {
        HashMap<Node, BitSet> in = new HashMap<>();
        HashMap<Node, BitSet> out = new HashMap<>();

        for (Instruction instruction: method.getInstructions()) {
            in.put((Node) instruction, new BitSet());
            out.put((Node) instruction, new BitSet());
        }

        boolean done = false;
        while (!done) {
            HashMap<Node, BitSet> in_temp = new HashMap<>();
            HashMap<Node, BitSet> out_temp = new HashMap<>();

            for (Instruction instruction: method.getInstructions()) {

            }
        }
    }
}
