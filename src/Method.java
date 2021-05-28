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

    public Symbol getLocalVariable(String varName) {
        for( Symbol symbol: localVariables) {
            if(symbol.getName().equals(varName)) return symbol;
        }
        for (Symbol symbol: parameters) {
            if(symbol.getName().equals(varName)) return symbol;
        }
        return null;
    }

    public Symbol getParameter(String varName) {
        for (Symbol symbol: parameters) {
            if(symbol.getName().equals(varName)) return symbol;
        }
        return null;
    }

    public int getParamNumber(String varName) {
        int i = 0;
        for (Symbol symbol: parameters) {
            if(symbol.getName().equals(varName)) return i;
            i++;
        }
        return -1;
    }

    public boolean containsParameter(String varName) {
        for (Symbol symbol: parameters) {
            if(symbol.getName().equals(varName)) return true;
        }
        return false;
    }

    public Symbol getLocalVariable(int index) {
        return localVariables.get(index);
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Method{" +
                "parameters=" + parameters +
                ", localVariables=" + localVariables +
                ", returnType=" + type +
                '}';
    }
}
