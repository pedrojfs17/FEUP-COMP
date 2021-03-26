import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class Method {
    private List<Symbol> parameters = new ArrayList<>();
    private List<Symbol> localVariables = new ArrayList<>();
    private Type type;

    public void setType(Type type) {
        this.type = type;
    }

    public void addParameters(Symbol parameter) {
        this.parameters.add(parameter);
    }

    public void addLocalVariables(Symbol localVariable) {
        this.localVariables.add(localVariable);
    }

    public List<Symbol> getLocalVariables() {
        return localVariables;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public Type getType() {
        return type;
    }
}
