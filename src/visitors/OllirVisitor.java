package visitors;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import semantic.Method;
import semantic.SymbolTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OllirVisitor extends AJmmVisitor<List<Report>, String> {
    SymbolTable symbolTable;
    String ollirStr;
    int tempVar;
    int labelCount = 0;
    int ifCount;
    int paramCount;
    boolean optimize;
    ArrayList<Integer> skipTemps = new ArrayList<>();

    public OllirVisitor(SymbolTable symbolTable, boolean optimize) {
        this.optimize = optimize;
        this.symbolTable = symbolTable;
        this.ollirStr = "";
        paramCount = symbolTable.getFields().size() + 1;

        ArrayList<Symbol> symbols = new ArrayList<>(symbolTable.getFields());
        for (String method : symbolTable.getMethods()) {
            symbols.addAll(symbolTable.getMethod(method).getLocalVariables());
            symbols.addAll(symbolTable.getMethod(method).getParameters());
        }

        Pattern p = Pattern.compile("t\\d+");
        Matcher m;
        for (Symbol symbol : symbols) {
            m = p.matcher(symbol.getName());
            if (m.find())
                skipTemps.add(Integer.parseInt(symbol.getName().substring(1)));
        }

        tempVar = 1;
        while (skipTemps.contains(tempVar))
            tempVar++;

        addVisit("CLASS_DECLARATION", this::dealWithClass);
        addVisit("VAR_DECLARATION", this::dealWithVar);
        addVisit("MAIN", this::dealWithMain);
        addVisit("METHOD_DECLARATION", this::dealWithMethodDeclaration);
        addVisit("OBJECT_METHOD", this::dealWithObjectMethod);
        addVisit("ASSIGNMENT", this::dealWithAssignment);
        addVisit("RETURN", this::dealWithReturn);
        addVisit("IDENTIFIER", this::dealWithIdentifier);
        addVisit("INT", this::dealWithInt);
        addVisit("TRUE", this::dealWithBoolean);
        addVisit("FALSE", this::dealWithBoolean);
        addVisit("NEW", this::dealWithNew);
        addVisit("OPERATION", this::dealWithOperation);
        addVisit("LESS", this::dealWithOperation);
        addVisit("AND", this::dealWithOperation);
        addVisit("EXCLAMATION", this::dealWithOperation);
        addVisit("IF", this::dealWithIf);
        addVisit("ELSE", this::dealWithElse);
        addVisit("WHILE", this::dealWithWhile);
        addVisit("ARRAY_ACCESS", this::dealWithArrayAccess);
        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithVar(JmmNode jmmNode, List<Report> reports) {
        Optional<JmmNode> ancestor = getAncestor(jmmNode, "METHOD_DECLARATION", "CLASS_DECLARATION");
        if (ancestor.get().getKind().equals("CLASS_DECLARATION") && symbolTable.isGlobal(jmmNode.get("name"))) {
            return ".field private " + escapeVarName(jmmNode.get("name")) + "." +
                    parseType(symbolTable.getField(jmmNode.get("name")).getType().isArray() ? symbolTable.getField(jmmNode.get("name")).getType().getName() + " array"
                            : (symbolTable.getField(jmmNode.get("name")).getType().getName())) + ";\n";
        }

        return "";
    }

    private String dealWithWhile(JmmNode jmmNode, List<Report> reports) {
        JmmNode condition = jmmNode.getChildren().get(0);
        String condString = visit(condition);
        int localLabel = labelCount;
        labelCount++;
        StringBuilder expressionString;
        List<String> result = this.buildCondition(condition, condString, true);
        if (!optimize) {
            expressionString = new StringBuilder("\t\tLoop" + localLabel + ":\n");
            expressionString.append(result.get(0));
            condString = result.get(1);

            expressionString.append("\t\t\tif (").append(condString).append(") goto Body").append(localLabel).append(";\n\t\t\tgoto EndLoop").append(localLabel).append(";\n\t\tBody").append(localLabel).append(":\n");

            for (JmmNode child : jmmNode.getChildren().get(1).getChildren()) {
                expressionString.append("\t").append(visit(child));
            }
            expressionString.append("\t\t\tgoto Loop").append(localLabel).append(";\n\t\tEndLoop").append(localLabel).append(":\n");
        } else {
            expressionString = new StringBuilder("\t\tgoto Condition" + localLabel + ";\n");
            condString = result.get(1);

            expressionString.append("\t\tLoop").append(localLabel).append(":\n");

            for (JmmNode child : jmmNode.getChildren().get(1).getChildren()) {
                expressionString.append("\t").append(visit(child));
            }
            expressionString.append("\t\tCondition").append(localLabel).append(":\n").append(result.get(0)).append("\t\t\tif (").append(condString).append(") goto Loop").append(localLabel).append(";\n");
        }

        return expressionString.toString();
    }


    private String dealWithElse(JmmNode jmmNode, List<Report> reports) {
        int localLabel = ifCount;
        StringBuilder expressionString = new StringBuilder("\t\telse" + localLabel + ":\n");
        for (JmmNode child : jmmNode.getChildren()) {
            expressionString.append("\t").append(visit(child));
        }
        expressionString.append("\t\tendif").append(localLabel).append(":\n");
        ifCount--;
        return expressionString.toString();
    }

    private String dealWithIf(JmmNode jmmNode, List<Report> reports) {
        JmmNode condition = jmmNode.getChildren().get(0);
        String condString = visit(condition);
        StringBuilder expressionString = new StringBuilder();
        int localLabel = labelCount;
        ifCount = localLabel;
        labelCount++;
        List<String> result = this.buildCondition(condition, condString, false);
        expressionString.append(result.get(0));
        condString = result.get(1);

        expressionString.append("\t\tif (").append(condString).append(") goto else").append(localLabel).append(";\n");

        for (int i = 1; i < jmmNode.getNumChildren(); i++) {
            expressionString.append("\t").append(visit(jmmNode.getChildren().get(i)));
        }
        expressionString.append("\t\t\tgoto endif").append(localLabel).append(";\n");
        return expressionString.toString();

    }

    private boolean containsOps(String condString) {
        return condString.contains("+") || condString.contains("-") || condString.contains("*") || condString.contains("/") || condString.contains("<") || condString.contains("&&");
    }

    private String parseVarName(String var) {
        var = var.substring(0, var.lastIndexOf("."));
        if (var.contains(".array")) {
            var = var.substring(0, var.lastIndexOf("."));
        }
        return var;

    }

    private String dealWithArrayAccess(JmmNode jmmNode, List<Report> reports) {
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode access = jmmNode.getChildren().get(1);
        String identifierString = visit(identifier);
        identifierString = parseVarName(identifierString);
        String accessString = visit(access);
        String ret = "";
        String before = "";
        if (access.getKind().equals("INT")) {
            before += "\t\tt" + tempVar + ".i32 :=.i32 " + accessString + ";\n";
            accessString = "t" + tempVar + ".i32";
            tempVar++;
            while (skipTemps.contains(tempVar))
                tempVar++;
        } else {
            String substring = accessString.substring(accessString.lastIndexOf("."), accessString.length() - 1);
            if (accessString.contains("\n")) {
                if (accessString.lastIndexOf("\n") > accessString.lastIndexOf(":=.")) {
                    String lastLine = accessString.substring(accessString.lastIndexOf("\n") + 1);
                    if (!lastLine.contains(";")) lastLine += ";";
                    before += "\t\t" + accessString.substring(0, accessString.lastIndexOf("\n") + 1) + "\t\tt" + tempVar + substring + " :=" + substring + " " + lastLine + "\n";
                    accessString = "\tt" + tempVar + substring;
                    tempVar++;
                    while (skipTemps.contains(tempVar))
                        tempVar++;
                } else
                    accessString = accessString.substring(accessString.lastIndexOf("\n"), accessString.lastIndexOf(":=."));
            } else if (accessString.contains(":=.")) {
                before = accessString + "\n";
                accessString = accessString.substring(0, accessString.indexOf(" "));
            } else {
                if (!identifier.getKind().equals("IDENTIFIER")) {
                    before += "\t\tt" + tempVar + substring + " :=" + substring + " " + accessString + "\n";
                    accessString = "t" + tempVar + substring;
                    tempVar++;
                    while (skipTemps.contains(tempVar))
                        tempVar++;
                }
            }
        }

        if (!identifierString.contains("\n")) {
            ret += before + "\t\tt" + tempVar + ".i32 :=.i32 " + identifierString + "[" + accessString + "].i32;";
            tempVar++;
            while (skipTemps.contains(tempVar))
                tempVar++;
        } else {
            ret += before + identifierString + "[" + accessString + "].i32;";
        }

        return ret;
    }

    private String dealWithOperation(JmmNode jmmNode, List<Report> reports) {
        String str = "";
        JmmNode lhs = jmmNode.getChildren().get(0);
        JmmNode rhs = jmmNode.getKind().equals("EXCLAMATION") ? lhs : jmmNode.getChildren().get(1);
        String lhsString = visit(lhs);
        String rhsString = jmmNode.getKind().equals("EXCLAMATION") ? lhsString : visit(rhs);
        List<String> lhsResult = checkForNested(lhs, lhsString);
        List<String> rhsResult = jmmNode.getKind().equals("EXCLAMATION") ? lhsResult : checkForNested(rhs, rhsString);
        str += !lhsResult.get(0).contains("\n") ? lhsResult.get(0) + "\n" : lhsResult.get(0);
        lhsString = lhsResult.get(1);
        str += jmmNode.getKind().equals("EXCLAMATION") ? "" : !rhsResult.get(0).contains("\n") ? rhsResult.get(0) + "\n" : rhsResult.get(0);
        rhsString = rhsResult.get(1);

        str += lhsString + " " + getOperator(jmmNode);
        if (jmmNode.getKind().equals("OPERATION"))
            str += ".i32 ";
        else
            str += ".bool ";
        str += rhsString + ";";
        return str;
    }

    private String dealWithReturn(JmmNode jmmNode, List<Report> reports) {
        StringBuilder methodStr = new StringBuilder();
        JmmNode identifier = jmmNode.getChildren().get(0);
        String child = visit(identifier);
        if (identifier.getKind().equals("OBJECT_METHOD") || isOp(identifier)) {
            String substring = child.substring(child.lastIndexOf("."), child.length() - 1);
            if (child.contains("\n")) {
                if (child.lastIndexOf("\n") > child.lastIndexOf(":=.")) {
                    methodStr.append("\t\t").append(child.substring(0, child.lastIndexOf("\n") + 1)).append("\t\tt").append(tempVar).append(substring).append(" :=").append(substring).append(" ").append(child.substring(child.lastIndexOf("\n") + 1)).append("\n");
                    child = "\tt" + tempVar + substring;
                    tempVar++;
                    while (skipTemps.contains(tempVar))
                        tempVar++;
                } else
                    child = child.substring(child.lastIndexOf("\n"), child.lastIndexOf(":=."));
            } else {
                methodStr.append("\t\tt").append(tempVar).append(substring).append(" :=").append(substring).append(" ").append(child).append("\n");
                child = "t" + tempVar + substring;
                tempVar++;
                while (skipTemps.contains(tempVar))
                    tempVar++;
            }
        }
        if (child.contains("\n")) {
            methodStr.append(child.substring(0, child.lastIndexOf("\n")));
            child = child.substring(child.lastIndexOf("\n") + 1);
        }
        methodStr.append("\t\t" + "ret");
        if (!child.contains("$"))
            methodStr.append(child.substring(child.indexOf("."))).append(" ").append(child).append(";\n");
        else {
            String subChild = child.substring(child.indexOf(".") + 1);
            methodStr.append(subChild.substring(subChild.indexOf("."))).append(" ").append(child).append(";\n");
        }

        return methodStr.toString();
    }

    private String dealWithAssignment(JmmNode jmmNode, List<Report> reports) {
        StringBuilder methodStr = new StringBuilder();
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode assignment = jmmNode.getChildren().get(1);
        Optional<JmmNode> ancestor = getAncestor(jmmNode, "MAIN", "METHOD_DECLARATION");
        Symbol var;
        boolean arrayAccess = false;
        if (identifier.getKind().equals("ARRAY_ACCESS")) {
            arrayAccess = true;
            var = symbolTable.getVariable(identifier.getChildren().get(0).get("name"), ancestor.get().get("name"));
            //var.setType(new Type("int", false));
        } else
            var = symbolTable.getVariable(identifier.get("name"), ancestor.get().get("name"));
        if (isOp(assignment)) {
            String operation = visit(assignment);
            String[] lines = operation.split("\n");
            for (int i = 0; i < lines.length; i++) {
                if (i == lines.length - 1) {
                    methodStr.append(varAssign(identifier, var, arrayAccess));

                    methodStr.append(lines[i]).append("\n");
                } else
                    methodStr.append("\t\t").append(lines[i]).append("\n");
            }
        } else if (assignment.getKind().equals("NEW")) {
            String assignmentString = visit(assignment);
            if (assignmentString.contains("\n")) {
                methodStr.append(assignmentString, 0, assignmentString.indexOf("\n"));
                assignmentString = assignmentString.substring(assignmentString.indexOf("\n") + 1);
            }
            methodStr.append(varAssign(identifier, var, arrayAccess));
            methodStr.append(assignmentString).append(";\n");
            if (!assignment.getChildren().get(0).getKind().equals("ARRAY"))
                methodStr.append("\t\tinvokespecial(").append(escapeVarName(var.getName())).append(".").append(parseType(var.getType().isArray() ? var.getType().getName() + " array" : var.getType().getName()))
                        .append(", \"<init>\").V;\n");
        } else {
            String assignString = visit(assignment);
            if (assignment.getKind().equals("ARRAY_ACCESS")) {
                String before;
                if (assignString.contains("\n")) {
                    before = assignString.substring(0, assignString.lastIndexOf("\n"));
                    if (assignString.lastIndexOf("\n") < assignString.lastIndexOf(":=.")) {
                        before += assignString.substring(assignString.lastIndexOf("\n"));
                        assignString = assignString.substring(assignString.lastIndexOf("\n") + 1, assignString.lastIndexOf(" :=."));
                    } else
                        assignString = assignString.substring(assignString.lastIndexOf("\n") + 1, assignString.lastIndexOf(";"));
                } else {
                    before = assignString;
                    assignString = assignString.substring(0, assignString.indexOf(' '));
                }
                methodStr.append(before).append("\n");
            } else if (assignment.getKind().equals("OBJECT_METHOD")) {
                if (assignString.contains("\n")) {
                    methodStr.append(assignString, 0, assignString.lastIndexOf("\n"));
                    assignString = assignString.substring(assignString.lastIndexOf("\n") + 1);
                } else if (assignString.contains(":=.")) {
                    methodStr.append(assignString);
                    assignString = assignString.substring(0, assignString.indexOf(" ")) + ";";
                }

            }
            if (assignString.contains("\n")) {
                methodStr.append(assignString.substring(0, assignString.lastIndexOf("\n") + 1));
                assignString = assignString.substring(assignString.lastIndexOf("\n") + 1);
            }

            methodStr.append(varAssign(identifier, var, arrayAccess));
            methodStr.append(assignString);

            if (!assignment.getKind().equals("OBJECT_METHOD"))
                methodStr.append(";");
            methodStr.append("\n");
        }
        /*
        if(identifier.getKind().equals("ARRAY_ACCESS"))
            var.setType(new Type("int", true));*/


        return methodStr.toString();
    }

    private String dealWithObjectMethod(JmmNode jmmNode, List<Report> reports) {
        StringBuilder methodStr = new StringBuilder();
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode call = jmmNode.getChildren().get(1);

        boolean grandchildren = false;
        List<String> params = new ArrayList<>();
        for (JmmNode grandchild : call.getChildren()) {
            grandchildren = true;
            String visitString;
            String param;
            visitString = visit(grandchild);
            if (isOp(grandchild)) {
                if (visitString.contains("\n")) {
                    if (visitString.lastIndexOf("\n") > visitString.lastIndexOf(":=.")) {
                        String substring = visitString.substring(visitString.lastIndexOf("."), visitString.length() - 1);
                        visitString = "\t\t" + visitString.substring(0, visitString.lastIndexOf("\n") + 1) + "\t\tt" + tempVar + substring + " :=" + substring + " " + visitString.substring(visitString.lastIndexOf("\n") + 1) + "\n";
                        param = "t" + tempVar + substring;
                        tempVar++;
                        while (skipTemps.contains(tempVar))
                            tempVar++;
                    } else
                        param = visitString.substring(visitString.lastIndexOf("\n"), visitString.lastIndexOf(":=."));
                } else {
                    String substring = visitString.substring(visitString.lastIndexOf("."), visitString.length() - 1);
                    visitString = "\t\tt" + tempVar + substring + " :=" + substring + " " + visitString + "\n";
                    param = "t" + tempVar + substring;
                    tempVar++;
                    while (skipTemps.contains(tempVar))
                        tempVar++;
                }

            } else if (grandchild.getKind().equals("OBJECT_METHOD")) {
                String substring = visitString.substring(visitString.contains(".array") ? visitString.indexOf(".array") : visitString.lastIndexOf("."), visitString.length() - 1);
                visitString = "\t\tt" + tempVar + substring + " :=" + substring + " " + visitString + "\n";
                param = "t" + tempVar + substring;
                tempVar++;
                while (skipTemps.contains(tempVar))
                    tempVar++;
            } else if (grandchild.getKind().equals("ARRAY_ACCESS")) {
                if (!visitString.contains("\n"))
                    param = visitString.substring(0, visitString.indexOf(" "));
                else {
                    String sub = visitString.substring(visitString.lastIndexOf("\n") + 1);
                    visitString = visitString.substring(0, visitString.lastIndexOf("\n") + 1);
                    param = sub.contains(":=.") ? sub.substring(0, sub.indexOf(" ")) : sub.substring(0, sub.length() - 1);
                    if (param.contains("[")) {
                        visitString += "\t\tt" + tempVar + ".i32 :=.i32 " + param + ";\n";
                        param = "t" + tempVar + ".i32";
                        tempVar++;
                        while (skipTemps.contains(tempVar))
                            tempVar++;
                    }

                }
            } else {
                if (!visitString.contains(":=.")) {
                    param = visitString;
                    visitString = "";
                } else {
                    param = visitString.substring(0, visitString.indexOf(":=."));
                    visitString = visitString.substring(0, visitString.indexOf(";") + 1) + "\n";
                }

            }

            params.add(param);
            methodStr.append(visitString);
        }

        if (identifier.getKind().equals("IDENTIFIER") && symbolTable.checkVariableInImports(identifier.get("name"))) {
            methodStr.append("\t\tinvokestatic(").append(identifier.get("name")).append(", \"").append(call.get("name")).append("\"");
            methodStr.append(this.buildMethodType(jmmNode, "", params, grandchildren));

        } else if (identifier.getKind().equals("NEW")) {
            String newVisit = visit(identifier);
            String subString = newVisit.substring(newVisit.lastIndexOf("."));
            methodStr.append("\t\tt").append(tempVar).append(subString).append(" :=").append(subString).append(" ").append(newVisit).append(";\n");
            methodStr.append("\t\tinvokespecial(").append("t" + tempVar + subString).append(", \"<init>\").V;\n");
            methodStr.append(classIsImported(identifier.getChildren().get(0))).append("t").append(tempVar).append(subString).append(", \"").append(call.get("name")).append("\"");
            methodStr.append(this.buildMethodType(jmmNode, call.get("name"), params, grandchildren));
            tempVar++;
            while (skipTemps.contains(tempVar))
                tempVar++;
        } else {
            String varName = this.getVarName(identifier);
            String callName = !call.getKind().equals("LENGTH") ? call.get("name") : "length";
            String type = !varName.equalsIgnoreCase("this") ? varName + "." : "";

            if (callName.equals("length")) {
                methodStr.append("t").append(tempVar).append(".i32 :=.i32 arraylength(").append(type).append("array.i32).i32");
                tempVar++;
                while (skipTemps.contains(tempVar))
                    tempVar++;
            } else {
                methodStr.append("invokevirtual(").append(type).append(getVariableType(varName, jmmNode)).append(", \"").append(callName).append("\"");
                methodStr.append(this.buildMethodType(jmmNode, callName, params, grandchildren));
            }
        }
        methodStr.append(";");
        return methodStr.toString();
    }

    private String dealWithMain(JmmNode jmmNode, List<Report> reports) {
        StringBuilder mainStr = new StringBuilder();
        mainStr.append("\n\t.method public static main(args.array.String).V {\n");

        for (JmmNode child : jmmNode.getChildren()) {
            mainStr.append(visit(child, reports));
        }

        mainStr.append("\t\tret.V;\n\t}\n");

        return mainStr.toString();
    }

    private String dealWithMethodDeclaration(JmmNode jmmNode, List<Report> reports) {
        StringBuilder mainStr = new StringBuilder();
        mainStr.append("\t.method public ").append(jmmNode.get("name")).append("(");
        boolean children = false;
        boolean hasReturn = false;
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("PARAMETER")) {
                children = true;
                mainStr.append(child.get("name")).append(".").append(parseType(child.get("type"))).append(", ");
            } else if (child.getKind().equals("RETURN"))
                hasReturn = true;
        }
        if (children)
            mainStr.delete(mainStr.length() - 2, mainStr.length());
        mainStr.append(").").append(parseType(jmmNode.get("return"))).append("{\n");
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("PARAMETER")) continue;
            mainStr.append(visit(child, reports));
        }
        String returnString = hasReturn ? "" : "\tret.V;\n\t";
        mainStr.append("\t").append(returnString).append("}\n");

        return mainStr.toString();
    }

    private String dealWithIdentifier(JmmNode jmmNode, List<Report> reports) {
        Optional<JmmNode> ancestor = getAncestor(jmmNode, "MAIN", "METHOD_DECLARATION");
        Symbol var = symbolTable.getVariable(jmmNode.get("name"), ancestor.get().get("name"));
        String varName = escapeVarName(var.getName());
        String varType = parseType(var.getType().isArray() ? var.getType().getName() + " array" : var.getType().getName());
        String before = "";
        if (symbolTable.isGlobal(var.getName())) {
            before += "\t\tt" + tempVar + "." + varType + " :=." + varType + " getfield(this, " + varName + "." + varType + ")." + varType + ";\n";
            varName = "t" + tempVar;
            tempVar++;
            while (skipTemps.contains(tempVar))
                tempVar++;
        } else if (symbolTable.isParam(ancestor.get().get("name"), var.getName())) {
            before += "$" + (symbolTable.getMethod(ancestor.get().get("name")).getParamNumber(var.getName()) + 1) + ".";
        }
        return before + varName + "." + varType;
    }

    private String dealWithInt(JmmNode jmmNode, List<Report> reports) {
        return jmmNode.get("value") + ".i32";
    }

    private String dealWithBoolean(JmmNode jmmNode, List<Report> reports) {
        return parseType(jmmNode.getKind());
    }

    private String dealWithNew(JmmNode jmmNode, List<Report> reports) {
        String str = "";
        JmmNode child = jmmNode.getChildren().get(0);
        if (child.getKind().equals("ARRAY")) {
            JmmNode grandchild = child.getChildren().get(0);
            String grandchildVisit = visit(grandchild);
            if (grandchild.getKind().equals("OBJECT_METHOD")) {
                str += grandchildVisit + "\n";
                grandchildVisit = grandchildVisit.substring(0, grandchildVisit.indexOf(" "));
            }
            str += "new(array, " + grandchildVisit + ").array.i32";
        } else if (child.getKind().equals("IDENTIFIER")) {
            str += "new(" + child.get("name") + ")." + child.get("name");
        } else {
            str += "new(" + child.get("name") + ")." + child.get("name");
        }
        return str;
    }

    private String buildMethodType(JmmNode jmmNode, String callName, List<String> params, boolean grandchildren) {
        StringBuilder ret = new StringBuilder();
        if (params.size() > 0)
            ret.append(", ");
        for (String param : params) {
            ret.append(param).append(", ");
        }
        if (grandchildren)
            ret = new StringBuilder(ret.substring(0, ret.length() - 2));

        Method method = symbolTable.getMethod(callName);
        String methodType = ".V";
        JmmNode ancestor = jmmNode.getParent();

        if (method == null) {
            if (ancestor.getKind().equals("ASSIGNMENT")) {
                String var = visit(ancestor.getChildren().get(0));
                methodType = var.contains(";") ? var.substring(var.indexOf("."), var.indexOf(" ")) : var.substring(var.lastIndexOf("."));
            } else if (ancestor.getKind().equals("IF") || ancestor.getKind().equals("WHILE")) {
                methodType = ".bool";
            }
        } else
            methodType = "." + parseType(method.getType().isArray() ? method.getType().getName() + " array" : method.getType().getName());
        ret.append(")").append(methodType);
        return ret.toString();
    }

    private String classIsImported(JmmNode object) {
        if (symbolTable.checkVariableInImports(object.get("name")) || (symbolTable.getSuper() != null && symbolTable.getSuper().equals(object.get("name"))))
            return "\t\tinvokestatic(";
        return "\t\tinvokevirtual(";
    }

    private String getVarName(JmmNode identifier) {
        if (identifier.getKind().equals("THIS"))
            return "this";
        if (identifier.getKind().equals("NEW"))
            return identifier.getChildren().get(0).get("name");
        return identifier.get("name");
    }

    private String getVariableType(String varName, JmmNode currentNode) {
        if (varName.equalsIgnoreCase("this"))
            return varName;
        if (varName.equals(symbolTable.getClassName()))
            return varName;
        Optional<JmmNode> ancestor = getAncestor(currentNode, "MAIN", "METHOD_DECLARATION");
        return symbolTable.getVariable(varName, ancestor.get().get("name")).getType().isArray() ?
                symbolTable.getVariable(varName, ancestor.get().get("name")).getType().getName() + " array"
                : symbolTable.getVariable(varName, ancestor.get().get("name")).getType().getName();

    }

    private String varAssign(JmmNode node, Symbol var, boolean arrayAccess) {

        if (!arrayAccess)
            return "\t\t" + escapeVarName(var.getName()) + "." + parseType(var.getType().isArray() ? var.getType().getName() + " array" : var.getType().getName()) + " :=." +
                    parseType(var.getType().isArray() ? var.getType().getName() + " array" : var.getType().getName()) + " ";

        String access = visit(node);
        String before = "";
        String ret = "";
        if (access.contains("\n")) {
            before = access.substring(0, access.lastIndexOf("\n"));
            access = access.substring(access.lastIndexOf("\n"));
        }
        if (access.contains(":=."))
            access = access.substring(access.lastIndexOf(" "), access.lastIndexOf(";"));
        if (access.contains(";"))
            access = access.substring(0, access.lastIndexOf(";"));
        ret += before + "\t\t" + access + " :=.i32 ";
        return ret;
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
            return "fld_" + varName.substring(5);
        return varName;
    }


    private Optional<JmmNode> getAncestor(JmmNode jmmNode, String globalScope, String specificScope) {
        return jmmNode.getAncestor(globalScope).isPresent() ? jmmNode.getAncestor(globalScope) : jmmNode.getAncestor(specificScope);
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

    private List<String> buildCondition(JmmNode condition, String condString, boolean isWhile) {
        String returnString = "";
        List<String> result = new ArrayList<>();
        if (condition.getKind().equals("OBJECT_METHOD")) {
            if (isWhile) {
                returnString += "\t\tt" + tempVar + ".bool :=.bool " + condString + "\n";
                condString = "t" + tempVar + ".bool ==.bool t" + tempVar + ".bool;";
            } else {
                if (!condString.contains("\n"))
                    returnString += "\t\tt" + tempVar + ".bool :=.bool " + condString + "\n";
                else {
                    returnString += condString.substring(0, condString.lastIndexOf("\n") + 1) + "\n\t\tt" + tempVar + ".bool :=.bool " + condString.substring(condString.lastIndexOf("\n")) + "\n";
                }
            }

            condString = "t" + tempVar + ".bool !.bool t" + tempVar + ".bool;";
            tempVar++;
            while (skipTemps.contains(tempVar))
                tempVar++;
        } else if (condition.getKind().equals("IDENTIFIER") || condition.getKind().equals("TRUE") || condition.getKind().equals("FALSE")) {
            if (isWhile)
                condString += " ==.bool " + condString;
            else
                condString += " !.bool " + condString;
        } else if (condition.getKind().equals("ARRAY_ACCESS")) {
            returnString += condString + "\n";
            condString = condString.substring(condString.lastIndexOf("\n") + 1, condString.lastIndexOf(":=."));
        }
        if (condString.contains("\n")) {
            returnString += condString.substring(0, condString.lastIndexOf("\n"));
            returnString += "t" + tempVar + ".bool :=.bool " + condString.substring(condString.lastIndexOf("\n") + 1) + "\n";
            condString = isWhile ? "t" + tempVar + ".bool ==.bool t" + tempVar + ".bool;" : "t" + tempVar + ".bool !.bool t" + tempVar + ".bool;";
            tempVar++;
            while (skipTemps.contains(tempVar))
                tempVar++;
        } else if (containsOps(condString)) {
            returnString += "t" + tempVar + ".bool :=.bool " + condString + "\n";
            condString = isWhile ? "t" + tempVar + ".bool ==.bool t" + tempVar + ".bool;" : "t" + tempVar + ".bool !.bool t" + tempVar + ".bool;";
            tempVar++;
            while (skipTemps.contains(tempVar))
                tempVar++;
        }
        if (condString.contains(";"))
            condString = condString.substring(0, condString.length() - 1);
        result.add(returnString);
        result.add(condString);
        return result;
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

    private List<String> checkForNested(JmmNode node, String nodeString) {
        String str = "";
        List<String> ret = new ArrayList<>();

        if (isOp(node) || node.getKind().equals("OBJECT_METHOD") || node.getKind().equals("ARRAY_ACCESS")) {

            String substring;
            String before = "";
            if (!nodeString.contains(":=."))
                substring = nodeString.substring(nodeString.lastIndexOf("."), nodeString.length() - 1);
            else {
                substring = nodeString.substring(nodeString.indexOf("."), nodeString.indexOf(" "));
                if (nodeString.contains("\n")) {
                    before = nodeString.substring(0, nodeString.lastIndexOf("\n") + 1);
                    nodeString = nodeString.substring(nodeString.lastIndexOf("\n") + 1);
                } else {
                    nodeString = nodeString.substring(nodeString.lastIndexOf(" "));
                }
            }
            if (!node.getKind().equals("ARRAY_ACCESS")) {
                str = before + "t" + tempVar + substring + " :=" + substring + " " + nodeString + "\n";
                nodeString = "t" + tempVar + substring;
                tempVar++;
                while (skipTemps.contains(tempVar))
                    tempVar++;
            } else {
                if (nodeString.contains(":=.")) {
                    before += nodeString;
                    nodeString = nodeString.substring(0, nodeString.indexOf(" "));
                } else
                    nodeString = nodeString.substring(0, nodeString.length() - 1);
            }

        } else {
            if (nodeString.contains("\n")) {
                String[] lines = nodeString.split("\n");
                if (lines[lines.length - 1].contains(":=.")) {
                    for (String line : lines)
                        str += line;

                    nodeString = lines[lines.length - 1].substring(0, lines[lines.length - 1].indexOf(" "));
                } else {
                    for (String line : Arrays.copyOfRange(lines, 0, lines.length - 1))
                        str += line;


                    nodeString = lines[lines.length - 1];
                }
            } else if (nodeString.contains(":=.")) {
                str = nodeString;
                nodeString = nodeString.substring(0, nodeString.indexOf(" "));
            }
        }
        ret.add(str);
        ret.add(nodeString);
        return ret;
    }

    private boolean isOp(JmmNode jmmNode) {
        return jmmNode.getKind().equals("OPERATION") || jmmNode.getKind().equals("LESS") || jmmNode.getKind().equals("AND") || jmmNode.getKind().equals("EXCLAMATION");
    }


    private String defaultVisit(JmmNode jmmNode, List<Report> reports) {
        StringBuilder visitStr = new StringBuilder();
        for (JmmNode child : jmmNode.getChildren()) {
            visitStr.append(visit(child, reports));
        }
        return visitStr.toString();
    }

    private String dealWithClass(JmmNode jmmNode, List<Report> reports) {
        StringBuilder classStr = new StringBuilder();
        String className = jmmNode.get("name");
        classStr.append(className).append(" {\n");

        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("VAR_DECLARATION"))
                classStr.append(visit(child));
        }

        classStr.append("\t.construct ").append(className).append("().V {\n")
                .append("\t\tinvokespecial(this, \"<init>\").V;\n")
                .append("\t}\n");

        for (JmmNode child : jmmNode.getChildren()) {
            if (!child.getKind().equals("VAR_DECLARATION"))
                classStr.append(visit(child, reports));
        }

        classStr.append("}");
        ollirStr += classStr;
        return ollirStr;
    }
}
