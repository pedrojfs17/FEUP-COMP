package visitors.optimizations;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;

public class LivenessAnalysis {
    public ArrayList<HashMap<Node, BitSet>> livenessAnalysis(Method method) {
        HashMap<Node, BitSet> def = new HashMap<>();
        HashMap<Node, BitSet> use = new HashMap<>();

        HashMap<Node, BitSet> in = new HashMap<>();
        HashMap<Node, BitSet> out = new HashMap<>();

        int n_vars = method.getVarTable().size();
        for (Instruction instruction : method.getInstructions()) {
            def.put(instruction, getDefinedVars(instruction, method.getVarTable()));
            use.put(instruction, getUsedVars(instruction, method.getVarTable()));
            in.put(instruction, new BitSet(n_vars));
            out.put(instruction, new BitSet(n_vars));
        }

        boolean done = false;
        int i = 0;
        ArrayList<Instruction> nodes = new ArrayList<>(method.getInstructions());
        Collections.reverse(nodes);

        while (!done) {
            System.out.println("iteration " + i);
            i++;
            HashMap<Node, BitSet> in_temp = new HashMap<>(in);
            HashMap<Node, BitSet> out_temp = new HashMap<>(out);

            for (Instruction instruction : nodes) {
                BitSet new_out = new BitSet(n_vars);
                if (instruction.getSucc1() != null) {
                    if (instruction.getSucc1().getNodeType() != NodeType.END) {
                        new_out = (BitSet) in.get(instruction.getSucc1()).clone();
                        if (instruction.getSucc2() != null) {
                            new_out.or(in.get(instruction.getSucc2()));
                        }
                    }
                }
                out.replace(instruction, new_out);

                BitSet new_in = (BitSet) out.get(instruction).clone();
                BitSet temp_def = def.get(instruction);
                for (int index = 0; index < n_vars; index++) {
                    if (new_in.get(index) && !temp_def.get(index))
                        new_in.set(index);
                    else
                        new_in.clear(index);
                }
                new_in.or(use.get(instruction));
                in.replace(instruction, new_in);
            }

            printTable(use, def, in, out, nodes, n_vars + 1);

            done = true;
            for (Instruction instruction : nodes) {
                if (!in.get(instruction).equals(in_temp.get(instruction)))
                    done = false;
                if (!out.get(instruction).equals(out_temp.get(instruction)))
                    done = false;
            }
        }

        ArrayList<HashMap<Node, BitSet>> result = new ArrayList<>();
        result.add(in);
        result.add(out);

        return result;
    }

    private BitSet getDefinedVars(Instruction instruction, HashMap<String, Descriptor> varTable) {
        BitSet vars = new BitSet();

        if (instruction.getInstType() == InstructionType.ASSIGN)
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

            for (Element arg : instruction.getListOfOperands())
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
        if (instruction.hasReturnValue())
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
        if (e.getType().getTypeOfElement() == ElementType.THIS) {
            vars.set(0);
            return;
        }

        if (e.isLiteral())
            return;

        Descriptor d = varTable.get(((Operand) e).getName());

        if (d.getVarType().getTypeOfElement() == ElementType.ARRAYREF
                && e.getType().getTypeOfElement() == ElementType.INT32) {
            for (Element index : ((ArrayOperand) e).getIndexOperands())
                setElementBit(vars, index, varTable);
        }

        if (d.getScope() == VarScope.PARAMETER || d.getScope() == VarScope.FIELD)
            return;

        int reg = d.getVirtualReg();
        vars.set(reg);
    }

    private String showBitset(BitSet bitset, int n) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < bitset.length(); i++)
            s.append(bitset.get(i) ? '1' : '0');
        while (s.length() < n)
            s.append('0');
        return s.toString();
    }

    private void printTable(HashMap<Node, BitSet> use, HashMap<Node, BitSet> def, HashMap<Node, BitSet> in, HashMap<Node, BitSet> out, ArrayList<Instruction> nodes, int n) {
        int table_size = Math.max(n, 15) + 1;
        System.out.format("%10s%" + table_size + "s%" + table_size + "s%10s%" + table_size + "s%" + table_size + "s\n", "node", "use", "def", "succ", "out", "in");
        for (Node node : nodes) {
            StringBuilder succ = new StringBuilder();
            if (node.getSucc1() != null) {
                succ.append(node.getSucc1().getId());
                if (node.getSucc2() != null)
                    succ.append(",");
            }
            if (node.getSucc2() != null)
                succ.append(node.getSucc2().getId());

            System.out.format("%10d%" + table_size + "s%" + table_size + "s%10s%" + table_size + "s%" + table_size + "s\n",
                    node.getId(), showBitset(use.get(node), n), showBitset(def.get(node), n),
                    succ, showBitset(out.get(node), n), showBitset(in.get(node), n));
        }
    }
}

