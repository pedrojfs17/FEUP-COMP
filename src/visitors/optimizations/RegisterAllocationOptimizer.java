package visitors.optimizations;

import graph.Graph;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;

import java.util.*;

public class RegisterAllocationOptimizer {
    private final ClassUnit classUnit;
    private final LivenessAnalysis livenessAnalyzer;

    public RegisterAllocationOptimizer(ClassUnit classUnit) {
        this.classUnit = classUnit;
        this.livenessAnalyzer = new LivenessAnalysis();
    }

    public void allocateRegisters(int n) {
        try {
            // Example of what you can do with the OLLIR class
            classUnit.checkMethodLabels(); // check the use of labels in the OLLIR loaded
            classUnit.buildCFGs(); // build the CFG of each method
            classUnit.buildVarTables(); // build the table of variables for each method
        } catch (OllirErrorException e) {
            e.printStackTrace();
            return;
        }
        for (Method method : classUnit.getMethods()) {
            System.out.println("semantic.Method: " + method.getMethodName());

            ArrayList<HashMap<Node, BitSet>> liveRanges = livenessAnalyzer.livenessAnalysis(method);
            Graph varGraph = new Graph(liveRanges, method);

            OllirAccesser.setVarTable(method, varGraph.graphColoring(n));
        }
    }

}