import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.List;
import java.util.Optional;

public class SemanticVisitor extends AJmmVisitor<List<Report>, Boolean> {
    SymbolTable symbolTable;

    public SemanticVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        addVisit("OBJECT_METHOD", this::dealWithOBJECTMETHOD);
        addVisit("METHOD_CALL", this::dealWithMETHODCALL);
        addVisit("OPERATION", this::dealWithOPERATION);
        addVisit("ARRAY", this::dealWithARRAY);
        addVisit("ARRAY_ACCESS", this::dealWithARRAY);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean dealWithOBJECTMETHOD(JmmNode jmmNode, List<Report> reports) {
        var children = jmmNode.getChildren();
        if (children.get(0).getKind().equals("THIS")) {
            if (symbolTable.getMethod(children.get(1).get("name")) == null)
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, children.get(1).get("line") != null ? Integer.parseInt(children.get(1).get("line")) : 0, Integer.parseInt(children.get(1).get("col")), "Method " + children.get(1).get("name") + "() isn't declared"));
            else if (children.get(1).getChildren().size() != symbolTable.getMethod(children.get(1).get("name")).getParameters().size())
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, children.get(1).get("line") != null ? Integer.parseInt(children.get(1).get("line")) : 0, Integer.parseInt(children.get(1).get("col")), "Method " + children.get(1).get("name") + "() has the wrong number of arguments"));
        } else if (children.get(1).getKind().equals("LENGTH") && !children.get(0).getKind().equals("IDENTIFIER")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, children.get(0).get("line") != null ? Integer.parseInt(children.get(0).get("line")) : 0, Integer.parseInt(children.get(0).get("col")), "Length does not exist over simple types"));
        }
        visit(jmmNode.getChildren().get(1), reports);
        return true;
    }

    private Boolean dealWithMETHODCALL(JmmNode jmmNode, List<Report> reports) {
        if(!jmmNode.getAncestor("OBJECT_METHOD").get().getChildren().get(0).getKind().equals("THIS")) return false;
        Optional<JmmNode> ancestor = jmmNode.getAncestor("MAIN").isPresent() ? jmmNode.getAncestor("MAIN") : jmmNode.getAncestor("METHOD_DECLARATION");

        int i = 0;
        for (JmmNode child : jmmNode.getChildren()) {
            if (child.getKind().equals("IDENTIFIER")) {
                Symbol symbol = symbolTable.getMethod(ancestor.get().get("name")).getLocalVariable(child.get("name")) != null ? symbolTable.getMethod(ancestor.get().get("name")).getLocalVariable(child.get("name")): symbolTable.getField(child.get("name"));
                System.out.println(symbol);
                if (symbol.getType()!=symbolTable.getMethod(jmmNode.get("name")).getParameters().get(i).getType())
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, child.get("line") != null ? Integer.parseInt(child.get("line")) : 0, Integer.parseInt(child.get("col")), "Argument " + child.get("name") + " is of wrong type, "+symbolTable.getMethod(jmmNode.get("name")).getParameters().get(i).getType().getName()+" expected."));
            }
            else if (i<symbolTable.getMethod(jmmNode.get("name")).getParameters().size() && !symbolTable.getMethod(jmmNode.get("name")).getParameters().get(i).getType().equals(toType(child)))
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, child.get("line") != null ? Integer.parseInt(child.get("line")) : 0, Integer.parseInt(child.get("col")), "Argument is of wrong type, "+symbolTable.getMethod(jmmNode.get("name")).getParameters().get(i).getType().getName()+" expected."));
            visit(child, reports);
            i++;
        }

        return true;
    }

    private Type toType(JmmNode node) {
        if(node.getKind().equals("ARRAY")) {
            return new Type("int",true);
        }
        return new Type(node.getKind().toLowerCase(),false);
    }

    private Boolean dealWithARRAY(JmmNode jmmNode, List<Report> reports) {
        for (JmmNode child : jmmNode.getChildren()) {
            if (!child.getKind().equals("OPERATION") && !child.getKind().equals("INT"))
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, child.get("line") != null ? Integer.parseInt(child.get("line")) : 0, Integer.parseInt(child.get("col")), "Array access/initialization must be done with integer - " + child.getKind() + " isn't type int"));
            visit(child, reports);
        }

        return true;
    }

    private Boolean defaultVisit(JmmNode jmmNode, List<Report> reports) {
        for (JmmNode child : jmmNode.getChildren()) {
            visit(child, reports);
        }
        return true;
    }


    private Boolean dealWithOPERATION(JmmNode jmmNode, List<Report> reports) {
        var children = jmmNode.getChildren();

        for (JmmNode child : children) {
            if (child.getKind().equals("IDENTIFIER")) {
                Optional<JmmNode> method = child.getAncestor("MAIN").isPresent() ? child.getAncestor("MAIN") : child.getAncestor("METHOD_DECLARATION");
                Symbol symbol = symbolTable.getMethod(method.get().get("name")).getLocalVariable(child.get("name"));
                if (!symbol.getType().getName().equals("int")) {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, child.get("line") != null ? Integer.parseInt(child.get("line")) : 0, Integer.parseInt(child.get("col")), "Variable \"" + child.get("name") + "\" isn't type int"));
                }

            } else if (!child.getKind().equals("INT")) {
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, child.get("line") != null ? Integer.parseInt(child.get("line")) : 0, Integer.parseInt(child.get("col")), child.getKind() + " can't be in a operation"));
            }
            visit(child, reports);
        }

        return true;
    }

}
