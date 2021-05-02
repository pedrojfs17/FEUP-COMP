import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SymbolTable implements pt.up.fe.comp.jmm.analysis.table.SymbolTable {
    private List<String> imports = new ArrayList<>();
    private String className;
    private String superClass;
    private List<Symbol> fields = new ArrayList<>();
    private HashMap<String,Method> methods = new HashMap<>();
    public ArrayList<Report> reports = new ArrayList<>();

    public SymbolTable(String result) {
        this.buildSymbolTable(result);
    }

    private void buildSymbolTable(String result) {
        String[] arr = result.split("\n");
        for (int i =0; i<arr.length; i++) {
            String node = arr[i];
            System.out.println(node);
            if(node.contains("IMPORT"))this.addImport(node);
            else if(node.contains("CLASS_DECLARATION"))this.addClassName(node);
            else if(node.contains("METHOD_DECLARATION") || node.contains("MAIN")) i = this.addMethod(arr,i);
            else if(node.contains("VAR_DECLARATION"))this.addField(node);
        }
    }

    public ArrayList<Report> getReports() {
        return reports;
    }

    private int addMethod(String[] arr, int i) {
        Method method = new Method();
        String node = arr[i];
        String name;
        if(node.contains("MAIN")) {
            name = "main";
            method.setType(new Type("void",false));
        }
        else {
            name = (node.substring(node.indexOf("name=")+5,node.lastIndexOf(","))).trim();
            method.setType(this.parseType(node.substring(node.lastIndexOf("=")+1,node.indexOf("]")).trim()));
        }

        i++;
        node = arr[i];

        while (!(node.contains("METHOD_DECLARATION") || node.contains("MAIN")) && i < arr.length-1) {
            System.out.println(node);
            if(node.contains("PARAMETER"))this.addParamOrVar(method,node, true);
            else if(node.contains("VAR_DECLARATION"))this.addParamOrVar(method,node,false);
            i++;
            node=arr[i];
        }
        this.methods.put(name,method);
        return --i;
    }


    private void addParamOrVar(Method method, String node, boolean isParam) {
        String name = (node.substring(node.indexOf("name=")+5,node.lastIndexOf(","))).trim();
        Type type = parseType((node.substring(node.lastIndexOf("=")+1,node.lastIndexOf("]"))));
        if(isParam)method.addParameters(new Symbol(type,name));
        else method.addLocalVariables(new Symbol(type,name));
    }

    public boolean isParam(String methodName, String variableName) {
        return this.getMethod(methodName).getParameter(variableName) != null;
    }

    private void addField(String node) {
        String name = (node.substring(node.indexOf("name=")+5,node.lastIndexOf(","))).trim();
        Type type = parseType((node.substring(node.lastIndexOf("type=")+5,node.lastIndexOf("]"))));
        Symbol field = new Symbol(type,name);
        //if(this.fields.contains(field))
            //reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, "Variable "+name+" already declared"));
        //else
        this.fields.add(field);
    }

    public Type parseType(String node) {
        return node.contains("array") ? new Type((node.substring(0,node.indexOf(" "))),true) : new Type(node,false);
    }

    private void addClassName(String node) {
        if(node.contains("super")) {
            this.superClass = (node.substring(node.indexOf("super=")+6,node.indexOf(",")));
        }
        this.className = (node.substring(node.indexOf("name=")+5,node.lastIndexOf("]")));
    }

    private void addImport(String node) {
        this.imports.add(node.substring(node.indexOf("name=")+5,node.lastIndexOf("]")));
    }

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    public Symbol getField(String fieldName) {
        for (Symbol symbol: fields) {
            if (symbol.getName().equals(fieldName)) return symbol;
        }
        return null;
    }

    @Override
    public List<String> getMethods() {
        List<String> methodNames = new ArrayList<>();
        methodNames.addAll(methods.keySet());
        return methodNames;
    }

    public Method getMethod(String methodName) {
        return this.methods.get(methodName);
    }

    public Symbol getVariable(String variableName, String methodName) {
        Symbol globalVar = this.getField(variableName);
        Symbol localVar = this.getMethod(methodName).getLocalVariable(variableName);
        return localVar != null ? localVar : globalVar;
    }

    public boolean isGlobal(String variableName) {
        return this.getField(variableName) !=null;
    }

    public int getGlobalIndex(String variableName) {
        for(int i=0; i<fields.size();i++) {
            if(fields.get(i).getName().equals(variableName)) return i+1;
        }
        return -1;
    }

    public boolean checkVariableInImports(String variableName) {
        for (String imp : imports) {
            if (imp.equals(variableName)) return true;
        }
        return false;
    }

    @Override
    public Type getReturnType(String methodName) {
        return methods.get(methodName).getType();
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return methods.get(methodName).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return methods.get(methodName).getLocalVariables();
    }
}
