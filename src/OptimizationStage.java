import java.util.ArrayList;
import java.util.List;

import visitors.OllirVisitor;
import visitors.optimizations.ConstantOptimizationVisitor;
import visitors.optimizations.RegisterAllocationOptimizer;
import org.specs.comp.ollir.ClassUnit;
import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import semantic.SymbolTable;

/**
 * Copyright 2021 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class OptimizationStage implements JmmOptimization {
    private int n_registers;

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult, boolean optimize) {
        JmmNode node = semanticsResult.getRootNode();

        // Convert the AST to a String containing the equivalent OLLIR code
        String ollirCode = ""; // Convert node ...

        // More reports from this stage
        List<Report> reports = new ArrayList<>();

        OllirVisitor visitor = new OllirVisitor((SymbolTable) semanticsResult.getSymbolTable(), optimize);

        ollirCode = visitor.visit(node);
        System.out.println(ollirCode);

        return new OllirResult(semanticsResult, ollirCode, reports);
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        JmmNode node = semanticsResult.getRootNode();

        boolean hasChanges = true;
        while (hasChanges) {
            System.out.println(node.toTree());
            ConstantOptimizationVisitor constantVisitor = new ConstantOptimizationVisitor();
            hasChanges = constantVisitor.visit(node, 0);
            System.out.println("------------------------");
        }

        return semanticsResult;
    }

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        return this.toOllir(semanticsResult, false);
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        ClassUnit classUnit = ollirResult.getOllirClass();
        RegisterAllocationOptimizer optimizer = new RegisterAllocationOptimizer(classUnit);
        optimizer.allocateRegisters(n_registers);

        return ollirResult;
    }

    public void setNumRegisters(int n) {
        n_registers = n;
    }

}
