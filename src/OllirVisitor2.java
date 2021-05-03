import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OllirVisitor2 extends AJmmVisitor<Boolean, String> {
    SymbolTable symbolTable;
    int tempVar;
    int labelCount = 0;
    int ifCount;
    int paramCount;

    public OllirVisitor2(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        paramCount = symbolTable.getFields().size() + 1;

        addVisit("CLASS_DECLARATION", this::dealWithClass);
        addVisit("VAR_DECLARATION", this::dealWithVar);
        addVisit("MAIN", this::dealWithMain);
        addVisit("METHOD_DECLARATION", this::dealWithMethodDeclaration);
        addVisit("METHOD_BODY", this::dealWithMethodBody);
        addVisit("OBJECT_METHOD", this::dealWithObjectMethod);
        addVisit("ASSIGNMENT", this::dealWithAssignment);
        addVisit("ARRAY_ACCESS", this::dealWithArrayAccess);
        addVisit("RETURN", this::dealWithReturn);
        addVisit("OPERATION", this::dealWithOperation);
        addVisit("LESS", this::dealWithOperation);
        addVisit("AND", this::dealWithOperation);
        addVisit("EXCLAMATION", this::dealWithOperation);
        addVisit("IF", this::dealWithIf);
        addVisit("ELSE", this::dealWithElse);
        addVisit("WHILE", this::dealWithWhile);
        addVisit("IDENTIFIER", this::dealWithIdentifier);
        addVisit("INT", this::dealWithInt);
        addVisit("TRUE", this::dealWithBoolean);
        addVisit("FALSE", this::dealWithBoolean);
        addVisit("METHOD_CALL", this::dealWithMethodCall);
        addVisit("NEW", this::dealWithNew);
        addVisit("ARRAY", this::dealWithArray);
        addVisit("THIS", this::dealWithThis);
        addVisit("OBJECT", this::dealWithObject);
        addVisit("LENGTH", this::dealWithLength);
        setDefaultVisit(this::defaultVisit);
    }


    private String dealWithClass(JmmNode jmmNode, Boolean tempVariable) {
        StringBuilder classStr = new StringBuilder();
        String className = jmmNode.get("name");
        classStr.append(className);
        if (jmmNode.getAttributes().contains("super"))
            classStr.append(" extends ").append(jmmNode.get("super"));
        classStr.append(" {\n");

        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("VAR_DECLARATION"))
                classStr.append("\t").append(visit(child, false)).append("\n");
        }

        classStr.append("\t.construct ").append(className).append("().V {\n")
                .append("\t\tinvokespecial(this, \"<init>\").V;\n")
                .append("\t}\n");

        for (JmmNode child : jmmNode.getChildren()) {
            if (!child.getKind().equals("VAR_DECLARATION"))
                classStr.append("\n").append(visit(child, false));
        }

        classStr.append("}");
        return classStr.toString();
    }

    private String dealWithVar(JmmNode jmmNode, Boolean tempVariable) {
        Optional<JmmNode> ancestor = getAncestor(jmmNode, "METHOD_DECLARATION", "CLASS_DECLARATION");
        if (ancestor.get().getKind().equals("CLASS_DECLARATION") && symbolTable.isGlobal(jmmNode.get("name")))
            return ".field private " + escapeVarName(jmmNode.get("name")) + "." + parseType(symbolTable.getField(jmmNode.get("name")).getType().getName()) + ";\n";

        return "";
    }

    private String dealWithMain(JmmNode jmmNode, Boolean tempVariable) {
        StringBuilder mainStr = new StringBuilder();
        mainStr.append("\t.method public static main(args.array.String).V {\n");

        for (JmmNode child : jmmNode.getChildren()) {
            mainStr.append(visit(child, tempVariable));
        }

        mainStr.append("\t\tret.V;\n\t}\n");

        return mainStr.toString();
    }

    private String dealWithMethodDeclaration(JmmNode jmmNode, Boolean tempVariable) {
        tempVar = 1;

        StringBuilder mainStr = new StringBuilder();
        mainStr.append("\t.method public ").append(jmmNode.get("name")).append("(");

        boolean children = false;

        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("PARAMETER")) {
                if (children) mainStr.append(", ");
                mainStr.append(child.get("name")).append(".").append(parseType(child.get("type")));
                children = true;
            }
        }

        mainStr.append(").").append(parseType(jmmNode.get("return"))).append(" {\n");

        boolean hasReturn = false;
        for (JmmNode child : jmmNode.getChildren()) {
            if (!child.getKind().equals("PARAMETER")) {
                mainStr.append(visit(child, tempVariable));
                if (child.getKind().equals("RETURN"))
                    hasReturn = true;
            }
        }
        mainStr.append("\t").append(hasReturn ? "" : "\tret.V;\n\t").append("}\n");

        return mainStr.toString();
    }

    private String dealWithMethodBody(JmmNode jmmNode, Boolean tempVariable) {
        StringBuilder visitStr = new StringBuilder();
        for (JmmNode child : jmmNode.getChildren()) {
            if (!child.getKind().equals("VAR_DECLARATION"))
                visitStr.append("\t\t");
            visitStr.append(visit(child, tempVariable));
            if (!child.getKind().equals("VAR_DECLARATION"))
                visitStr.append("\n");
        }
        return visitStr.toString();
    }

    private String dealWithWhile(JmmNode jmmNode, Boolean tempVariable) {
        JmmNode condition = jmmNode.getChildren().get(0);
        String condString = visit(condition, true);
        List<String> res = getLastLine(condString);
        int localLabel = labelCount;
        labelCount++;
        StringBuilder expressionString = new StringBuilder("Loop" + localLabel + ":\n");
        condString = this.buildCondition(res.get(1), true);
        expressionString.append("\t\t\t").append(res.get(0));

        expressionString.append("\t\t\tif (").append(condString).append(") goto Body").append(localLabel).append(";\n\t\t\tgoto EndLoop").append(localLabel).append(";\n\t\tBody").append(localLabel).append(":\n");

        for (JmmNode child : jmmNode.getChildren().get(1).getChildren()) {
            expressionString.append("\t\t\t").append(visit(child, false)).append("\n");
        }
        expressionString.append("\t\t\tgoto Loop").append(localLabel).append(";\n\t\tEndLoop").append(localLabel).append(":");
        return expressionString.toString();
    }


    private String dealWithElse(JmmNode jmmNode, Boolean tempVariable) {
        int localLabel = ifCount;
        StringBuilder expressionString = new StringBuilder("\t\telse" + localLabel + ":\n");
        for (JmmNode child : jmmNode.getChildren()) {
            expressionString.append("\t").append(visit(child));
        }
        expressionString.append("\t\tendif").append(localLabel).append(":");
        ifCount--;
        return expressionString.toString();
    }

    private String dealWithIf(JmmNode jmmNode, Boolean tempVariable) {
        JmmNode condition = jmmNode.getChildren().get(0);
        StringBuilder expressionString = new StringBuilder();
        String condString = visit(condition, true);
        List<String> res = getLastLine(condString);
        int localLabel = labelCount;
        ifCount = localLabel;
        labelCount++;
        condString = this.buildCondition(res.get(1), false);
        expressionString.append(res.get(0).isEmpty() ? "" : "\t\t").append(res.get(0));

        expressionString.append("\t\tif (").append(condString).append(") goto else").append(localLabel).append(";\n");

        for (int i = 1; i < jmmNode.getNumChildren(); i++) {
            expressionString.append("\t\t\t").append(visit(jmmNode.getChildren().get(i))).append("\n");
        }
        expressionString.append("\t\t\tgoto endif").append(localLabel).append(";");
        return expressionString.toString();

    }

    private String buildCondition(String condString, boolean isWhile) {

        return isWhile ? condString + " ==.bool 1.bool" : condString + " !.bool 1.bool";
    }

    private boolean containsOps(String condString) {
        return condString.contains("+") || condString.contains("-") || condString.contains("*") || condString.contains("/");
    }

    private String dealWithAssignment(JmmNode jmmNode, Boolean tempVariable) {
        StringBuilder methodStr = new StringBuilder();
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode assignment = jmmNode.getChildren().get(1);
        String identifierVisit = visit(identifier, false);
        String assignmentVisit = visit(assignment, identifierVisit.startsWith("getfield"));
        List<String> res = getLastLine(assignmentVisit);
        String assignmentValue = res.get(1);
        //quando invokestatic é preciso mudar o valor de retorno da funcao
        if (!assignmentValue.substring(assignmentValue.lastIndexOf("."))
                .equals(identifierVisit.substring(identifierVisit.lastIndexOf(".")))) {
            assignmentValue = assignmentValue.substring(0, assignmentValue.lastIndexOf("."))
                    + identifierVisit.substring(identifierVisit.lastIndexOf("."));
        }
        if (identifierVisit.startsWith("getfield"))
            methodStr.append(res.get(0)).append((res.get(0).equals("")) ? "" : "\t\t").append("put").append(identifierVisit.substring(3, identifierVisit.indexOf(")"))).append(", ").append(assignmentValue).append(").V");
        else
            methodStr.append(res.get(0)).append((res.get(0).equals("")) ? "" : "\n\t\t")
                    .append(identifierVisit).append(" :=.").append(assignmentType(identifierVisit, identifier)).append(" ").append(assignmentValue);

        //inicializar object
        if (assignment.getKind().equals("NEW")) {
            if (assignment.getChildren().get(0).getKind().equals("OBJECT"))
                methodStr.append(";\n\t\tinvokespecial(").append(identifierVisit).append(",\"<init>\").V");
        }

        methodStr.append(appendColon(methodStr.toString()));
        return methodStr.toString();
    }

    private List<String> getLastLine(String text) {
        List<String> res = new ArrayList<>();
        if (text.contains("\n")) {
            res.add(text.substring(0, text.lastIndexOf("\n")));
            res.add(text.substring(text.lastIndexOf("\n") + 1));
        } else {
            res.add("");
            res.add(text);
        }

        return res;
    }


    private String assignmentType(String identifierVisit, JmmNode identifier) {
        if (identifier.getKind().equals("ARRAY_ACCESS"))
            return "i32";
        if (identifierVisit.contains("$"))
            identifierVisit = identifierVisit.substring(identifierVisit.indexOf(".") + 1);
        if (identifierVisit.contains(".array"))
            return "array.i32";
        return identifierVisit.substring(identifierVisit.indexOf(".") + 1);
    }

    private String dealWithObjectMethod(JmmNode jmmNode, Boolean tempVariable) {
        StringBuilder methodStr = new StringBuilder();
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode call = jmmNode.getChildren().get(1);
        tempVariable = tempVariable != null && tempVariable;

        String identifierStr = visit(identifier, false);
        String callStr = visit(call, false);
        List<String> lines = getLastLine(callStr);
        String callName = lines.get(1).substring(lines.get(1).indexOf("\"") + 1, lines.get(1).lastIndexOf("\""));

        if (identifier.getKind().equals("NEW")) {
            String type = identifierStr.substring(identifierStr.lastIndexOf("."));
            methodStr.append("t").append(tempVar).append(type)
                    .append(" :=").append(type).append(" ").append(identifierStr).append(";\n");

            identifierStr = "t" + tempVar + type;
            methodStr.append("\t\tinvokespecial(").append(identifierStr).append(",\"<init>\").V;");
        }

        methodStr.append(lines.get(0)).append(lines.get(0).isEmpty() ? "" : "\n");
        String method = "";
        if (callName.equals("length")) {
            if (identifierStr.contains("getfield(")) {
                List<String> res = getLastLine(createTempVar(identifierStr));
                methodStr.append(res.get(0));
                identifierStr = res.get(1);
            }
            method += "arraylength(" + identifierStr + ").i32";
        } else if (isStatic(callName)) {
            if (identifierStr.contains(".")) identifierStr = identifierStr.substring(identifierStr.indexOf(".") + 1);
            method += "invokestatic(" + identifierStr + ", " + lines.get(1) + ").V;";
        } else
            method += "invokevirtual(" + identifierStr + ", " + lines.get(1) + ")." + parseType(symbolTable.getMethod(callName).getType().getName()) + ";";

        method = tempVariable ? createTempVar(method) : method;

        if (method.contains("\n\n")) method = method.replace("\n\n", "\n");

        methodStr.append(method);

        return methodStr.toString();
    }

    private boolean isStatic(String callName) {
        return symbolTable.getMethod(callName) == null;
    }

    private String dealWithOperation(JmmNode jmmNode, Boolean tempVariable) {
        String str = "";
        JmmNode lhs = jmmNode.getChildren().get(0);
        JmmNode rhs = jmmNode.getKind().equals("EXCLAMATION") ? lhs : jmmNode.getChildren().get(1);

        String lhsString = visit(lhs, lhs.getKind().equals("OBJECT_METHOD") || isOp(lhs));
        String rhsString = jmmNode.getKind().equals("EXCLAMATION") ? lhsString : visit(rhs, rhs.getKind().equals("OBJECT_METHOD") || isOp(rhs));

        if (lhsString.startsWith("getfield(")) lhsString = createTempVar(lhsString);
        if (rhsString.startsWith("getfield(")) rhsString = createTempVar(rhsString);

        List<String> lhsResult = getLastLine(lhsString);
        List<String> rhsResult = jmmNode.getKind().equals("EXCLAMATION") ? lhsResult : getLastLine(rhsString);
        str += lhsResult.get(0) + ((lhsResult.get(0).isEmpty()) ? "" : "\n");
        lhsString = lhsResult.get(1);
        str += jmmNode.getKind().equals("EXCLAMATION") ? "" : rhsResult.get(0) + ((rhsResult.get(0).isEmpty()) ? "" : "\n");
        rhsString = rhsResult.get(1);
        String operation = lhsString + " " + getOperator(jmmNode) + (jmmNode.getKind().equals("OPERATION") ? ".i32 " : ".bool ") + rhsString;
        str += tempVariable ? createTempVar(operation) : operation;
        return str;
    }

    private String dealWithReturn(JmmNode jmmNode, Boolean tempVariable) {
        JmmNode identifier = jmmNode.getChildren().get(0);
        String identifierStr = visit(identifier, isOp(identifier) || identifier.getKind().equals("OBJECT_METHOD"));
        String before = "";
        List<String> result = getLastLine(identifierStr);
        if (result.get(1).contains("getfield"))
            result = getLastLine(createTempVar(identifierStr));

        before = (result.get(0).isEmpty() ? "" : "\t\t") + result.get(0);
        identifierStr = result.get(1);
        String type = identifierStr.contains("array.i32") ? ".array.i32" : identifierStr.substring(identifierStr.lastIndexOf("."));
        return before + "\t\tret" + type + " " + identifierStr + appendColon(identifierStr) + "\n";
    }

    private String dealWithArrayAccess(JmmNode jmmNode, Boolean tempVariable) {
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode access = jmmNode.getChildren().get(1);

        String identifierVisit = visit(identifier, false);
        String accessVisit = visit(access, !access.getKind().equals("IDENTIFIER"));
        String before = "";
        if (identifierVisit.startsWith("getfield")) {
            before += "t" + tempVar + ".array.i32 :=.array.i32 " + identifierVisit + ";\n";
            identifierVisit = "\t\tt" + tempVar;
            tempVar++;
        } else
            identifierVisit = identifierVisit.substring(0, identifierVisit.indexOf(".array.i32"));

        List<String> result = getLastLine(accessVisit);
        before += (result.get(0).isEmpty() ? "" : "\n\t\t") + result.get(0);
        accessVisit = result.get(1);
        if (accessVisit.startsWith("getfield") || accessVisit.contains("[")) {
            before += "\t\tt" + tempVar + ".i32 :=.i32 " + accessVisit + ";\n";
            accessVisit = "t" + tempVar + ".i32";
            tempVar++;
        }
        return before + identifierVisit + "[" + accessVisit + "].i32";
    }

    private String dealWithNew(JmmNode jmmNode, Boolean tempVariable) {
        return tempVariable ? createTempVar(visit(jmmNode.getChildren().get(0), false)) : visit(jmmNode.getChildren().get(0), false);
    }

    private String dealWithObject(JmmNode jmmNode, Boolean tempVariable) {
        return "new(" + jmmNode.get("name") + ")." + jmmNode.get("name");
    }

    private String dealWithMethodCall(JmmNode jmmNode, Boolean tempVariable) {
        StringBuilder before = new StringBuilder();
        String callName = "\"" + jmmNode.get("name") + "\"";
        StringBuilder str = new StringBuilder(before + callName);
        for (JmmNode child : jmmNode.getChildren()) {
            String childVisit = visit(child, !child.getKind().equals("INT") && !child.getKind().equals("IDENTIFIER"));
            List<String> result = getLastLine(childVisit);
            before.append(result.get(0) + "\n");
            if (child.getKind().equals("ARRAY_ACCESS") || childVisit.startsWith("getfield(")) {
                childVisit = createTempVar(result.get(1).trim());
                result = getLastLine(childVisit);
                before.append(result.get(0) + "\n");
            }
            str.append(", ").append(result.get(1).trim());
        }
        return before.toString() + str;
    }

    private String dealWithIdentifier(JmmNode jmmNode, Boolean tempVariable) {
        Optional<JmmNode> ancestor = getAncestor(jmmNode, "MAIN", "METHOD_DECLARATION");
        Symbol var = symbolTable.getVariable(jmmNode.get("name"), ancestor.get().get("name"));
        if (var == null) return jmmNode.get("name");
        String varName = escapeVarName(var.getName());
        String varType = parseType(var.getType().getName());
        String before = "";
        if (symbolTable.isGlobal(var.getName()))
            return "getfield(this, " + varName + "." + varType + ")." + varType;
        else if (symbolTable.isParam(ancestor.get().get("name"), var.getName()))
            before += "$" + paramCount + ".";
        return before + varName + "." + varType;
    }

    private String dealWithArray(JmmNode jmmNode, Boolean tempVariable) {
        String str = "";
        JmmNode child = jmmNode.getChildren().get(0);
        String childVisit = "";
        if (child.getKind().equals("IDENTIFIER") || child.getKind().equals("OBJECT_METHOD")) {
            childVisit = visit(child, true);

        } else {
            childVisit = visit(child, false);
        }
        List<String> result = getLastLine(childVisit);
        str += result.get(0) + (result.get(0).isEmpty() ? "" : "\n");
        str += "new(array, " + result.get(1) + ").array.i32";

        return str;
    }

    private String dealWithInt(JmmNode jmmNode, Boolean tempVariable) {
        String ret = jmmNode.get("value") + ".i32";
        return tempVariable ? createTempVar(ret) : ret;
    }

    private String dealWithBoolean(JmmNode jmmNode, Boolean tempVariable) {
        String ret = parseType(jmmNode.getKind());
        return tempVariable ? createTempVar(ret) : ret;
    }

    private String dealWithLength(JmmNode jmmNode, Boolean tempVariable) {
        return "\"length\"";
    }

    private String dealWithThis(JmmNode jmmNode, Boolean tempVariable) {
        return "this";
    }

    private String createTempVar(String text) {
        String str = "";
        String opType;
        if (text.contains("arraylength("))
            opType = ".i32";
        else if (text.contains("new(array"))
            opType = ".array.i32";
        else if (text.contains("getfield") || text.contains("invokevirtual"))
            opType = text.substring(text.lastIndexOf(").") + 1);
        else if (text.contains(").") || text.contains("]."))
            opType = text.substring(text.lastIndexOf("."));
        else if (text.contains("<") || text.contains("&&") || text.contains("!"))
            opType = ".bool";
        else if (containsOps(text))
            opType = ".i32";
        else if (text.contains("$"))
            opType = text.substring(text.indexOf(".") + 1).substring(text.lastIndexOf("."));
        else if (text.contains("array.i32"))
            opType = ".array.i32";
        else
            opType = text.substring(text.lastIndexOf("."));

        if (opType.contains(";")) opType = opType.substring(0, opType.length() - 1);
        if (text.contains("\n")) {
            str += text.substring(0, text.lastIndexOf("\n"));
            text = text.substring(text.lastIndexOf("\n"));
        }
        str += "t" + tempVar + opType + " :=" + opType + " " + text;
        str += appendColon(str);
        str += "\n\nt" + tempVar + opType;
        tempVar++;

        return str;
    }

    private String appendColon(String text) {
        if (text.charAt(text.length() - 1) != ';') return ";";
        return "";
    }

    private boolean isOp(JmmNode jmmNode) {
        return jmmNode.getKind().equals("OPERATION") || jmmNode.getKind().equals("LESS") || jmmNode.getKind().equals("AND") || jmmNode.getKind().equals("EXCLAMATION");
    }

    private boolean booleanOp(JmmNode jmmNode) {
        return jmmNode.getKind().equals("LESS") || jmmNode.getKind().equals("AND") || jmmNode.getKind().equals("EXCLAMATION");
    }

    private String getOperator(JmmNode jmmNode) {
        if (jmmNode.getKind().equals("LESS"))
            return " <";
        if (jmmNode.getKind().equals("AND"))
            return " &&";
        if (jmmNode.getKind().equals("EXCLAMATION"))
            return " !";
        return jmmNode.get("op");
    }

    private Optional<JmmNode> getAncestor(JmmNode jmmNode, String specificScope, String globalScope) {
        return jmmNode.getAncestor(specificScope).isPresent() ? jmmNode.getAncestor(specificScope) : jmmNode.getAncestor(globalScope);
    }

    private String parseType(String type) {
        switch (type) {
            case "int array":
                return "array.i32";
            case "int":
                return "i32";
            case "void":
                return "V";
            case "FALSE":
                return "0.bool";
            case "TRUE":
                return "1.bool";
            case "boolean":
                return "bool";
        }
        return type;
    }

    private String escapeVarName(String varName) {
        if (varName.charAt(0) == '$')
            return "d_" + varName.substring(1);
        else if (varName.charAt(0) == '_')
            return "u_" + varName.substring(1);
        else if (varName.startsWith("ret"))
            return "r_" + varName.substring(3);
        else if (varName.startsWith("array"))
            return "arr_" + varName.substring(5);
        else if (varName.startsWith("field"))
            return "f_" + varName.substring(5);
        return varName;
    }

    private String defaultVisit(JmmNode jmmNode, Boolean tempVariable) {
        StringBuilder visitStr = new StringBuilder();
        for (JmmNode child : jmmNode.getChildren()) {
            visitStr.append(visit(child, tempVariable));
        }
        return visitStr.toString();
    }


}
