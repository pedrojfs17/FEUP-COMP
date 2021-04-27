import java.util.*;

import org.specs.comp.ollir.*;

import org.specs.comp.ollir.Method;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import javax.swing.text.AbstractDocument;

/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class BackendStage implements JasminBackend {
    int conditionals;
    int comparisons;
    int stacklimit;
    String className;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ClassUnit ollirClass = ollirResult.getOllirClass();

        try {

            // Example of what you can do with the OLLIR class
            ollirClass.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            ollirClass.buildCFGs(); // build the CFG of each method
            ollirClass.outputCFGs(); // output to .dot files the CFGs, one per method
            ollirClass.buildVarTables(); // build the table of variables for each method
            ollirClass.show(); // print to console main information about the input OLLIR

            // Convert the OLLIR to a String containing the equivalent Jasmin code
            String jasminCode = generateClass(ollirClass);
            System.out.println("\n\n---------\n" + jasminCode + "\n---------\n\n");

            // More reports from this stage
            List<Report> reports = new ArrayList<>();

            return new JasminResult(ollirResult, jasminCode, reports);

        } catch (OllirErrorException e) {
            return new JasminResult(ollirClass.getClassName(), null,
                    Arrays.asList(Report.newError(Stage.GENERATION, -1, -1, "Exception during Jasmin generation", e)));
        }

    }

    private String generateClass(ClassUnit classUnit) {
        StringBuilder jasminCode = new StringBuilder(classHeader(classUnit));
        className = classUnit.getClassName();

        for (Field field: classUnit.getFields())
            jasminCode.append("\n").append(generateField(field));

        for (Method method: classUnit.getMethods())
            jasminCode.append("\n").append(generateMethod(method));

        return jasminCode.toString();
    }

    private String classHeader(ClassUnit classUnit) {
        StringBuilder jasminCode = new StringBuilder(".class");

        if (classUnit.getClassAccessModifier() != AccessModifiers.DEFAULT)
            jasminCode.append(" ").append(classUnit.getClassAccessModifier().toString().toLowerCase());

        jasminCode.append(" ").append(classUnit.getClassName()).append("\n")
                .append(".super java/lang/Object\n");

        return jasminCode.toString();
    }

    private String generateField(Field field) {
        StringBuilder jasminCode = new StringBuilder(".field");

        AccessModifiers accessModifier = field.getFieldAccessModifier();
        if (accessModifier != AccessModifiers.DEFAULT)
            jasminCode.append(" ").append(field.getFieldAccessModifier().toString().toLowerCase());

        if (field.isStaticField())
            jasminCode.append(" static");
        if (field.isFinalField())
            jasminCode.append(" final");

        jasminCode.append(" ").append(field.getFieldName())
                .append(" ").append(getDescriptor(field.getFieldType()));

        if(field.isInitialized()) {
            jasminCode.append(" = ").append(field.getInitialValue());
        }
        jasminCode.append("\n");
        return jasminCode.toString();
    }

    private String generateMethod(Method method) {
        conditionals = 0;
        stacklimit = 0;
        comparisons = 0;

        StringBuilder jasminCode = new StringBuilder(methodHeader(method));

        HashMap<String, Descriptor> varTable = OllirAccesser.getVarTable(method);

        StringBuilder instructions = new StringBuilder();
        HashMap<String, Instruction> labels = OllirAccesser.getMethodLabels(method);
        for (Instruction instruction: method.getInstructions()) {
            for (String s : labels.keySet()) {
                if(labels.get(s) == instruction) {
                    instructions.append(s).append(":\n");
                }
            }
            instructions.append(generateInstruction(instruction, varTable));
        }

        jasminCode.append("\t.limit stack ").append(stacklimit).append("\n");
        int locals = varTable.size();
        if (!method.isConstructMethod())
            locals++;
        jasminCode.append("\t.limit locals ").append(locals).append("\n\n");

        jasminCode.append(instructions);

        if (method.getReturnType().getTypeOfElement() == ElementType.VOID)
            jasminCode.append("\treturn\n");

        jasminCode.append(".end method\n");

        return jasminCode.toString();
    }

    private String methodHeader(Method method) {
        StringBuilder jasminCode = new StringBuilder(".method");

        if (method.getMethodAccessModifier() != AccessModifiers.DEFAULT)
            jasminCode.append(" ").append(method.getMethodAccessModifier().toString().toLowerCase());

        if (method.isStaticMethod())
            jasminCode.append(" static");
        if (method.isFinalMethod())
            jasminCode.append(" final");

        if(method.isConstructMethod())
            jasminCode.append(" <init>");
        else
            jasminCode.append(" ").append(method.getMethodName());

        jasminCode.append("(");
        for (Element param: method.getParams())
            jasminCode.append(getDescriptor(param.getType()));
        jasminCode.append(")").append(getDescriptor(method.getReturnType())).append("\n");

        return jasminCode.toString();
    }

    private String generateInstruction(Instruction instruction, HashMap<String, Descriptor> varTable) {
        switch(instruction.getInstType()) {
            case ASSIGN:
                return assignInstruction((AssignInstruction) instruction, varTable);
            case BRANCH:
                return branchInstruction((CondBranchInstruction) instruction, varTable);
            case GOTO:
                return goToInstruction((GotoInstruction) instruction);
            case RETURN:
                return returnInstruction((ReturnInstruction) instruction, varTable);
            case NOPER:
                return singleOpInstruction((SingleOpInstruction) instruction, varTable);
            case CALL:
                return callInstruction((CallInstruction) instruction, varTable);
            case BINARYOPER:
                return binaryOpInstruction((BinaryOpInstruction) instruction, varTable);
            case PUTFIELD:
                return putFieldInstruction((PutFieldInstruction) instruction, varTable);
            case GETFIELD:
                return getFieldInstruction((GetFieldInstruction) instruction, varTable);
            default:
                return "ERROR instruction not known\n";
        }
    }

    private String assignInstruction(AssignInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();

        Operand o = (Operand) instruction.getDest();
        int reg = varTable.get(o.getName()).getVirtualReg();

        if(varTable.get(o.getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF
            && o.getType().getTypeOfElement() != ElementType.ARRAYREF) {
            ArrayOperand arrayOp = (ArrayOperand) o;
            Element index = arrayOp.getIndexOperands().get(0);
            jasminCode.append(loadDescriptor(varTable.get(o.getName()))).append(loadElement(index, varTable));
            limitStack(3);
        }

        if (instruction.getDest().getType().getTypeOfElement() == ElementType.BOOLEAN
            && instruction.getRhs().getInstType() == InstructionType.BINARYOPER) {
            conditionals++;

            BinaryOpInstruction compare = (BinaryOpInstruction) instruction.getRhs();

            jasminCode.append(loadElement(compare.getLeftOperand(), varTable));
            jasminCode.append(loadElement(compare.getRightOperand(), varTable));
            jasminCode.append("\t").append(getOperation(compare.getUnaryOperation())).append(" True").append(conditionals).append("\n");
            jasminCode.append("\ticonst_0\n");
            jasminCode.append("\tgoto Store").append(conditionals).append("\n");
            jasminCode.append("True").append(conditionals).append(":\n\ticonst_1\nStore").append(conditionals).append(":\n");
            limitStack(2);
        }
        else
            jasminCode.append(generateInstruction(instruction.getRhs(), varTable));


        if(o.getType().getTypeOfElement() == ElementType.INT32 || o.getType().getTypeOfElement() == ElementType.BOOLEAN)
            if (varTable.get(o.getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                jasminCode.append("\tiastore\n");
                return jasminCode.toString();
            }
            else
                jasminCode.append("\tistore");
        else {
            jasminCode.append("\tastore");
        }

        if (reg <= 3)
            jasminCode.append("_");
        else
            jasminCode.append(" ");

        jasminCode.append(reg).append("\n");

        return jasminCode.toString();
    }

    private String branchInstruction(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {
        if (instruction.getCondOperation().getOpType() == OperationType.ANDB) {
            comparisons++;
            return loadElement(instruction.getLeftOperand(), varTable) +
                    "\tifeq False" + comparisons + "\n" +
                    loadElement(instruction.getRightOperand(), varTable) +
                    "\tifeq False" + comparisons + "\n" +
                    "\tgoto " + instruction.getLabel() + "\n" +
                    "False" + comparisons + ":\n";
        }

        String jasminCode = loadElement(instruction.getLeftOperand(), varTable) +
                loadElement(instruction.getRightOperand(), varTable);

        limitStack(2);

        jasminCode += "\t" + getOperation(instruction.getCondOperation()) +
                " " + instruction.getLabel() + "\n";
        return jasminCode;
    }

    private String goToInstruction(GotoInstruction instruction) {
        return "\tgoto " + instruction.getLabel() + "\n";
    }

    private String returnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        if (!instruction.hasReturnValue())
            return "\treturn\n";

        StringBuilder jasminCode = new StringBuilder();

        jasminCode.append(loadElement(instruction.getOperand(), varTable));
        limitStack(1);

        ElementType returnType = instruction.getOperand().getType().getTypeOfElement();

        if (returnType == ElementType.INT32 || returnType == ElementType.BOOLEAN)
            jasminCode.append("\tireturn\n");
        else
            jasminCode.append("\tareturn\n");

        return jasminCode.toString();
    }

    private String singleOpInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        return loadElement(instruction.getSingleOperand(), varTable);
    }

    private String callInstruction(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();

        CallType func = OllirAccesser.getCallInvocation(instruction);

        if (func == CallType.NEW) {
            if (instruction.getReturnType().getTypeOfElement() == ElementType.OBJECTREF) {
                for (Element e: instruction.getListOfOperands())
                    jasminCode.append(loadElement(e, varTable));
                jasminCode.append("\tnew ")
                        .append(((Operand) instruction.getFirstArg()).getName()).append("\n")
                        .append("\tdup\n");

                limitStack(Math.max(instruction.getNumOperands(), 1));
            } else if(instruction.getReturnType().getTypeOfElement() == ElementType.ARRAYREF) {
                for (Element e: instruction.getListOfOperands())
                    jasminCode.append(loadElement(e, varTable));

                jasminCode.append("\tnewarray ");
                switch (instruction.getListOfOperands().get(0).getType().getTypeOfElement()) {
                    case INT32:
                        jasminCode.append("int\n");
                        break;
                    default:
                        jasminCode.append("array type not implemented\n");
                }


                limitStack(Math.max(instruction.getNumOperands(), 1));
            } else
                jasminCode.append("\tnew (not implemented)\n");

        } else if(func == CallType.invokespecial) {
            jasminCode.append(loadElement(instruction.getFirstArg(), varTable));
            jasminCode.append("\t").append(func)
                    .append(" ").append(getFuncName(((LiteralElement)instruction.getSecondArg()).getLiteral()))
                    .append("(");
            for (Element e : instruction.getListOfOperands())
                jasminCode.append(getDescriptor(e.getType()));
            jasminCode.append(")").append(getDescriptor(instruction.getReturnType())).append("\n");
            limitStack(1);
        }
        else if (func == CallType.invokevirtual) {
            jasminCode.append(loadElement(instruction.getFirstArg(), varTable));
            for (Element e: instruction.getListOfOperands())
                jasminCode.append(loadElement(e, varTable));
            limitStack(instruction.getNumOperands() + 1);
            jasminCode.append("\tinvokevirtual ")
                    .append(getObjectName(((ClassType)instruction.getFirstArg().getType()).getName()))
                    .append(".").append(((LiteralElement)instruction.getSecondArg()).getLiteral().replace("\"", ""))
                    .append("(");
            for (Element e: instruction.getListOfOperands())
                jasminCode.append(getDescriptor(e.getType()));
            jasminCode.append(")").append(getDescriptor(instruction.getReturnType())).append("\n");
        }
        else if (func == CallType.invokestatic) {
            for (Element e: instruction.getListOfOperands())
                jasminCode.append(loadElement(e, varTable));
            limitStack(instruction.getNumOperands());
            jasminCode.append("\tinvokestatic ")
                    .append(getObjectName(((Operand)instruction.getFirstArg()).getName()))
                    .append(".").append(((LiteralElement)instruction.getSecondArg()).getLiteral().replace("\"", ""))
                    .append("(");
            for (Element e: instruction.getListOfOperands())
                jasminCode.append(getDescriptor(e.getType()));
            jasminCode.append(")").append(getDescriptor(instruction.getReturnType())).append("\n");
        }
        else if (func == CallType.ldc) {
            jasminCode.append(loadElement(instruction.getFirstArg(), varTable));
            limitStack(1);
        }
        else if (func == CallType.arraylength){
            jasminCode.append(loadElement(instruction.getFirstArg(), varTable));
            jasminCode.append("\t").append(func).append("\n");
            limitStack(1);
        }
        else {
            jasminCode.append("\tcall func not implemented\n");
        }

        return jasminCode.toString();
    }

    private String binaryOpInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        limitStack(2);
        return loadElement(instruction.getLeftOperand(), varTable) +
                unaryOpInstruction(instruction, varTable);
    }

    private String unaryOpInstruction(UnaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        limitStack(1);
        return loadElement(instruction.getRightOperand(), varTable) +
                "\t" + getOperation(OllirAccesser.getUnaryInstructionOp(instruction)) + "\n";
    }

    private String putFieldInstruction(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        limitStack(2);
        return loadElement(instruction.getFirstOperand(), varTable) +
                loadElement(instruction.getThirdOperand(), varTable) +
                "\tputfield " +
                getObjectName(((Operand) instruction.getFirstOperand()).getName()) +
                "/" + ((Operand) instruction.getSecondOperand()).getName() +
                " " + getDescriptor(instruction.getSecondOperand().getType()) + "\n";

    }

    private String getFieldInstruction(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        limitStack(1);

        return loadElement(instruction.getFirstOperand(), varTable) +
                "\tgetfield " +
                getObjectName(((Operand) instruction.getSecondOperand()).getName()) +
                "/" + ((Operand) instruction.getSecondOperand()).getName() +
                " " + getDescriptor(OllirAccesser.getFieldType(instruction)) + "\n";
    }

    private String loadElement(Element e, HashMap<String, Descriptor> varTable) {
        if (e.isLiteral())
            return loadLiteral((LiteralElement) e);

        Descriptor d = varTable.get(((Operand) e).getName());

        if (e.getType().getTypeOfElement() != ElementType.ARRAYREF
                && d.getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
            ArrayOperand arrayOp = (ArrayOperand) e;
            Element index = arrayOp.getIndexOperands().get(0);
            return loadDescriptor(d) + loadElement(index, varTable) + "\tiaload\n";
        }

        return loadDescriptor(d);
    }

    private String loadDescriptor(Descriptor descriptor) {
        ElementType t = descriptor.getVarType().getTypeOfElement();
        if (t == ElementType.THIS)
            return "\taload_0\n";

        StringBuilder jasminCode = new StringBuilder();
        if (t == ElementType.INT32 || t == ElementType.BOOLEAN)
            jasminCode.append("\tiload");
        else if (t == ElementType.STRING || t == ElementType.ARRAYREF || t == ElementType.OBJECTREF)
            jasminCode.append("\taload");
        else
            jasminCode.append("ERROR descriptor not implemented");

        int reg = descriptor.getVirtualReg();
        if (reg <= 3)
            jasminCode.append("_");
        else
            jasminCode.append(" ");
        jasminCode.append(reg).append("\n");

        return jasminCode.toString();
    }

    private String loadLiteral(LiteralElement element) {
        if (element.getType().getTypeOfElement() == ElementType.INT32 || element.getType().getTypeOfElement() == ElementType.BOOLEAN) {
            if (Integer.parseInt(element.getLiteral()) <= 5)
                return "\ticonst_" + element.getLiteral() + "\n";
            else
                return "\tbipush " + element.getLiteral() + "\n";
        }
        return "\tldc " + element.getLiteral() + "\n";
    }

    private String getDescriptor(Type type) {
        ElementType elementType = type.getTypeOfElement();
        String jasminCode = "";

        if (elementType == ElementType.ARRAYREF) {
            elementType = ((ArrayType) type).getTypeOfElements();
            jasminCode += "[";
        }

        switch (elementType) {
            case INT32:
                return jasminCode + "I";
            case VOID:
                return jasminCode + "V";
            case STRING:
                return jasminCode + "Ljava/lang/String;";
            case BOOLEAN:
                return jasminCode + "Z";
            default:
                return "ERROR descriptor not implemented";
        }
    }

    private String getOperation(Operation operation) {
        switch(operation.getOpType()) {
            case GTE:
                return "if_icmpge";
            case LTH:
                return "if_icmplt";
            case ADD:
                return "iadd";
            case MUL:
                return "imul";
            case SUB:
                return "isub";
            case EQ:
                return "if_icmpeq";
            default:
                return "ERROR operation not implemented yet";
        }
    }

    private void limitStack(int s) {
        if (s > stacklimit)
            stacklimit = s;
    }

    private String getObjectName(String name) {
        if (name.equals("this"))
            return className;
        return name;
    }

    private String getFuncName(String name) {
        if (name.equals("\"<init>\""))
            return "java/lang/Object/<init>";
        return name;
    }

}
