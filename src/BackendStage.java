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
        } else if (o.getType().getTypeOfElement() == ElementType.BOOLEAN
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

        jasminCode.append((reg <= 3) ? "_" : " ").append(reg).append("\n");

        return jasminCode.toString();
    }

    private String branchInstruction(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {
        if (instruction.getCondOperation().getOpType() == OperationType.ANDB) {
            comparisons++;
            limitStack(1);

            return loadElement(instruction.getLeftOperand(), varTable) +
                    "\tifeq False" + comparisons + "\n" +
                    loadElement(instruction.getRightOperand(), varTable) +
                    "\tifeq False" + comparisons + "\n" +
                    "\tgoto " + instruction.getLabel() + "\n" +
                    "False" + comparisons + ":\n";
        }

        limitStack(2);

        return loadElement(instruction.getLeftOperand(), varTable) +
                loadElement(instruction.getRightOperand(), varTable) +
                "\t" + getOperation(instruction.getCondOperation()) + " " + instruction.getLabel() + "\n";
    }

    private String goToInstruction(GotoInstruction instruction) {
        return "\tgoto " + instruction.getLabel() + "\n";
    }

    private String returnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        if (!instruction.hasReturnValue())
            return "\treturn\n";

        limitStack(1);
        ElementType returnType = instruction.getOperand().getType().getTypeOfElement();

        return loadElement(instruction.getOperand(), varTable)
            + "\t" + ((returnType == ElementType.INT32 || returnType == ElementType.BOOLEAN)? "i" : "a") + "return\n";
    }

    private String singleOpInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        return loadElement(instruction.getSingleOperand(), varTable);
    }

    private String callInstruction(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();

        switch(instruction.getInvocationType()) {
            case NEW:
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
                    if (instruction.getListOfOperands().get(0).getType().getTypeOfElement() == ElementType.INT32)
                        jasminCode.append("int\n");
                    else
                        jasminCode.append("array type not implemented\n");

                    limitStack(Math.max(instruction.getNumOperands(), 1));
                } else
                    jasminCode.append("\tnew (not implemented)\n");
                return jasminCode.toString();
            case invokespecial:
                jasminCode.append(loadElement(instruction.getFirstArg(), varTable));

                jasminCode.append("\tinvokespecial ")
                        .append((instruction.getFirstArg().getType().getTypeOfElement() == ElementType.THIS) ? superClass : className)
                        .append(".<init>(");

                for (Element e : instruction.getListOfOperands())
                    jasminCode.append(getDescriptor(e.getType()));

                jasminCode.append(")").append(getDescriptor(instruction.getReturnType())).append("\n");
                limitStack(1);
                return jasminCode.toString();
            case invokevirtual:
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
                return jasminCode.toString();
            case invokestatic:
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
                return jasminCode.toString();
            case ldc:
                limitStack(1);
                return loadElement(instruction.getFirstArg(), varTable);
            case arraylength:
                limitStack(1);
                return loadElement(instruction.getFirstArg(), varTable) + "\tarraylength\n";
            default:
                return "\tcall func not implemented\n";
        }
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
                getObjectName(((Operand) instruction.getFirstOperand()).getName()) +
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

        int reg = descriptor.getVirtualReg();
        return "\t" + ((t == ElementType.INT32 || t == ElementType.BOOLEAN) ? "i" : "a") + "load" +
                ((reg <= 3) ? "_" : " ") + reg + "\n";
    }

    private String loadLiteral(LiteralElement element) {
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
}
