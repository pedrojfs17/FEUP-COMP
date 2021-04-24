import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;
import java.util.Optional;

public class OllirVisitor extends AJmmVisitor<List<Report>, String> {
    SymbolTable symbolTable;
    String ollirStr;

    public OllirVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.ollirStr = "";

        addVisit("CLASS_DECLARATION", this::dealWithClass);
        addVisit("MAIN", this::dealWithMain);
        addVisit("METHOD_DECLARATION", this::dealWithMethodDeclaration);
        addVisit("OBJECT_METHOD", this::dealWithObjectMethod);
        addVisit("ASSIGNMENT", this::dealWithAssignment);
        addVisit("RETURN", this::dealWithReturn);
        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithReturn(JmmNode jmmNode, List<Report> reports) {
        StringBuilder methodStr = new StringBuilder();
        JmmNode identifier = jmmNode.getChildren().get(0);
        methodStr.append("\t\t" + "ret.");
        if (identifier.getKind().equals("IDENTIFIER")) {
            Optional<JmmNode> ancestor = jmmNode.getAncestor("MAIN").isPresent() ? jmmNode.getAncestor("MAIN") : jmmNode.getAncestor("METHOD_DECLARATION");
            Symbol var = symbolTable.getVariable(identifier.get("name"), ancestor.get().get("name"));
            methodStr.append(parseType(var.getType().getName()) + " " + var.getName() + "." + parseType(var.getType().getName()) + "\n");

        } else if (identifier.getKind().equals("INT")) {
            methodStr.append("i32 " + identifier.get("value") + ".i32" + "\n");
        } else if (identifier.getKind().equals("FALSE") || identifier.getKind().equals("TRUE")) {
            methodStr.append("bool " + parseType(identifier.getKind()) + "\n");
        }

        return methodStr.toString();
    }

    private String dealWithAssignment(JmmNode jmmNode, List<Report> reports) {
        StringBuilder methodStr = new StringBuilder();
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode assignment = jmmNode.getChildren().get(1);
        Optional<JmmNode> ancestor = jmmNode.getAncestor("MAIN").isPresent() ? jmmNode.getAncestor("MAIN") : jmmNode.getAncestor("METHOD_DECLARATION");
        Symbol var;
        if(identifier.getKind().equals("ARRAY_ACCESS"))
            var = symbolTable.getVariable(identifier.getChildren().get(0).get("name"), ancestor.get().get("name"));
        else
            var = symbolTable.getVariable(identifier.get("name"), ancestor.get().get("name"));


        // Visitar filhos do assignment

        methodStr.append("\t\t" + var.getName() + "." + parseType(var.getType().getName()) + " :=." + parseType(var.getType().getName()) + " ");
        // Inserir argumentos

        if (assignment.getKind().equals("INT")) {
            methodStr.append(assignment.get("value") + ".i32\n");
        } else if (assignment.getKind().equals("NEW")) {
            if (assignment.getChildren().get(0).getKind().equals("ARRAY")) {
                methodStr.append("new(array, " + assignment.getChildren().get(0).getChildren().get(0).get("value") + ".i32).array.i32\n");
            } else if (assignment.getChildren().get(0).getKind().equals("IDENTIFIER")) {
                methodStr.append("new(" + assignment.getChildren().get(0).get("name") + ")." + assignment.getChildren().get(0).get("name") + "\n");
                methodStr.append("\t\tinvokespecial(" + identifier.get("name") + "." + assignment.getChildren().get(0).get("name") + ", \"<init>\").V\n");
            }

        } else if (assignment.getKind().equals("TRUE") || assignment.getKind().equals("FALSE")) {
            methodStr.append(parseType(assignment.getKind()) + "\n");
        } else if (assignment.getKind().equals("OBJECT_METHOD")) {
            methodStr.append(visit(assignment, reports));
        }
        return methodStr.toString();
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
                if (grandchild.getKind().equals("IDENTIFIER")) {
                    Optional<JmmNode> ancestor = jmmNode.getAncestor("MAIN").isPresent() ? jmmNode.getAncestor("MAIN") : jmmNode.getAncestor("METHOD_DECLARATION");
                    Symbol var = symbolTable.getVariable(grandchild.get("name"), ancestor.get().get("name"));
                    methodStr.append(var.getName() + "." + parseType(var.getType().getName()) + ", ");
                } else if (grandchild.getKind().equals("INT")) {
                    methodStr.append(grandchild.get("value") + ".i32\n");
                } else if (grandchild.getKind().equals("TRUE") || grandchild.getKind().equals("FALSE")) {
                    methodStr.append(parseType(grandchild.getKind()) + "\n");
                }
            }
            if (grandchildren)
                methodStr.delete(methodStr.length() - 2, methodStr.length());
            methodStr.append(").V;\n");
        } else {
            String varName = !identifier.getKind().equals("THIS") ? identifier.get("name") : "this";
            String callName = !call.getKind().equals("LENGTH") ? call.get("name") : "length";
            methodStr.append("\t\tinvokevirtual(" + varName + ", \"" + callName + "\"");
            if (callName.equals("length")) methodStr.append(").i32;\n");
            else {
                if (call.getChildren().size() > 0)
                    methodStr.append(", ");
                boolean grandchildren = false;
                for (JmmNode grandchild : call.getChildren()) {
                    grandchildren = true;
                    if (grandchild.getKind().equals("IDENTIFIER")) {
                        Optional<JmmNode> ancestor = jmmNode.getAncestor("MAIN").isPresent() ? jmmNode.getAncestor("MAIN") : jmmNode.getAncestor("METHOD_DECLARATION");
                        Symbol var = symbolTable.getVariable(grandchild.get("name"), ancestor.get().get("name"));
                        methodStr.append(var.getName() + "." + parseType(var.getType().getName()) + ", ");
                    } else if (grandchild.getKind().equals("INT")) {
                        methodStr.append(grandchild.get("value") + ".i32\n");
                    } else if (grandchild.getKind().equals("TRUE") || grandchild.getKind().equals("FALSE")) {
                        methodStr.append(parseType(grandchild.getKind()) + "\n");
                    }
                }
                if (grandchildren)
                    methodStr.delete(methodStr.length() - 2, methodStr.length());
                // Inserir argumentos
                methodStr.append(")." + parseType(symbolTable.getMethod(call.get("name")).getType().getName()) + ";\n");
            }

        }
        return methodStr.toString();
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
        mainStr.append("\n\t.method public " + jmmNode.get("name") + "(");
        boolean children = false;
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("PARAMETER")) {
                children = true;
                mainStr.append(child.get("name") + "." + parseType(child.get("type")) + ", ");
            } else
                continue;
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

    private String parseType(String type) {
        if (type.equals("int"))
            return "i32";
        else if (type.equals("void"))
            return "V";
        else if (type.equals("FALSE"))
            return "0.bool";
        else if (type.equals("TRUE"))
            return "1.bool";
        else if (type.equals("int array"))
            return "array.i32";
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

        classStr.append("\n}");
        ollirStr += classStr;
        return ollirStr;
    }
}
