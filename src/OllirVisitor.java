import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

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
        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithOperation(JmmNode jmmNode, List<Report> reports) {
        String str="";
        JmmNode lhs = jmmNode.getChildren().get(0);
        JmmNode rhs = jmmNode.getChildren().get(1);
        String lhsString = visit(lhs);
        String rhsString = visit(rhs);
        if(isOp(lhs) || lhs.getKind().equals("OBJECT_METHOD") ) {
            str += "t"+tempVar+lhsString.substring(lhsString.lastIndexOf("."),lhsString.length()-1)+" :=" +lhsString.substring(lhsString.lastIndexOf("."),lhsString.length()-1)+" " +lhsString+"\n";
            lhsString = "t"+tempVar+lhsString.substring(lhsString.lastIndexOf("."),lhsString.length()-1);
            tempVar++;
        }
        if(isOp(rhs) || rhs.getKind().equals("OBJECT_METHOD")) {
            str += "t"+tempVar+rhsString.substring(rhsString.lastIndexOf("."),rhsString.length()-1)+" :="+rhsString.substring(rhsString.lastIndexOf("."),rhsString.length()-1)+" "+rhsString+"\n";
            rhsString = "t"+tempVar+rhsString.substring(rhsString.lastIndexOf("."),rhsString.length()-1);
            tempVar++;
        }
        // IF CHILD IS: OP, LESS, AND OR OBJECT_METHOD -> COLLECT STRING, ASSIGN TO TEMP VARIABLE, REPLACE THE STRING BY THE TEMP VAR
        str += lhsString+" "+jmmNode.get("op");
        if(jmmNode.getKind().equals("OPERATION"))
            str+=".i32 ";
        else
            str+=".bool ";
        str +=rhsString+";";
        return str;
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
            methodStr.append("\t\t" + var.getName() + "." + parseType(var.getType().getName()) + " :=." + parseType(var.getType().getName()) + " ");
            methodStr.append(visit(assignment));
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
                methodStr.append(visit(grandchild)+", ");
            }
            if (grandchildren)
                methodStr.delete(methodStr.length() - 2, methodStr.length());
            methodStr.append(").V");
        } else {
            String varName = !identifier.getKind().equals("THIS") ? identifier.get("name") : "this";
            String callName = !call.getKind().equals("LENGTH") ? call.get("name") : "length";
            String type = !varName.equalsIgnoreCase("this") ? varName+"." : "";
            methodStr.append("invokevirtual(" + type +  getVariableTtype(varName,jmmNode)+ ", \"" + callName + "\"");
            if (callName.equals("length")) methodStr.append(").i32;");
            else {
                if (call.getChildren().size() > 0)
                    methodStr.append(", ");
                boolean grandchildren = false;
                for (JmmNode grandchild : call.getChildren()) {
                    grandchildren = true;
                    methodStr.append(visit(grandchild)+", ");
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
    private String getVariableTtype(String varName, JmmNode currentNode) {
        if(varName.equalsIgnoreCase("this"))
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
            case "int":
                return "i32";
            case "void":
                return "V";
            case "FALSE":
                return "0.bool";
            case "TRUE":
                return "1.bool";
            case "int array":
                return "array.i32";
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
