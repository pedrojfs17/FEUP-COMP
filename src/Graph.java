import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.VarScope;

import java.util.*;

public class Graph {
    HashMap<Integer, GraphNode> nodes;
    int minRegisters;

    public Graph(HashMap<Node, BitSet> in, HashMap<Node, BitSet> out, Method method) {
        nodes = new HashMap<>();

        HashMap<String, Descriptor> varTable = method.getVarTable();
        minRegisters = method.isStaticMethod() ? 0 : 1;

        for(String varname: varTable.keySet()) {
            Descriptor d = varTable.get(varname);
            if (d.getScope() == VarScope.PARAMETER || d.getScope() == VarScope.FIELD)
                minRegisters++;
            else
                nodes.put(d.getVirtualReg(), new GraphNode(varname, d.getVirtualReg()));
        }

        for (Node node: in.keySet()) {
            BitSet bitset = in.get(node);
            List<Integer> indexes = new ArrayList<>();
            for (int i = 0; i < bitset.length(); i++) {
                if (bitset.get(i)) indexes.add(i);
            }


            for(int i=0; i<indexes.size()-1;i++) {
                GraphNode nodeEdited = nodes.get(indexes.get(i));
                for (int j=i+1;j<indexes.size();j++){
                    GraphNode secondNode = nodes.get(indexes.get(j));
                    nodeEdited.addEdge(new GraphEdge(nodeEdited,secondNode));
                    secondNode.addEdge(new GraphEdge(secondNode,nodeEdited));
                    nodes.replace(indexes.get(j),secondNode);
                }
                nodes.replace(indexes.get(i),nodeEdited);
            }

        }
    }

    public void addNode(GraphNode node) {
        //nodes.put(node);
    }

    public void graphColoring(int k) {
        Stack<GraphNode> stack = new Stack<>();
        for(GraphNode node: nodes.values()) {
            if(node.getNumEdges()<k){
                stack.push(node);
                for(GraphEdge edge: node.removeEdges()) {
                    nodes.get(edge.getSecond().getOriginalReg()).removeEdge(new GraphEdge(edge.getSecond(),node));
                }
            }
            System.out.println(nodes);
        }

    }
}
