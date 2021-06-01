package graph;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.VarScope;

import java.util.*;

public class Graph {
    HashMap<Integer, GraphNode> nodes;
    int minRegisters;
    boolean staticMethod;
    HashMap<String, Descriptor> varTable;

    public Graph(ArrayList<HashMap<Node, BitSet>> liveRanges, Method method) {
        nodes = new HashMap<>();

        varTable = method.getVarTable();
        staticMethod = method.isStaticMethod();
        minRegisters = staticMethod ? 0 : 1;

        for(String name: varTable.keySet()) {
            Descriptor d = varTable.get(name);
            if (d.getScope() == VarScope.PARAMETER || d.getScope() == VarScope.FIELD)
                minRegisters++;
            else
                nodes.put(d.getVirtualReg(), new GraphNode(name, d.getVirtualReg()));
        }

        for (HashMap<Node, BitSet> range: liveRanges) {
            for (Node node : range.keySet()) {
                BitSet bitset = range.get(node);
                List<Integer> indexes = new ArrayList<>();
                for (int i = 0; i < bitset.length(); i++) {
                    if (bitset.get(i)) indexes.add(i);
                }


                for (int i = 0; i < indexes.size() - 1; i++) {
                    GraphNode nodeEdited = nodes.get(indexes.get(i));
                    for (int j = i + 1; j < indexes.size(); j++) {
                        GraphNode secondNode = nodes.get(indexes.get(j));
                        nodeEdited.addEdge(new GraphEdge(nodeEdited, secondNode));
                        secondNode.addEdge(new GraphEdge(secondNode, nodeEdited));
                    }
                }
            }
        }
    }

    public HashMap<String, Descriptor> graphColoring(int k) {
        if (k < minRegisters) {
            //If the value of n is not enough to have the required local variables of the JVM sufficient
            //to store the variables of the Java-- method, the compiler shall abort execution and shall
            //report an error and indicate the minimum number of JVM local variables required.
            System.out.println("Insufficient registers to store this method's variables");
            return graphColoring(k+1);
        }

        Stack<GraphNode> stack = new Stack<>();

        while (!nodes.isEmpty()) {
            Iterator<Map.Entry<Integer, GraphNode>> it = nodes.entrySet().iterator();
            while (it.hasNext()) {
                GraphNode node = it.next().getValue();
                if (node.getNumEdges() < k) {
                    stack.push(node);
                    node.setActive(false);
                    it.remove();
                }
            }
        }

        HashMap<Integer, ArrayList<String>> colors = new HashMap<>();
        for (int i = minRegisters; i < k; i++)
            colors.put(i, new ArrayList<>());

        HashMap<String, Descriptor> newVarTable = new HashMap<>();
        while (!stack.isEmpty()) {
            GraphNode node = stack.pop();
            node.setActive(true);
            nodes.put(node.getOriginalReg(), node);

            boolean colored = false;
            for (Integer reg : colors.keySet()) {
                boolean canColor = true;
                for (String var : node.getLinkedNodes()) {
                    if (colors.get(reg).contains(var))
                        canColor = false;
                }

                if (canColor) {
                    colors.get(reg).add(node.getName());
                    Descriptor old = varTable.get(node.getName());
                    newVarTable.put(node.getName(), new Descriptor(old.getScope(), reg, old.getVarType()));
                    colored = true;
                    break;
                }
            }

            if (!colored) {
                System.out.println(k + " -- Insufficient registers to store this method's variables");
                while (!stack.isEmpty()) {
                    GraphNode n = stack.pop();
                    n.setActive(true);
                    nodes.put(n.getOriginalReg(), n);
                }
                return graphColoring(k+1);
            }
        }

        int reg = staticMethod ? 0 : 1;
        for (String name: varTable.keySet()) {
            Descriptor d = varTable.get(name);
            if (d.getScope() == VarScope.PARAMETER || d.getScope() == VarScope.FIELD) {
                newVarTable.put(name, new Descriptor(d.getScope(), reg, d.getVarType()));
                reg++;
            }
        }

        ArrayList<Integer> used_reg = new ArrayList<>();
        for (Descriptor d: newVarTable.values()) {
            if(!used_reg.contains(d.getVirtualReg()))
                used_reg.add(d.getVirtualReg());
        }
        if (!used_reg.contains(0))
            used_reg.add(0);

        System.out.println("Allocated " + used_reg.size() + " registers");
        return newVarTable;
    }
}
