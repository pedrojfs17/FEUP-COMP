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

        for (Field field: classUnit.getFields())
            jasminCode.append("\n").append(generateField(field));

        for (Method method: classUnit.getMethods())
            jasminCode.append("\n").append(generateMethod(method));

        return jasminCode.toString();
    }

    private String classHeader(ClassUnit classUnit) {
        StringBuilder jasminCode = new StringBuilder(".class");

        if (classUnit.getClassAccessModifier() == AccessModifiers.DEFAULT)
            jasminCode.append(" public");
        else
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
        StringBuilder jasminCode = new StringBuilder(methodHeader(method));

        HashMap<String, Descriptor> varTable = OllirAccesser.getVarTable(method);

        HashMap<String, Instruction> labels = OllirAccesser.getMethodLabels(method);
        for (Instruction instruction: method.getInstructions()) {
            for (String s : labels.keySet()) {
                if(labels.get(s) == instruction) {
                    jasminCode.append(s).append(":\n");
                }
            }
            jasminCode.append(generateInstruction(instruction, varTable));
        }

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
            jasminCode.append("<init>");
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
        StringBuilder jasminCode  = new StringBuilder(generateInstruction(instruction.getRhs(), varTable));

        Operand o = (Operand) instruction.getDest();
        int reg = varTable.get(o.getName()).getVirtualReg();

        if(o.getType().getTypeOfElement() == ElementType.INT32)
            if (varTable.get(o.getName()).getVarType().getTypeOfElement() == ElementType.ARRAYREF)
                jasminCode.append("\tiastore_");
            else
                jasminCode.append("\tistore_");
        else {
            jasminCode.append("\tastore_");
        }

        jasminCode.append(reg).append("\n");

        return jasminCode.toString();
    }

    private String branchInstruction(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();

        jasminCode.append(loadElement(instruction.getLeftOperand(), varTable))
                .append(loadElement(instruction.getRightOperand(), varTable));

        jasminCode.append("\tif_icmp");

        Operation op = instruction.getCondOperation();
        switch(op.getOpType()) {
            case GTE:
                jasminCode.append("ge");
                break;
            default:
                jasminCode.append("ERROR");
        }

        jasminCode.append(" ").append(instruction.getLabel()).append("\n");

        return jasminCode.toString();
    }

    private String goToInstruction(GotoInstruction instruction) {
        return "\tgoto " + instruction.getLabel() + "\n";
    }

    private String returnInstruction(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        if (instruction.hasReturnValue())
            return "\treturn\n";

        StringBuilder jasminCode = new StringBuilder();

        jasminCode.append(loadElement(instruction.getOperand(), varTable));

        Element returnValue = instruction.getOperand();
        if (returnValue.isLiteral() || ((Operand) returnValue).getType().getTypeOfElement() == ElementType.INT32)
            jasminCode.append("\tireturn\n");
        else
            jasminCode.append("\tareturn\n");

        return jasminCode.toString();
    }

    private String singleOpInstruction(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        Element singleOperand = instruction.getSingleOperand();
        if (singleOperand.isLiteral()) {
            String val = ((LiteralElement) singleOperand).getLiteral();

            return "\tldc " + val + "\n";
        }

        Operand o = (Operand) singleOperand;
        int reg = varTable.get(o.getName()).getVirtualReg();

        if (o.getType().getTypeOfElement() == ElementType.INT32)
            return "\tiload_" + reg + "\n";
        else
            return "\taload_" + reg + "\n";
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
            } else
                jasminCode.append("\tnew (not implemented)\n");
        } else if(func == CallType.invokespecial) {
            jasminCode.append(loadElement(instruction.getFirstArg(), varTable));
            jasminCode.append("\t").append(func)
                    .append(" ").append("java/lang/Object.<init>()V;")
                    .append("\n");
        }
        else if (func == CallType.invokevirtual) {
            jasminCode.append(loadElement(instruction.getFirstArg(), varTable));
            for (Element e: instruction.getListOfOperands())
                jasminCode.append(loadElement(e, varTable));
            jasminCode.append("\tinvokevirtual ")
                    .append(((Operand)instruction.getFirstArg()).getName())
                    .append(".").append(((LiteralElement)instruction.getSecondArg()).getLiteral().replace("\"", ""))
                    .append("(");
            for (Element e: instruction.getListOfOperands())
                jasminCode.append(getDescriptor(e.getType()));
            jasminCode.append(")").append(getDescriptor(instruction.getReturnType())).append("\n");
        }
        else if (func == CallType.invokestatic) {
            for (Element e: instruction.getListOfOperands())
                jasminCode.append(loadElement(e, varTable));
            jasminCode.append("\tinvokestatic ")
                    .append(((Operand)instruction.getFirstArg()).getName())
                    .append(".").append(((LiteralElement)instruction.getSecondArg()).getLiteral().replace("\"", ""))
                    .append("(");
            for (Element e: instruction.getListOfOperands())
                jasminCode.append(getDescriptor(e.getType()));
            jasminCode.append(")").append(getDescriptor(instruction.getReturnType())).append("\n");
        }
        else if (func == CallType.ldc) {
            jasminCode.append(loadElement(instruction.getFirstArg(), varTable));
        }
        else if (func == CallType.arraylength){
            jasminCode.append(loadElement(instruction.getFirstArg(), varTable));
            jasminCode.append("\t").append(func).append("\n");
        }
        else {
            jasminCode.append("\tcall func not implemented\n");
        }

        return jasminCode.toString();
    }

    private String binaryOpInstruction(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder(loadElement(instruction.getLeftOperand(), varTable));
        jasminCode.append(loadElement(instruction.getRightOperand(), varTable));


        switch (instruction.getUnaryOperation().getOpType()) {
            case ADD:
                jasminCode.append("\tiadd\n");
                break;
            case MUL:
                jasminCode.append("\timul\n");
                break;
            default:
                jasminCode.append("ERROR\n");
                break;
        }

        return jasminCode.toString();
    }

    private String unaryOpInstruction(UnaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append(loadElement(instruction.getRightOperand(), varTable));

        switch (instruction.getUnaryOperation().getOpType()) {
            case ADD:
                jasminCode.append("\tiadd\n");
            default:
                jasminCode.append("ERROR\n");
        }

        return jasminCode.toString();
    }

    private String putFieldInstruction(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        return "\tputfield ???\n";
    }

    private String getFieldInstruction(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {
        return loadElement(instruction.getFirstOperand(), varTable) +
                "\tgetfield " + getDescriptor(OllirAccesser.getFieldType(instruction)) +
                " " + ((Operand) instruction.getSecondOperand()).getName() + "\n";
    }

    private String loadElement(Element e, HashMap<String, Descriptor> varTable) {
        if (e.isLiteral())
            return loadLiteral((LiteralElement) e);

        Descriptor d = varTable.get(((Operand) e).getName());
        return loadDescriptor(d);
    }

    private String loadDescriptor(Descriptor descriptor) {
        ElementType t = descriptor.getVarType().getTypeOfElement();
        if (t == ElementType.THIS)
            return "\taload_0\n";

        StringBuilder jasminCode = new StringBuilder();
        if (t == ElementType.INT32)
            jasminCode.append("\tiload_");
        else if (t == ElementType.STRING || t == ElementType.ARRAYREF || t == ElementType.OBJECTREF)
            jasminCode.append("\taload_");
        else
            jasminCode.append("ERROR descriptor not implemented");

        int reg = descriptor.getVirtualReg();
        jasminCode.append(reg).append("\n");

        return jasminCode.toString();
    }

    private String loadLiteral(LiteralElement element) {
        if (element.getType().getTypeOfElement() == ElementType.INT32) {
            if (Integer.parseInt(element.getLiteral()) <= 5)
                return "\ticonst_" + element.getLiteral() + "\n";
            else
                return "\tbipush " + element.getLiteral() + "\n";
        }
        return "\tldc " + element.getLiteral() + "\n";
    }

    private String getDescriptor(Type type) {
        switch (type.getTypeOfElement()) {
            case INT32:
                return "I";
            case ARRAYREF:
                return "[I";
            case VOID:
                return "V";
            case STRING:
                return "Ljava/lang/String;";
            default:
                return "ERROR descriptor not implemented";
        }
    }

}
