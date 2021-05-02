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
    int stack;
    String className;
    String superClass;

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
        className = classUnit.getClassName();
        if (classUnit.getSuperClass() == null)
            superClass = "java/lang/Object";
        else
            superClass = classUnit.getSuperClass();

        StringBuilder jasminCode = new StringBuilder(classHeader(classUnit));

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
                .append(".super ").append(superClass).append("\n");

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
        stack = 0;
        comparisons = 0;

        StringBuilder jasminCode = new StringBuilder(methodHeader(method));

        HashMap<String, Descriptor> varTable = method.getVarTable();

        StringBuilder instructions = new StringBuilder();
        HashMap<String, Instruction> labels = method.getLabels();
        for (Instruction instruction: method.getInstructions()) {
            for (String s : labels.keySet()) {
                if(labels.get(s) == instruction) {
                    instructions.append(s).append(":\n");
                }
            }
            instructions.append(generateInstruction(instruction, varTable));
        }

        //jasminCode.append("\t.limit stack ").append(stacklimit).append("\n");
        jasminCode.append("\t.limit stack 99\n");

        int locals = varTable.size();
        if (!method.isConstructMethod())
            locals++;
        jasminCode.append("\t.limit locals ").append(locals).append("\n\n");

        jasminCode.append(instructions);

        if (method.isConstructMethod())
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

            jasminCode.append(loadDescriptor(varTable.get(o.getName())))
                    .append(loadElement(index, varTable));
        }

        if (o.getType().getTypeOfElement() == ElementType.BOOLEAN
            && instruction.getRhs().getInstType() == InstructionType.BINARYOPER) {
            conditionals++;

            BinaryOpInstruction compare = (BinaryOpInstruction) instruction.getRhs();

            jasminCode.append(loadElement(compare.getLeftOperand(), varTable))
                    .append(loadElement(compare.getRightOperand(), varTable))
                    .append("\t").append(getOperation(compare.getUnaryOperation())).append(" True").append(conditionals).append("\n")
                    .append("\ticonst_0\n")
                    .append("\tgoto Store").append(conditionals).append("\n")
                    .append("True").append(conditionals).append(":\n")
                    .append("\ticonst_1\n")
                    .append("Store").append(conditionals).append(":\n");
        }
        else
            jasminCode.append(generateInstruction(instruction.getRhs(), varTable));

        if(o.getType().getTypeOfElement() == ElementType.INT32 || o.getType().getTypeOfElement() == ElementType.BOOLEAN)
            if (varTable.get(o.getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                jasminCode.append("\tiastore\n");

                limitStack(stack);
                stack = 0;
                return jasminCode.toString();
            }
            else
                jasminCode.append("\tistore");
        else {
            jasminCode.append("\tastore");
        }

        limitStack(stack);
        stack = 0;

        jasminCode.append((reg <= 3) ? "_" : " ").append(reg).append("\n");

        return jasminCode.toString();
    }

    private String branchInstruction(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {
        if (instruction.getCondOperation().getOpType() == OperationType.ANDB) {
            comparisons++;
            limitStack(1);
            stack = 0;

            return loadElement(instruction.getLeftOperand(), varTable) +
                    "\tifeq False" + comparisons + "\n" +
                    loadElement(instruction.getRightOperand(), varTable) +
                    "\tifeq False" + comparisons + "\n" +
                    "\tgoto " + instruction.getLabel() + "\n" +
                    "False" + comparisons + ":\n";
        }

        Element l = instruction.getLeftOperand();
        Element r = instruction.getRightOperand();

        if(((Operand)l).getName().equals(((Operand)r).getName())) {
            String jasminCode = loadElement(l, varTable);
            limitStack(stack);

            if (instruction.getCondOperation().getOpType() == OperationType.NOTB)
                jasminCode += "\tifeq ";
            else if (instruction.getCondOperation().getOpType() == OperationType.EQ)
                jasminCode += "\tifne ";

            stack = 0;
            return jasminCode + instruction.getLabel() + "\n";
        }

        String jasminCode = loadElement(l, varTable) +
                loadElement(r, varTable)+
                "\t" + getOperation(instruction.getCondOperation()) + " " + instruction.getLabel() + "\n";

        limitStack(stack);
        stack = 0;

        return jasminCode;
    }

    private String goToInstruction(GotoInstruction instruction) {
        return "\tgoto " + instruction.getLabel() + "\n";
    }

    private String returnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        if (!instruction.hasReturnValue())
            return "\treturn\n";

        ElementType returnType = instruction.getOperand().getType().getTypeOfElement();

        String jasminCode = loadElement(instruction.getOperand(), varTable)
            + "\t" + ((returnType == ElementType.INT32 || returnType == ElementType.BOOLEAN)? "i" : "a") + "return\n";

        limitStack(stack);
        stack = 0;
        return jasminCode;
    }

    private String singleOpInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        return loadElement(instruction.getSingleOperand(), varTable);
    }

    private String callInstruction(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();

        switch(instruction.getInvocationType()) {
            case NEW:
                if (instruction.getReturnType().getTypeOfElement() == ElementType.OBJECTREF) {
                    for (Element e: instruction.getListOfOperands()) {
                        jasminCode.append(loadElement(e, varTable));
                    }

                    jasminCode.append("\tnew ")
                            .append(((Operand) instruction.getFirstArg()).getName()).append("\n")
                            .append("\tdup\n");
                } else if(instruction.getReturnType().getTypeOfElement() == ElementType.ARRAYREF) {
                    for (Element e: instruction.getListOfOperands()) {
                        jasminCode.append(loadElement(e, varTable));
                    }

                    jasminCode.append("\tnewarray ");
                    if (instruction.getListOfOperands().get(0).getType().getTypeOfElement() == ElementType.INT32)
                        jasminCode.append("int\n");
                    else
                        jasminCode.append("array type not implemented\n");
                } else
                    jasminCode.append("\tnew (not implemented)\n");

                limitStack(stack);
                stack = 1;

                return jasminCode.toString();
            case invokespecial:
                jasminCode.append(loadElement(instruction.getFirstArg(), varTable));
                limitStack(stack);

                jasminCode.append("\tinvokespecial ")
                        .append((instruction.getFirstArg().getType().getTypeOfElement() == ElementType.THIS) ? superClass : className)
                        .append(".<init>(");

                for (Element e : instruction.getListOfOperands())
                    jasminCode.append(getDescriptor(e.getType()));

                jasminCode.append(")").append(getDescriptor(instruction.getReturnType())).append("\n");
                stack = 0;
                return jasminCode.toString();
            case invokevirtual:
                jasminCode.append(loadElement(instruction.getFirstArg(), varTable));

                for (Element e: instruction.getListOfOperands())
                    jasminCode.append(loadElement(e, varTable));

                limitStack(stack + 1);
                stack = (instruction.getReturnType().getTypeOfElement() == ElementType.VOID) ? 0 : 1;

                jasminCode.append("\tinvokevirtual ")
                        .append(getObjectName(((ClassType)instruction.getFirstArg().getType()).getName()))
                        .append(".").append(((LiteralElement)instruction.getSecondArg()).getLiteral().replace("\"", ""))
                        .append("(");

                for (Element e: instruction.getListOfOperands())
                    jasminCode.append(getDescriptor(e.getType()));

                jasminCode.append(")").append(getDescriptor(instruction.getReturnType())).append("\n");

                return jasminCode.toString();
            case invokestatic:
                for (Element e: instruction.getListOfOperands())
                    jasminCode.append(loadElement(e, varTable));

                limitStack(stack);
                jasminCode.append("\tinvokestatic ")
                        .append(getObjectName(((Operand)instruction.getFirstArg()).getName()))
                        .append(".").append(((LiteralElement)instruction.getSecondArg()).getLiteral().replace("\"", ""))
                        .append("(");

                for (Element e: instruction.getListOfOperands())
                    jasminCode.append(getDescriptor(e.getType()));

                jasminCode.append(")").append(getDescriptor(instruction.getReturnType())).append("\n");

                stack = (instruction.getReturnType().getTypeOfElement() == ElementType.VOID) ? 0 : 1;

                return jasminCode.toString();
            case ldc:
                return loadElement(instruction.getFirstArg(), varTable);
            case arraylength:
                return loadElement(instruction.getFirstArg(), varTable) + "\tarraylength\n";
            default:
                return "\tcall func not implemented\n";
        }
    }

    private String binaryOpInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        if (instruction.getUnaryOperation().getOpType() == OperationType.NOTB){
            conditionals++;
            String jasminCode = loadElement(instruction.getLeftOperand(), varTable) +
                    "\tifne True" + conditionals + "\n" +
                    "\ticonst_1\n" +
                    "\tgoto Store" + conditionals + "\n" +
                    "True" + conditionals + ":\n" +
                    "\ticonst_0\n" +
                    "Store" + conditionals + ":\n";

            stack = 1;
            return jasminCode;
        }

        return loadElement(instruction.getLeftOperand(), varTable) +
                unaryOpInstruction(instruction, varTable);
    }

    private String unaryOpInstruction(UnaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        String jasminCode = loadElement(instruction.getRightOperand(), varTable) +
                "\t" + getOperation(instruction.getUnaryOperation()) + "\n";

        limitStack(stack);
        stack -= stackSize(instruction.getRightOperand());
        return jasminCode;
    }

    private String putFieldInstruction(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        String jasminCode = loadElement(instruction.getFirstOperand(), varTable) +
                loadElement(instruction.getThirdOperand(), varTable) +
                "\tputfield " +
                getObjectName(((Operand) instruction.getFirstOperand()).getName()) +
                "/" + ((Operand) instruction.getSecondOperand()).getName() +
                " " + getDescriptor(instruction.getSecondOperand().getType()) + "\n";

        limitStack(stack);
        stack = 0;
        return jasminCode;
    }

    private String getFieldInstruction(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        String jasminCode = loadElement(instruction.getFirstOperand(), varTable) +
                "\tgetfield " +
                getObjectName(((Operand) instruction.getFirstOperand()).getName()) +
                "/" + ((Operand) instruction.getSecondOperand()).getName() +
                " " + getDescriptor(OllirAccesser.getFieldType(instruction)) + "\n";

        limitStack(stack);
        stack = 0;
        return jasminCode;
    }

    private String loadElement(Element e, HashMap<String, Descriptor> varTable) {
        if (e.isLiteral())
            return loadLiteral((LiteralElement) e);

        Descriptor d = varTable.get(((Operand) e).getName());
        if(d == null)
            return "!!!" + ((Operand) e).getName();

        try {
            if (e.getType().getTypeOfElement() != ElementType.ARRAYREF
                    && d.getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
                ArrayOperand arrayOp = (ArrayOperand) e;
                Element index = arrayOp.getIndexOperands().get(0);
                return loadDescriptor(d) + loadElement(index, varTable) + "\tiaload\n";
            }
        } catch (NullPointerException | ClassCastException except) {
            System.out.println(((Operand)e).getName());
            System.out.println(d.getVirtualReg() + " " + d.getVarType());
        }

        return loadDescriptor(d);
    }

    private String loadDescriptor(Descriptor descriptor) {
        stack += 1;
        try {
            ElementType t = descriptor.getVarType().getTypeOfElement();
            if (t == ElementType.THIS)
                return "\taload_0\n";

            int reg = descriptor.getVirtualReg();
            return "\t" + ((t == ElementType.INT32 || t == ElementType.BOOLEAN) ? "i" : "a") + "load" +
                    ((reg <= 3) ? "_" : " ") + reg + "\n";
        } catch (NullPointerException except) {
            System.out.println(descriptor.getVirtualReg());
            return "merde";
        }
    }

    private String loadLiteral(LiteralElement element) {
        stack += stackSize(element);
        String jasminCode = "\t";
        if (element.getType().getTypeOfElement() == ElementType.INT32 || element.getType().getTypeOfElement() == ElementType.BOOLEAN) {
            if (Integer.parseInt(element.getLiteral()) <= 5)
                jasminCode += "iconst_";
            else if (Integer.parseInt(element.getLiteral()) > 255)
                jasminCode += "ldc ";
            else if (Integer.parseInt(element.getLiteral()) > 127)
                jasminCode += "sipush ";
            else
                jasminCode += "bipush ";
        } else
            jasminCode += "ldc ";
        return jasminCode + element.getLiteral() + "\n";
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
            case DIV:
                return "idiv";
            case NOTB:
                return "ifeq";
            default:
                System.out.println(operation.getOpType());
                return "ERROR operation not implemented yet";
        }
    }

    private void limitStack(int s) {
        if (s > stacklimit)
            stacklimit = s;
    }

    private int stackSize(Element e) {
        if (e.isLiteral()) {
            int val = Integer.parseInt(((LiteralElement) e).getLiteral());
            if (val > 65535)
                return 4;
            else if(val > 255)
                return 2;
            else return 1;
        }
        return 1;
    }

    private String getObjectName(String name) {
        if (name.equals("this"))
            return className;
        return name;
    }
}
