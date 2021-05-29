import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class ConstantOptimizationVisitor extends AJmmVisitor<Integer, Boolean> {
    private HashMap<String, Map.Entry<String, String>> constants;

    public ConstantOptimizationVisitor() {
        super();
        this.constants = new HashMap<>();

        addVisit("ASSIGNMENT", this::dealWithAssignment);
        addVisit("OPERATION", this::dealWithOperation);
        addVisit("AND", this::dealWithAnd);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode node, Integer index) {
        boolean changes = false;

        for (int i = 0; i < node.getNumChildren(); i++) {
            JmmNode child = node.getChildren().get(i);
            changes = changes || visit(child, i);
            checkIdentifier(node, child, i);
        }

        return changes;
    }

    private Boolean dealWithAssignment(JmmNode jmmNode, Integer index) {
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode value = jmmNode.getChildren().get(1);

        boolean changes = visit(value, 1);

        if (value.getKind().equals("INT"))
            constants.put(identifier.get("name"), new AbstractMap.SimpleEntry<>("INT", value.get("value")));
        else if (value.getKind().equals("TRUE") || value.getKind().equals("FALSE"))
            constants.put(identifier.get("name"), new AbstractMap.SimpleEntry<>(value.getKind(), value.getKind()));
        else if (value.getKind().equals("IDENTIFIER") && constants.get(identifier.get("name")) != null)
            constants.remove(identifier.get("name"));

        return changes || checkIdentifier(jmmNode, value, 1);
    }

    private Boolean dealWithOperation(JmmNode jmmNode, Integer index) {
        JmmNode lhs = jmmNode.getChildren().get(0);
        JmmNode rhs = jmmNode.getChildren().get(1);

        if (lhs.getKind().equals("INT") && rhs.getKind().equals("INT")) {
            jmmNode.removeChild(lhs);
            jmmNode.removeChild(rhs);
            JmmNode newNode = new JmmNodeImpl("INT");
            newNode.put("value", String.valueOf(evaluateOperation(jmmNode.get("op"), lhs.get("value"), rhs.get("value"))));
            JmmNode parent = jmmNode.getParent();
            parent.removeChild(jmmNode);
            parent.add(newNode, index);
            return true;
        }

        return checkIdentifier(jmmNode, lhs, 0) || checkIdentifier(jmmNode, rhs, 1);
    }

    private Boolean dealWithAnd(JmmNode jmmNode, Integer index) {
        JmmNode lhs = jmmNode.getChildren().get(0);
        JmmNode rhs = jmmNode.getChildren().get(1);

        if ((lhs.getKind().equals("TRUE") || lhs.getKind().equals("FALSE"))
                && (rhs.getKind().equals("TRUE") || rhs.getKind().equals("FALSE"))) {
            jmmNode.removeChild(lhs);
            jmmNode.removeChild(rhs);
            JmmNode newNode = new JmmNodeImpl(evaluateAnd(lhs.getKind(), rhs.getKind()));
            JmmNode parent = jmmNode.getParent();
            parent.removeChild(jmmNode);
            parent.add(newNode, index);
            return true;
        }

        return checkIdentifier(jmmNode, lhs, 0) || checkIdentifier(jmmNode, rhs, 1);
    }

    private boolean checkIdentifier(JmmNode parentNode, JmmNode child, int index) {
        if (child.getKind().equals("IDENTIFIER") && constants.get(child.get("name")) != null) {
            parentNode.removeChild(child);
            Map.Entry<String, String> constant = constants.get(child.get("name"));
            JmmNode newNode = new JmmNodeImpl(constant.getKey());
            if (constant.getKey().equals("INT"))
                newNode.put("value", constant.getValue());
            parentNode.add(newNode, index);
            return true;
        }
        return false;
    }

    private int evaluateOperation(String operation, String lhs, String rhs) {
        switch(operation) {
            case "+" : return Integer.parseInt(lhs) + Integer.parseInt(rhs);
            case "-" : return Integer.parseInt(lhs) - Integer.parseInt(rhs);
            case "*" : return Integer.parseInt(lhs) / Integer.parseInt(rhs);
            case "/" : return Integer.parseInt(lhs) * Integer.parseInt(rhs);
        }
        return 0;
    }

    private String evaluateAnd(String lhs, String rhs) {
        if (lhs.equals("TRUE")) {
            return rhs;
        }
        return lhs;
    }
}
