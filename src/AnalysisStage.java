
import java.util.*;

import visitors.InitializedVariablesVisitor;
import visitors.SemanticVisitor;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.JmmParserResult;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.examples.ExampleVisitor;
import pt.up.fe.comp.jmm.report.Report;
import semantic.SymbolTable;

public class AnalysisStage implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        JmmNode node = parserResult.getRootNode().sanitize();

        ExampleVisitor visitor = new ExampleVisitor("Identifier", "id");

        SymbolTable symbolTable = new SymbolTable(visitor.visit(node, ""));
        SemanticVisitor semanticVisitor = new SemanticVisitor(symbolTable);
        List<Report> reports = new ArrayList<>();
        semanticVisitor.visit(node, reports);

        InitializedVariablesVisitor variablesVisitor = new InitializedVariablesVisitor(symbolTable);
        variablesVisitor.visit(node, reports);

        reports.sort(Comparator.comparing(Report::getLine));

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }

}