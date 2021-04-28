import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OllirVisitor extends AJmmVisitor<List<Report>, String> {
    SymbolTable symbolTable;
    String ollirStr;
    int tempVar=1;

    public OllirVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.ollirStr = "";

        addVisit("CLASS_DECLARATION", this::dealWithClass);
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
        addVisit("ARRAY_ACCESS", this::dealWithArrayAccess);
        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithArrayAccess(JmmNode jmmNode, List<Report> reports) {
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode access = jmmNode.getChildren().get(1);
        String acc = "";
        if(access.getKind().equals("INT"))
            acc = access.get("value");
        else if (access.getKind().equals("IDENTIFIER"))
            acc = access.get("name");
        String ret ="\t\tt"+tempVar+".i32 :=.i32 "+identifier.get("name")+"["+acc+".i32].i32;";
        tempVar++;
        return ret;
    }


    private String dealWithOperation(JmmNode jmmNode, List<Report> reports) {
        String str="";
        JmmNode lhs = jmmNode.getChildren().get(0);
        JmmNode rhs = jmmNode.getChildren().get(1);
        String lhsString = visit(lhs);
        String rhsString = visit(rhs);
        List<String> lhsResult = checkForNested(lhs,lhsString);
        List<String> rhsResult = checkForNested(rhs,rhsString);
        str += lhsResult.get(0);
        lhsString = lhsResult.get(1);
        str += rhsResult.get(0);
        rhsString = rhsResult.get(1);

        str += lhsString+" "+getOperator(jmmNode);
        if(jmmNode.getKind().equals("OPERATION"))
            str+=".i32 ";
        else
            str+=".bool ";
        str +=rhsString+";";
        return str;
    }

    private String getOperator(JmmNode jmmNode) {
        if(jmmNode.getKind().equals("LESS"))
            return " <";
        if(jmmNode.getKind().equals("AND"))
            return " &&";
        return jmmNode.get("op");
    }
    private List<String> checkForNested(JmmNode node, String nodeString) {
        String str="";
        List<String> ret = new ArrayList<>();
        if(isOp(node) || node.getKind().equals("OBJECT_METHOD")) {
            String substring;
            String before = "";
            if(!nodeString.contains(":=."))
                substring = nodeString.substring(nodeString.lastIndexOf("."), nodeString.length() - 1);
            else {
                substring = nodeString.substring(nodeString.indexOf("."), nodeString.indexOf(" "));
                before = nodeString.substring(0,nodeString.lastIndexOf("\n")+1);
                nodeString = nodeString.substring(nodeString.lastIndexOf("\n")+1);
            }

            str = before+"t"+tempVar+ substring +" :="+ substring +" "+nodeString+"\n";
            nodeString = "t"+tempVar+ substring;
            tempVar++;
        }
        ret.add(str);
        ret.add(nodeString);
        return ret;
    }

    private boolean isOp(JmmNode jmmNode) {
        return jmmNode.getKind().equals("OPERATION") || jmmNode.getKind().equals("LESS") || jmmNode.getKind().equals("AND");
    }


    private String dealWithReturn(JmmNode jmmNode, List<Report> reports) {
        StringBuilder methodStr = new StringBuilder();
        JmmNode identifier = jmmNode.getChildren().get(0);
        methodStr.append("\t\t" + "ret");
        String child = visit(identifier);
        methodStr.append(child.substring(child.indexOf("."))).append(" ").append(child).append(";\n");

        return methodStr.toString();
    }

    private String dealWithAssignment(JmmNode jmmNode, List<Report> reports) {
        StringBuilder methodStr = new StringBuilder();
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode assignment = jmmNode.getChildren().get(1);
        Optional<JmmNode> ancestor = getAncestor(jmmNode);
        Symbol var;
        if(identifier.getKind().equals("ARRAY_ACCESS"))
            var = symbolTable.getVariable(identifier.getChildren().get(0).get("name"), ancestor.get().get("name"));
        else
            var = symbolTable.getVariable(identifier.get("name"), ancestor.get().get("name"));

        if(assignment.getKind().equals("OPERATION")) {
            String operation = visit(assignment);
            String[] lines = operation.split("\n");

            for (int i = 0; i < lines.length; i++) {
                if (i == lines.length-1) {
                    methodStr.append("\t\t" + var.getName() + "." + parseType(var.getType().getName()) + " :=." + parseType(var.getType().getName()) + " ");
                    methodStr.append(lines[i]).append("\n");
                }
                else
                    methodStr.append("\t\t").append(lines[i]).append("\n");
            }
        } else if(assignment.getKind().equals("NEW")) {
            methodStr.append("\t\t" + var.getName() + "." + parseType(var.getType().getName()) + " :=." + parseType(var.getType().getName()) + " ");
            methodStr.append(visit(assignment)).append(";\n");
            methodStr.append("\t\tinvokespecial(").append(var.getName()).append(".").append(parseType(var.getType().getName()))
                    .append(", \"<init>\").V;\n");
        }
        else {
            String assignString = visit(assignment);
            if(assignment.getKind().equals("ARRAY_ACCESS")) {
                methodStr.append(assignString+"\n");
                assignString = assignString.substring(0, assignString.indexOf(' '));
            }
            methodStr.append("\t\t" + var.getName() + "." + parseType(var.getType().getName()) + " :=." + parseType(var.getType().getName()) + " ");
            methodStr.append(assignString);
            if(!assignment.getKind().equals("OBJECT_METHOD"))
                methodStr.append(";");
            methodStr.append("\n");
        }

        return methodStr.toString();
    }


    private Optional<JmmNode> getAncestor(JmmNode jmmNode) {
        return jmmNode.getAncestor("MAIN").isPresent() ? jmmNode.getAncestor("MAIN") : jmmNode.getAncestor("METHOD_DECLARATION");
    }

    private String dealWithObjectMethod(JmmNode jmmNode, List<Report> reports) {
        StringBuilder methodStr = new StringBuilder();
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode call = jmmNode.getChildren().get(1);

        // Visitar filhos da call
        // Construir lista de argumentos

        if (identifier.getKind().equals("IDENTIFIER") && symbolTable.checkVariableInImports(identifier.get("name"))) {
            methodStr.append("\t\tinvokestatic(" + identifier.get("name") + ", \"" + call.get("name") + "\"");
            if (call.getChildren().size() > 0)
                methodStr.append(", ");
            boolean grandchildren = false;
            for (JmmNode grandchild : call.getChildren()) {
                grandchildren = true;
                String visitString = visit(grandchild);
                if(grandchild.getKind().equals("OBJECT_METHOD"))
                    visitString = visitString.substring(0,visitString.length()-1);
                methodStr.append(visitString+", ");
            }
            if (grandchildren)
                methodStr.delete(methodStr.length() - 2, methodStr.length());
            methodStr.append(").V");
        } else {
            String varName = this.getVarName(identifier);
            String callName = !call.getKind().equals("LENGTH") ? call.get("name") : "length";
            String type = !varName.equalsIgnoreCase("this") ? varName+"." : "";

            if (callName.equals("length")) {
                methodStr.append("t"+tempVar+".i32 :=.i32 arraylength(" + type +"array.i32).i32;");
            }
            else {
                methodStr.append("invokevirtual(" + type +  getVariableType(varName,jmmNode)+ ", \"" + callName + "\"");
                if (call.getChildren().size() > 0)
                    methodStr.append(", ");
                boolean grandchildren = false;
                for (JmmNode grandchild : call.getChildren()) {
                    grandchildren = true;
                    String visitString = visit(grandchild);
                    if(grandchild.getKind().equals("OBJECT_METHOD"))
                        visitString = visitString.substring(0,visitString.length()-1);
                    methodStr.append(visitString+", ");
                }
                if (grandchildren)
                    methodStr.delete(methodStr.length() - 2, methodStr.length());
                // Inserir argumentos
                methodStr.append(")." + parseType(symbolTable.getMethod(call.get("name")).getType().getName()));
            }

        }
        methodStr.append(";");
        return methodStr.toString();
    }

    private String getVarName(JmmNode identifier) {
        if(identifier.getKind().equals("THIS"))
            return "this";
        if (identifier.getKind().equals("NEW"))
            return identifier.getChildren().get(0).get("name");
        return identifier.get("name");
    }

    private String getVariableType(String varName, JmmNode currentNode) {
        if(varName.equalsIgnoreCase("this"))
            return varName;
        if(varName.equals(symbolTable.getClassName()))
            return varName;
        Optional<JmmNode> ancestor = getAncestor(currentNode);
        return symbolTable.getVariable(varName, ancestor.get().get("name")).getType().getName();

    }

    private String dealWithMain(JmmNode jmmNode, List<Report> reports) {
        StringBuilder mainStr = new StringBuilder();
        mainStr.append("\n\t.method public static main(args.array.String).V {\n");

        for (JmmNode child : jmmNode.getChildren()) {
            mainStr.append(visit(child, reports));
        }

        mainStr.append("\t}\n");

        return mainStr.toString();
    }

    private String dealWithMethodDeclaration(JmmNode jmmNode, List<Report> reports) {
        StringBuilder mainStr = new StringBuilder();
        mainStr.append("\t.method public " + jmmNode.get("name") + "(");
        boolean children = false;
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("PARAMETER")) {
                children = true;
                mainStr.append(child.get("name") + "." + parseType(child.get("type")) + ", ");
            }
        }
        if (children)
            mainStr.delete(mainStr.length() - 2, mainStr.length());
        mainStr.append(")." + parseType(jmmNode.get("return")) + "{\n");
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("PARAMETER")) continue;
            mainStr.append(visit(child, reports));
        }
        mainStr.append("\t}\n");

        return mainStr.toString();
    }

    private String dealWithIdentifier(JmmNode jmmNode, List<Report> reports) {
        Optional<JmmNode> ancestor = getAncestor(jmmNode);
        Symbol var = symbolTable.getVariable(jmmNode.get("name"), ancestor.get().get("name"));
        return var.getName() + "." + parseType(var.getType().getName());
    }

    private String dealWithInt(JmmNode jmmNode, List<Report> reports) {
        return jmmNode.get("value") + ".i32";
    }

    private String dealWithBoolean(JmmNode jmmNode, List<Report> reports){
        return parseType(jmmNode.getKind());
    }

    private String dealWithNew(JmmNode jmmNode, List<Report> reports) {
        String str;
        JmmNode child = jmmNode.getChildren().get(0);
        if (child.getKind().equals("ARRAY")) {
            str = "new(array, " + child.getChildren().get(0).get("value") + ".i32).array.i32";
        } else if (child.getKind().equals("IDENTIFIER")) {
            str = "new(" + child.get("name") + ")." + child.get("name");
        } else {
            str = "new(" + child.get("name") + ")." + child.get("name");
        };
        return str;
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

        }
        return type;
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
        classStr.append(className + " {\n" +
                "\t.construct " + className + "().V {\n" +
                "\t\tinvokespecial(this, \"<init>\").V;\n" +
                "\t}\n");

        for (JmmNode child : jmmNode.getChildren()) {
            classStr.append(visit(child, reports));
        }

        classStr.append("}");
        ollirStr += classStr;
        return ollirStr;
    }
}
