import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;

public class OllirVisitor extends AJmmVisitor<List<Report>, String> {
    SymbolTable symbolTable;
    String ollirStr;

    public OllirVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.ollirStr = "";

        addVisit("CLASS_DECLARATION", this::dealWithClass);
        addVisit("MAIN", this::dealWithMain);
        addVisit("OBJECT_METHOD", this::dealWithObjectMethod);
        setDefaultVisit(this::defaultVisit);
    }

    private String dealWithObjectMethod(JmmNode jmmNode, List<Report> reports) {
        StringBuilder methodStr = new StringBuilder();
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode call = jmmNode.getChildren().get(1);

        // Visitar filhos da call
        // Construir lista de argumentos

        if (symbolTable.checkVariableInImports(identifier.get("name"))) {
            methodStr.append("invokestatic(" + identifier.get("name") + ", \"" + call.get("name")+ "\"");
            // Inserir argumentos
            methodStr.append(").V;\n");
        }
        System.out.println(methodStr);
        return methodStr.toString();
    }

    private String dealWithMain(JmmNode jmmNode, List<Report> reports) {
        StringBuilder mainStr = new StringBuilder();
        mainStr.append("\n.method public static main(args.array.String).V {\n");

        for (JmmNode child : jmmNode.getChildren()) {
            mainStr.append(visit(child, reports));
        }

        mainStr.append("}\n");

        return mainStr.toString();
    }

    private String defaultVisit(JmmNode jmmNode, List<Report> reports) {
        StringBuilder visitStr = new StringBuilder();
        System.out.println(jmmNode.getKind());
        for (JmmNode child : jmmNode.getChildren()) {
            System.out.println(child.getKind());
            visitStr.append(visit(child, reports));
        }
        return visitStr.toString();
    }

    private String dealWithClass(JmmNode jmmNode, List<Report> reports) {
        System.out.println("VISITANDUWU");
        StringBuilder classStr = new StringBuilder();
        String className = jmmNode.get("name");
        classStr.append(className + " {\n" +
                ".construct " + className + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n");

        for (JmmNode child : jmmNode.getChildren()) {
            classStr.append(visit(child, reports));
        }

        classStr.append("\n}");
        ollirStr += classStr;
        return ollirStr;
    }
}
