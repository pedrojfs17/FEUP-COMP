package visitors.optimizations;

import pt.up.fe.comp.jmm.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.*;

public class ConstantOptimizationVisitor extends AJmmVisitor<Integer, Boolean> {
    private final HashMap<String, Map.Entry<String, String>> constants;
    private boolean check;

    public ConstantOptimizationVisitor() {
        super();
        this.constants = new HashMap<>();
        this.check = true;

        addVisit("ASSIGNMENT", this::dealWithAssignment);
        addVisit("OPERATION", this::dealWithOperation);
        addVisit("LESS", this::dealWithLess);
        addVisit("AND", this::dealWithAnd);
        addVisit("VAR_DECLARATION", this::dealWithVarDeclaration);
        addVisit("METHOD_DECLARATION", this::dealWithMethodDeclaration);
        addVisit("IF", this::dealWithIf);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode node, Integer index) {
        boolean changes = false;

        for (int i = 0; i < node.getNumChildren(); i++) {
            JmmNode child = node.getChildren().get(i);
            changes = changes || visit(child, i);
            if (this.check && !insideIfOrWhile(child))
                changes = changes || checkIdentifier(node, child, i);
        }

        return changes;
    }

    private Boolean dealWithVarDeclaration(JmmNode node, Integer index) {
        constants.remove(node.get("name"));
        return false;
    }

    private Boolean dealWithMethodDeclaration(JmmNode node, Integer index) {
        constants.clear();
        return defaultVisit(node, index);
    }

    private Boolean dealWithIf(JmmNode jmmNode, Integer index) {
        JmmNode condition = jmmNode.getChildren().get(0);

        if (condition.getKind().equals("TRUE")) {
            JmmNode parent = jmmNode.getParent();
            JmmNode elseNode = parent.getChildren().get(index + 1);
            parent.removeChild(jmmNode);
            parent.removeChild(elseNode);
            for (int i = 1; i < jmmNode.getNumChildren(); i++) {
                JmmNode child = jmmNode.getChildren().get(i);
                parent.add(child, index + i - 1);
            }
            return true;
        } else if (condition.getKind().equals("FALSE")) {
            JmmNode parent = jmmNode.getParent();
            JmmNode elseNode = parent.getChildren().get(index + 1);
            parent.removeChild(jmmNode);
            parent.removeChild(elseNode);
            for (int i = 0; i < elseNode.getNumChildren(); i++) {
                JmmNode child = elseNode.getChildren().get(i);
                parent.add(child, index + i);
            }
            return true;
        }

        boolean changes = visit(condition, 0);
        this.check = false;
        for (int i = 1; i < jmmNode.getNumChildren(); i++) {
            JmmNode child = jmmNode.getChildren().get(i);
            changes = changes || visit(child, i);
        }
        this.check = true;
        return changes;
    }

    private Boolean dealWithAssignment(JmmNode jmmNode, Integer index) {
        JmmNode identifier = jmmNode.getChildren().get(0);
        JmmNode value = jmmNode.getChildren().get(1);

        boolean changes = visit(value, 1);

        String var;
        if (identifier.getKind().equals("ARRAY_ACCESS")) {
            if(!identifier.getChildren().get(1).getKind().equals("IDENTIFIER") && !identifier.getChildren().get(1).getKind().equals("INT")) return false;
            var = identifier.getChildren().get(0).get("name") + "[" +
                    (identifier.getChildren().get(1).getKind().equals("IDENTIFIER") ? identifier.getChildren().get(1).get("name") : identifier.getChildren().get(1).get("value"))
                    + "]";
        } else
            var = identifier.get("name");

        if (insideIfOrWhile(jmmNode)) {
            constants.remove(var);
        } else {
            if (value.getKind().equals("INT")) {
                constants.put(var, new AbstractMap.SimpleEntry<>("INT", value.get("value")));
            } else if (value.getKind().equals("TRUE") || value.getKind().equals("FALSE"))
                constants.put(var, new AbstractMap.SimpleEntry<>(value.getKind(), value.getKind()));
            else if (value.getKind().equals("IDENTIFIER") && constants.get(var) != null)
                constants.remove(var);
        }

        if (this.check)
            changes = changes || checkIdentifier(jmmNode, value, 1);

        return changes;
    }

