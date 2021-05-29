import org.specs.comp.ollir.*;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;

import java.util.*;

public class RegisterAllocationOptimizer {
    private final ClassUnit classUnit;

    public RegisterAllocationOptimizer(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public ClassUnit allocateRegisters(int n) {
        try {
            classUnit.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            classUnit.buildCFGs(); // build the CFG of each method
            classUnit.outputCFGs(); // output to .dot files the CFGs, one per method
            classUnit.buildVarTables(); // build the table of variables for each method
            classUnit.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (Method method: classUnit.getMethods()) {
            System.out.println("Method: " + method.getMethodName());
            allocateRegisters(n, method);
        }

        return classUnit;
    }

    private void allocateRegisters(int n_registers, Method method) {
        HashMap<Node, BitSet> def = new HashMap<>();
        HashMap<Node, BitSet> use = new HashMap<>();

        HashMap<Node, BitSet> in = new HashMap<>();
        HashMap<Node, BitSet> out = new HashMap<>();

        int n_vars = method.getVarTable().size();
        for (Instruction instruction: method.getInstructions()) {
            def.put(instruction, getDefinedVars(instruction, method.getVarTable()));
            use.put(instruction, getUsedVars(instruction, method.getVarTable()));
            in.put(instruction, new BitSet(n_vars));
            out.put(instruction, new BitSet(n_vars));
        }

        boolean done = false;
        while (!done) {
            HashMap<Node, BitSet> in_temp = new HashMap<>();
            HashMap<Node, BitSet> out_temp = new HashMap<>();

            done = true;
            for (Instruction instruction: method.getInstructions()) {
                in_temp.replace(instruction, in.get(instruction));
                out_temp.replace(instruction, out.get(instruction));

                BitSet new_out = new BitSet(n_vars);
                if (instruction.getSucc1() != null) {
                    if(instruction.getSucc1().getNodeType() != NodeType.END) {
                        new_out = in.get(instruction.getSucc1());
                        if (instruction.getSucc2() != null) {
                            new_out.or(in.get(instruction.getSucc2()));
                        }
                    }
                }
                out.replace(instruction, new_out);

                BitSet new_in = out.get(instruction);
                for (Node node: out.keySet()) {
                    System.out.println(node + " " + out.get(node));
                }
                System.out.println(out);
                new_in.xor(def.get(instruction));
                new_in.or(use.get(instruction));
                in.replace(instruction, new_in);

                if (!in_temp.get(instruction).equals(in.get(instruction)) || !out_temp.get(instruction).equals(out.get(instruction)))
                    done = false;
            }
        }
    }

    private BitSet getDefinedVars(Instruction instruction, HashMap<String, Descriptor> varTable) {
        BitSet vars = new BitSet();

        if(instruction.getInstType() == InstructionType.ASSIGN)
            setElementBit(vars, ((AssignInstruction) instruction).getDest(), varTable);

        return vars;
    }

    private BitSet getUsedVars(Instruction instruction, HashMap<String, Descriptor> varTable) {
        switch (instruction.getInstType()) {
            case CALL:
                return getUsedVarsCall((CallInstruction) instruction, varTable);
            case NOPER:
                return getUsedVarsSingleOp((SingleOpInstruction) instruction, varTable);
            case ASSIGN:
                return getUsedVarsAssign((AssignInstruction) instruction, varTable);
            case BRANCH:
                return getUsedVarsBranch((CondBranchInstruction) instruction, varTable);
            case RETURN:
                return getUsedVarsReturn((ReturnInstruction) instruction, varTable);
            case GETFIELD:
                return getUsedVarsGetField((GetFieldInstruction) instruction, varTable);
            case PUTFIELD:
                return getUsedVarsPutField((PutFieldInstruction) instruction, varTable);
            case UNARYOPER:
                return getUsedVarsUnaryOp((UnaryOpInstruction) instruction, varTable);
            case BINARYOPER:
                return getUsedVarsBinaryOp((BinaryOpInstruction) instruction, varTable);
            default:
                break;
        }
        return new BitSet();
    }

    private BitSet getUsedVarsCall(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        BitSet vars = new BitSet();
        setElementBit(vars, instruction.getFirstArg(), varTable);
        if (instruction.getNumOperands() > 1) {
            if (instruction.getInvocationType() != CallType.NEW)
                setElementBit(vars, instruction.getSecondArg(), varTable);

            for (Element arg: instruction.getListOfOperands())
                setElementBit(vars, arg, varTable);
        }
        return vars;
    }

    private BitSet getUsedVarsSingleOp(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        BitSet vars = new BitSet();
        setElementBit(vars, instruction.getSingleOperand(), varTable);
        return vars;
    }

    private BitSet getUsedVarsAssign(AssignInstruction instruction, HashMap<String, Descriptor> varTable) {
        return getUsedVars(instruction.getRhs(), varTable);
    }

    private BitSet getUsedVarsBranch(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {
        BitSet vars = new BitSet();
        setElementBit(vars, instruction.getRightOperand(), varTable);
        setElementBit(vars, instruction.getLeftOperand(), varTable);
        return vars;
    }

    private BitSet getUsedVarsReturn(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        BitSet vars = new BitSet();
        setElementBit(vars, instruction.getOperand(), varTable);
        return vars;
    }

    private BitSet getUsedVarsGetField(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        BitSet vars = new BitSet();
        setElementBit(vars, instruction.getFirstOperand(), varTable);
        setElementBit(vars, instruction.getSecondOperand(), varTable);
        return vars;
    }

    private BitSet getUsedVarsPutField(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        BitSet vars = getUsedVarsGetField(instruction, varTable);
        setElementBit(vars, instruction.getThirdOperand(), varTable);
        return vars;
    }

    private BitSet getUsedVarsUnaryOp(UnaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        BitSet vars = new BitSet();
        setElementBit(vars, instruction.getRightOperand(), varTable);
        return vars;
    }

    private BitSet getUsedVarsBinaryOp(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        BitSet vars = getUsedVarsUnaryOp(instruction, varTable);
        setElementBit(vars, instruction.getLeftOperand(), varTable);
        return vars;
    }

    private void setElementBit(BitSet vars, Element e, HashMap<String, Descriptor> varTable) {
        if (e.isLiteral() || ((Operand) e).isParameter())
            return;

        int reg =  varTable.get(((Operand) e).getName()).getVirtualReg();
        vars.set(reg);
    }

    private String showBitset(BitSet bitset) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < bitset.length(); i++)
            s.append(bitset.get(i) ? '1': '0');
        return s.toString();
    }
}
