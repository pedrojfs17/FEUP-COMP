import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OllirVisitor2 extends AJmmVisitor<List<Report>, String> {
    SymbolTable symbolTable;
    int tempVar = 1;
    int labelCount = 0;
    int ifCount;
    int paramCount;

    public OllirVisitor2(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        paramCount = symbolTable.getFields().size()+1;

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

    private String dealWithClass(JmmNode jmmNode, List<Report> reports) {
        StringBuilder classStr = new StringBuilder();
        String className = jmmNode.get("name");
        classStr.append(className).append(" {\n");

        for (JmmNode child: jmmNode.getChildren()) {
            if(child.getKind().equals("VAR_DECLARATION"))
                classStr.append(visit(child));
        }

        classStr.append("\t.construct ").append(className).append("().V {\n")
                .append("\t\tinvokespecial(this, \"<init>\").V;\n")
                .append("\t}\n");

        for (JmmNode child : jmmNode.getChildren()) {
            if(!child.getKind().equals("VAR_DECLARATION"))
                classStr.append(visit(child, reports));
        }

        classStr.append("}");
        return classStr.toString();
    }

    private String dealWithVar(JmmNode jmmNode, List<Report> reports) {
        Optional<JmmNode> ancestor = getAncestor(jmmNode,"METHOD_DECLARATION","CLASS_DECLARATION");
        if(ancestor.get().getKind().equals("CLASS_DECLARATION") && symbolTable.isGlobal(jmmNode.get("name")))
            return ".field private "+ escapeVarName(jmmNode.get("name"))+"."+parseType(symbolTable.getField(jmmNode.get("name")).getType().getName())+";\n";

        return "";
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

        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("PARAMETER")) {
                if (children) mainStr.append(", ");
                mainStr.append(child.get("name")).append(".").append(parseType(child.get("type")));
                children = true;
            }
        }

        mainStr.append(").").append(parseType(jmmNode.get("return"))).append("{\n");

        boolean hasReturn = false;
        for (JmmNode child : jmmNode.getChildren()) {
            if (!child.getKind().equals("PARAMETER")) {
                mainStr.append(visit(child, reports));
                if (child.getKind().equals("RETURN"))
                    hasReturn = true;
            }
        }
        mainStr.append("\t").append(hasReturn ? "" : "\tret.V;\n\t").append("}\n");

        return mainStr.toString();
    }

    private String dealWithObjectMethod(JmmNode jmmNode, List<Report> reports) {
        StringBuilder methodStr = new StringBuilder();
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode call = jmmNode.getChildren().get(1);

        String identifierStr = visit(identifier);
    }

    private String dealWithOperation(JmmNode jmmNode, List<Report> reports) {
        String str = "";
        JmmNode lhs = jmmNode.getChildren().get(0);
        JmmNode rhs = jmmNode.getKind().equals("EXCLAMATION") ? lhs : jmmNode.getChildren().get(1);
        String lhsString = visit(lhs);
        String rhsString = jmmNode.getKind().equals("EXCLAMATION") ? lhsString : visit(rhs);
        List<String> lhsResult = checkForNested(lhs, lhsString);
        List<String> rhsResult = jmmNode.getKind().equals("EXCLAMATION") ? lhsResult : checkForNested(rhs, rhsString);
        str += lhsResult.get(0);
        lhsString = lhsResult.get(1);
        str += jmmNode.getKind().equals("EXCLAMATION") ? "" : rhsResult.get(0);
        rhsString = rhsResult.get(1);

        str += lhsString + " " + getOperator(jmmNode);
        if (jmmNode.getKind().equals("OPERATION"))
            str += ".i32 ";
        else
            str += ".bool ";
        str += rhsString + ";";
        return str;
    }

    private String dealWithArrayAccess(JmmNode jmmNode, List<Report> reports) {
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode access = jmmNode.getChildren().get(1);
        String identifierVisit = visit(identifier);
        String accessVisit = visit(access);
        String before = "";
        if(identifierVisit.startsWith("getfield")) {
            before += "\t\tt"+tempVar+".array.i32 :=.array.i32 "+identifierVisit;
            identifierVisit = "\t\tt"+tempVar;
            tempVar++;
        } else
            identifierVisit=identifierVisit.substring(0,identifierVisit.indexOf(".array.i32"));


        if(accessVisit.startsWith("getfield")) {
            before += "\t\tt"+tempVar+".array.i32 :=.array.i32 "+accessVisit;
            accessVisit = "\t\tt"+tempVar;
            tempVar++;
        } else
            accessVisit=accessVisit.substring(0,accessVisit.indexOf(".array.i32"));




        return "";
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

    private String dealWithIdentifier(JmmNode jmmNode, List<Report> reports) {
        Optional<JmmNode> ancestor = getAncestor(jmmNode, "MAIN", "METHOD_DECLARATION");
        Symbol var = symbolTable.getVariable(jmmNode.get("name"), ancestor.get().get("name"));
        String varName = escapeVarName(var.getName());
        String varType = parseType(var.getType().getName());
        String before="";
        if(symbolTable.isGlobal(var.getName()))
            return "getfield(this, " + varName+"."+varType+")."+varType;
        else if (symbolTable.isParam(ancestor.get().get("name"),var.getName()))
            before+="$"+paramCount+".";
        return before + varName + "." + varType;
    }

    private String dealWithArray(JmmNode jmmNode, List<Report> reports) {
        String str = "";
        JmmNode child = jmmNode.getChildren().get(0);
        String childVisit = visit(child);
        if (child.getKind().equals("OBJECT_METHOD")) {
            str += "\t\tt"+tempVar+".array.i32 :=.array.i32 ";
            childVisit = childVisit.substring(0, childVisit.indexOf(" "));
        }
        str += "new(array, t"+tempVar+".array.i32" + ").array.i32";

        return jmmNode.get("value") + ".i32";
    }

    private String dealWithInt(JmmNode jmmNode, List<Report> reports) {
        return jmmNode.get("value") + ".i32";
    }

    private String dealWithBoolean(JmmNode jmmNode, List<Report> reports) {
        return parseType(jmmNode.getKind());
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
            if(!node.getKind().equals("ARRAY_ACCESS")) {
                str = before + "t" + tempVar + substring + " :=" + substring + " " + nodeString + "\n";
                nodeString = "t" + tempVar + substring;
                tempVar++;
            } else {
                nodeString = nodeString.substring(0,nodeString.length()-1);
            }

        } else {
            if(nodeString.contains(":=.")) {
                str=nodeString;
                nodeString=nodeString.substring(0,nodeString.indexOf(" "));
            }
        }
        ret.add(str);
        ret.add(nodeString);
        return ret;
    }

    private boolean isOp(JmmNode jmmNode) {
        return jmmNode.getKind().equals("OPERATION") || jmmNode.getKind().equals("LESS") || jmmNode.getKind().equals("AND") || jmmNode.getKind().equals("EXCLAMATION");
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
        else if(varName.startsWith("array"))
            return "arr_"+varName.substring(5);
        return varName;
    }

    private String defaultVisit(JmmNode jmmNode, List<Report> reports) {
        StringBuilder visitStr = new StringBuilder();
        for (JmmNode child : jmmNode.getChildren()) {
            visitStr.append(visit(child, reports));
        }
        return visitStr.toString();
    }


}
