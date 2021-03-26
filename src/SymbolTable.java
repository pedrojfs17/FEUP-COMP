import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class SymbolTable implements pt.up.fe.comp.jmm.analysis.table.SymbolTable {
    private List<String> imports = new ArrayList<>();
    private String className;
    private String superClass;
    private Type type;
    private List<String> fields = new ArrayList<>();
    private List<String> methods = new ArrayList<>();
    private List<Symbol> parameters = new ArrayList<>();
    private List<Symbol> localVariables = new ArrayList<>();

    public void setClassName(String className) {
        this.className = className;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public void setLocalVariables(List<Symbol> localVariables) {
        this.localVariables = localVariables;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public void setParameters(List<Symbol> parameters) {
        this.parameters = parameters;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public List<String> getImports() {
        return null;
    }

    @Override
    public String getClassName() {
        return null;
    }

    @Override
    public String getSuper() {
        return null;
    }

    @Override
    public List<Symbol> getFields() {
        return null;
    }

    @Override
    public List<String> getMethods() {
        return null;
    }

    @Override
    public Type getReturnType(String methodName) {
        return null;
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return null;
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return null;
    }
}
