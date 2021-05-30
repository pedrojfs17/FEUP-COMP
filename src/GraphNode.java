import java.util.*;

public class GraphNode {
    String varname;
    int originalReg;
    ArrayList<GraphEdge> edges;

    public GraphNode(String name, int reg) {
        varname = name;
        originalReg = reg;
        edges = new ArrayList<>();
    }

    public void addEdge(GraphEdge edge) {
        edges.add(edge);
    }

    public void removeEdge(GraphEdge edge) {
        edges.remove(edge);
    }

    public int getNumEdges() {
        return edges.size();
    }

    public int getOriginalReg() {
        return originalReg;
    }

    public ArrayList<GraphEdge> removeEdges(){
        ArrayList<GraphEdge> copy = edges;
        for (GraphEdge edge: edges) {
            removeEdge(edge);
        }
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return originalReg == graphNode.originalReg && Objects.equals(varname, graphNode.varname) && Objects.equals(edges, graphNode.edges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(varname, originalReg, edges);
    }

    @Override
    public String toString() {
        return "GraphNode{" +
                "varname='" + varname + '\'' +
                ", originalReg=" + originalReg +
                ", edges=" + edges +
                '}';
    }
}
