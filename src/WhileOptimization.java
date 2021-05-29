import org.specs.comp.ollir.*;
import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class WhileOptimization{
    private ClassUnit ollirClass;

    public WhileOptimization(ClassUnit ollirClass) {
        this.ollirClass = ollirClass;
        this.optimizeLoops();
        this.removeUnusedLabels();
    }

    private void optimizeLoops() {
        for (Method method : ollirClass.getMethods()) {
            ArrayList<Instruction> methodInstructions =  method.getInstructions();
            for(int i=0; i<methodInstructions.size(); i++) {
                Instruction ins = methodInstructions.get(i);
                if(ins.getInstType() == InstructionType.BRANCH && ((CondBranchInstruction) ins).getLabel().startsWith("Body")) {
                    ((CondBranchInstruction) ins).getCondOperation().setOpType(OperationType.NEQ);
                    ((CondBranchInstruction) ins).setLabel(((GotoInstruction) methodInstructions.get(i+1)).getLabel());
                    methodInstructions.set(i,ins);
                }

            }
        }
    }

    private void removeUnusedLabels() {
        for (Method method : ollirClass.getMethods()) {
            ArrayList<Instruction> methodInstructions =  method.getInstructions();
            methodInstructions.removeIf(ins -> ins.getInstType() == InstructionType.GOTO && ((GotoInstruction) ins).getLabel().startsWith("EndLoop"));
        }
    }

    public ClassUnit getOptimizedClassUnit() {
        return this.ollirClass;
    }


}