    private Boolean dealWithOperation(JmmNode jmmNode, Integer index) {
        JmmNode lhs = jmmNode.getChildren().get(0);
        JmmNode rhs = jmmNode.getChildren().get(1);

        boolean changes;

        if (this.check && lhs.getKind().equals("INT") && rhs.getKind().equals("INT")) {
            JmmNode parent = jmmNode.getParent();
            parent.removeChild(jmmNode);
            JmmNode newNode = new JmmNodeImpl("INT");
            newNode.put("value", String.valueOf(evaluateOperation(jmmNode.get("op"), lhs.get("value"), rhs.get("value"))));
            parent.add(newNode, index);
            changes = true;
        } else {
            changes = visit(lhs, 0) || visit(rhs, 1);
        }

        if (this.check && !insideIfOrWhile(jmmNode)) {
            changes = changes || checkIdentifier(jmmNode, lhs, 0) || checkIdentifier(jmmNode, rhs, 1);
        }

        return changes;
    }

    private Boolean dealWithAnd(JmmNode jmmNode, Integer index) {
        JmmNode lhs = jmmNode.getChildren().get(0);
        JmmNode rhs = jmmNode.getChildren().get(1);

        boolean changes;

        if (this.check && (lhs.getKind().equals("TRUE") || lhs.getKind().equals("FALSE"))
                && (rhs.getKind().equals("TRUE") || rhs.getKind().equals("FALSE"))) {
            JmmNode parent = jmmNode.getParent();
            parent.removeChild(jmmNode);
            JmmNode newNode = new JmmNodeImpl(evaluateAnd(lhs.getKind(), rhs.getKind()));
            parent.add(newNode, index);
            changes = true;
        } else {
            changes = visit(lhs, 0) || visit(rhs, 1);
        }

        if (this.check && !insideIfOrWhile(jmmNode)) {
            changes = changes || checkIdentifier(jmmNode, lhs, 0) || checkIdentifier(jmmNode, rhs, 1);
        }

        return changes;
    }

    private Boolean dealWithLess(JmmNode jmmNode, Integer index) {
        JmmNode lhs = jmmNode.getChildren().get(0);
        JmmNode rhs = jmmNode.getChildren().get(1);

        boolean changes;

        if (this.check && lhs.getKind().equals("INT") && rhs.getKind().equals("INT")) {
            JmmNode parent = jmmNode.getParent();
            parent.removeChild(jmmNode);
            JmmNode newNode = new JmmNodeImpl(evaluateLess(lhs.get("value"), rhs.get("value")));
            parent.add(newNode, index);
            changes = true;
        } else {
            changes = visit(lhs, 0) || visit(rhs, 1);
        }

        if (this.check && !insideIfOrWhile(jmmNode)) {
            changes = changes || checkIdentifier(jmmNode, lhs, 0) || checkIdentifier(jmmNode, rhs, 1);
        }

        return changes;
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
        switch (operation) {
            case "+":
                return Integer.parseInt(lhs) + Integer.parseInt(rhs);
            case "-":
                return Integer.parseInt(lhs) - Integer.parseInt(rhs);
            case "*":
                return Integer.parseInt(lhs) * Integer.parseInt(rhs);
            case "/":
                return Integer.parseInt(lhs) / Integer.parseInt(rhs);
        }
        return 0;
    }

    private String evaluateLess(String lhs, String rhs) {
        if (Integer.parseInt(lhs) < Integer.parseInt(rhs)) return "TRUE";
        return "FALSE";
    }

    private String evaluateAnd(String lhs, String rhs) {
        if (lhs.equals("TRUE")) {
            return rhs;
        }
        return lhs;
    }

    private boolean insideIfOrWhile(JmmNode jmmNode) {
        try {
            return (getAncestor(jmmNode, "WHILE").get().getKind().equals("WHILE")
                    || getAncestor(jmmNode, "ELSE").get().getKind().equals("ELSE")
            );
        } catch (Exception e) {
            return false;
        }
    }

    private Optional<JmmNode> getAncestor(JmmNode jmmNode, String specificScope) {
        return jmmNode.getAncestor(specificScope).isPresent() ? jmmNode.getAncestor(specificScope) : jmmNode.getAncestor("PROGRAM");
    }
}
