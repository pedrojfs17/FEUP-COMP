import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class InitializedVariablesVisitor extends AJmmVisitor<List<Report>, String> {
    SymbolTable symbolTable;
    List<String> initializedVariables;

    public InitializedVariablesVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.initializedVariables = new ArrayList<>();

        addVisit("IDENTIFIER", this::dealWithIdentifier);
        addVisit("ASSIGNMENT", this::dealWithAssignment);
        addVisit("TRUE", this::dealWithBoolean);
        addVisit("FALSE", this::dealWithBoolean);
        addVisit("INT", this::dealWithInt);
        addVisit("OPERATION", this::dealWithOperation);
        addVisit("VAR_DECLARATION", this::dealWithVarDeclaration);
        addVisit("NEW", this::dealWithNew);
        addVisit("ARRAY", this::dealWithArray);
        addVisit("ARRAY_ACCESS", this::dealWithInt);
        addVisit("LENGTH", this::dealWithInt);
        addVisit("OBJECT_METHOD", this::dealWithObjectMethod);
        addVisit("METHOD_CALL", this::dealWithMethodCall);
        setDefaultVisit(this::defaultVisit);
    }

    private String defaultVisit(JmmNode jmmNode, List<Report> reports) {
        for (JmmNode child : jmmNode.getChildren())
            visit(child, reports);
        return "";
    }

    private String dealWithBoolean(JmmNode jmmNode, List<Report> reports) {
        return "boolean";
    }

    private String dealWithInt(JmmNode jmmNode, List<Report> reports) {
        return "int";
    }

    private String dealWithArray(JmmNode jmmNode, List<Report> reports) {
        visit(jmmNode.getChildren().get(0), reports);
        return "int array";
    }

    private String dealWithObjectMethod(JmmNode jmmNode, List<Report> reports) {
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode method = jmmNode.getChildren().get(1);

        visit(identifier, reports);
        return visit(method, reports);
    }

    private String dealWithMethodCall(JmmNode jmmNode, List<Report> reports) {
        for (JmmNode child : jmmNode.getChildren())
            visit(child, reports);
        return symbolTable.getReturnType(jmmNode.get("name")).getName();
    }

    private String dealWithNew(JmmNode jmmNode, List<Report> reports) {
        return visit(jmmNode.getChildren().get(0), reports);
    }

    private String dealWithOperation(JmmNode jmmNode, List<Report> reports) {
        JmmNode lhs = jmmNode.getChildren().get(0);
        JmmNode rhs = jmmNode.getChildren().get(1);

        String lhsType = visit(lhs, reports);
        String rhsType = visit(rhs, reports);

        if (!lhsType.equals(rhsType)) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(rhs.get("line")),
                    Integer.parseInt(rhs.get("col")),
                    "Type mismatch in operation. " + lhsType + " != " + rhsType
            ));
        }

        return lhsType;
    }

    private String dealWithIdentifier(JmmNode jmmNode, List<Report> reports) {
        Optional<JmmNode> ancestor = jmmNode.getAncestor("MAIN").isPresent() ? jmmNode.getAncestor("MAIN") : jmmNode.getAncestor("METHOD_DECLARATION");
        Symbol localVar = symbolTable.getMethod(ancestor.get().get("name")).getLocalVariable(jmmNode.get("name"));
        Symbol classVar = symbolTable.getField(jmmNode.get("name"));

        if (localVar == null && classVar == null) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("line")),
                    Integer.parseInt(jmmNode.get("col")),
                    "Variable \"" +  jmmNode.get("name") + "\" is undefined."
            ));
            return "<Unknown>";
        }

        if (jmmNode.getParent().getKind().equals("OPERATION") && !initializedVariables.contains(jmmNode.get("name"))) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("line")),
                    Integer.parseInt(jmmNode.get("col")),
                    "Variable \"" +  jmmNode.get("name") + "\" has not been initialized."
            ));
        }

        return localVar != null ? localVar.getType().getName() : classVar.getType().getName();
    }

    private String dealWithAssignment(JmmNode jmmNode, List<Report> reports) {
        JmmNode lhs = jmmNode.getChildren().get(0);
        JmmNode rhs = jmmNode.getChildren().get(1);

        if (!lhs.getKind().equals("IDENTIFIER") && !lhs.getKind().equals("ARRAY")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(lhs.get("line")),
                    Integer.parseInt(lhs.get("col")),
                    lhs.getKind() + " is not an Identifier!"
            ));
        }

        String lhsType = visit(lhs, reports);
        String rhsType = visit(rhs, reports);

        if (!lhsType.equals(rhsType)) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(rhs.get("line")),
                    Integer.parseInt(rhs.get("col")),
                    "Type mismatch in assignment. " + lhsType + " != " + rhsType
            ));
        }
        else {
            if (lhs.getKind().equals("IDENTIFIER"))
                initializedVariables.add(lhs.get("name"));
            else
                initializedVariables.add(lhs.getChildren().get(0).get("name"));
        }

        return "";
    }

    private String dealWithVarDeclaration(JmmNode jmmNode, List<Report> reports) {
        List<String> types = new ArrayList<>(Arrays.asList("int", "int array", "boolean", symbolTable.getClassName()));
        types.addAll(symbolTable.getImports());

        if (!types.contains(jmmNode.get("type"))) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("line")),
                    Integer.parseInt(jmmNode.get("col")),
                    "Type \"" + jmmNode.get("type") + "\" is missing."
            ));
            return "<Unknown>";
        }

        return jmmNode.get("type");
    }
}
