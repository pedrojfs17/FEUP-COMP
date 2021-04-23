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
        addVisit("OBJECT", this::dealWithObject);
        addVisit("ARRAY", this::dealWithArray);
        addVisit("ARRAY_ACCESS", this::dealWithArrayAccess);
        addVisit("LENGTH", this::dealWithInt);
        addVisit("OBJECT_METHOD", this::dealWithObjectMethod);
        addVisit("METHOD_CALL", this::dealWithMethodCall);
        addVisit("LESS", this::dealWithLess);
        addVisit("AND", this::dealWithAnd);
        addVisit("NOT", this::dealWithNot);
        addVisit("WHILE", this::dealWithCondition);
        addVisit("IF", this::dealWithCondition);
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

    private String dealWithNot(JmmNode jmmNode, List<Report> reports) {
        String expressionType = visit(jmmNode.getChildren().get(0), reports);

        if (!expressionType.equals("boolean")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("line")),
                    Integer.parseInt(jmmNode.get("col")),
                    "Not operator can only be applied to boolean expressions."
            ));
        }

        return "int array";
    }

    private String dealWithArray(JmmNode jmmNode, List<Report> reports) {
        String arrSize = visit(jmmNode.getChildren().get(0), reports);

        if (!arrSize.equals("int")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("line")),
                    Integer.parseInt(jmmNode.get("col")),
                    "Array length must be of type int."
            ));
        }

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

        if (symbolTable.getMethod(jmmNode.get("name")) != null)
            return symbolTable.getReturnType(jmmNode.get("name")).getName();
        else
            return "";
    }

    private String dealWithNew(JmmNode jmmNode, List<Report> reports) {
        return visit(jmmNode.getChildren().get(0), reports);
    }

    private String dealWithObject(JmmNode jmmNode, List<Report> reports) {
        return jmmNode.get("name");
    }

    private String dealWithArrayAccess(JmmNode jmmNode, List<Report> reports) {
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode idx = jmmNode.getChildren().get(1);

        String identifierType = visit(identifier, reports);
        String idxType = visit(idx, reports);

        if (!identifier.getKind().equals("IDENTIFIER")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(identifier.get("line")),
                    Integer.parseInt(identifier.get("col")),
                    "\"" + identifier.getKind() + "\" is not a variable."
            ));
            return "<Unknown>";
        }

        if (!identifierType.equals("int array")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(identifier.get("line")),
                    Integer.parseInt(identifier.get("col")),
                    "Variable \"" + identifier.get("name") + "\" is not an array."
            ));
            return "<Unknown>";
        }

        if (!idxType.equals("int")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(idx.get("line")),
                    Integer.parseInt(idx.get("col")),
                    "Array index must be of type int."
            ));
        }

        return "int";
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
                    "Type mismatch in operation. <" + lhsType + "> to <" + rhsType + ">"
            ));
        }

        return lhsType;
    }

    private String dealWithLess(JmmNode jmmNode, List<Report> reports) {
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
                    "Type mismatch in operation. <" + lhsType + "> to <" + rhsType + ">"
            ));
        }

        if (!lhsType.equals("int")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(lhs.get("line")),
                    Integer.parseInt(lhs.get("col")),
                    "Type mismatch in less. <" + lhsType + "> should be <int>"
            ));
        }

        if (!rhsType.equals("int")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(rhs.get("line")),
                    Integer.parseInt(rhs.get("col")),
                    "Type mismatch in operation. <" + rhsType + "> should be <int>"
            ));
        }

        return "boolean";
    }

    private String dealWithAnd(JmmNode jmmNode, List<Report> reports) {
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
                    "Type mismatch in operation. <" + lhsType + "> to <" + rhsType + ">"
            ));
        }

        if (!lhsType.equals("boolean")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(lhs.get("line")),
                    Integer.parseInt(lhs.get("col")),
                    "Type mismatch in less. <" + lhsType + "> should be <int>"
            ));
        }

        if (!rhsType.equals("boolean")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(rhs.get("line")),
                    Integer.parseInt(rhs.get("col")),
                    "Type mismatch in operation. <" + rhsType + "> should be <int>"
            ));
        }

        return "boolean";
    }


    private String dealWithIdentifier(JmmNode jmmNode, List<Report> reports) {
        Optional<JmmNode> ancestor = jmmNode.getAncestor("MAIN").isPresent() ? jmmNode.getAncestor("MAIN") : jmmNode.getAncestor("METHOD_DECLARATION");
        Symbol var = symbolTable.getVariable(jmmNode.get("name"), ancestor.get().get("name"));

        if (var == null && !symbolTable.checkVariableInImports(jmmNode.get("name"))) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("line")),
                    Integer.parseInt(jmmNode.get("col")),
                    "Variable \"" + jmmNode.get("name") + "\" is undefined."
            ));
            return "<Unknown>";
        }

        if (jmmNode.getParent().getKind().equals("OPERATION") && !initializedVariables.contains(jmmNode.get("name")) && !symbolTable.getMethod(ancestor.get().get("name")).containsParameter(jmmNode.get("name"))) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.get("line")),
                    Integer.parseInt(jmmNode.get("col")),
                    "Variable \"" + jmmNode.get("name") + "\" has not been initialized."
            ));
        }

        return var != null ? var.getType().getName() : "<Unknown>";
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
                    "Type mismatch in operation. <" + lhsType + "> to <" + rhsType + ">"
            ));
        } else {
            if (lhs.getKind().equals("IDENTIFIER"))
                initializedVariables.add(lhs.get("name"));
            else
                initializedVariables.add(lhs.getChildren().get(0).get("name"));
        }

        return "";
    }

    private String dealWithCondition(JmmNode jmmNode, List<Report> reports) {
        String condition = visit(jmmNode.getChildren().get(0), reports);

        if (!condition.equals("boolean")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(jmmNode.getChildren().get(0).get("line")),
                    Integer.parseInt(jmmNode.getChildren().get(0).get("col")),
                    "Condition must evaluate to a boolean value."
            ));
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
