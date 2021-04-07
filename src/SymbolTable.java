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
    public ArrayList<Report> reports = new ArrayList<Report>();

    public SymbolTable(String result) {
        this.buildSymbolTable(result);
    }

    private void buildSymbolTable(String result) {
        String[] arr = result.split("\n");
        for (int i =0; i<arr.length; i++) {
            String node = arr[i];
            if(node.contains("IMPORT"))this.addImport(node);
            else if(node.contains("CLASS_DECLARATION"))this.addClassName(node);
            else if(node.contains("METHOD_DECLARATION") || node.contains("MAIN")) i = this.addMethod(arr,i);
            else if(node.contains("VAR_DECLARATION"))this.addField(node);
        }
        System.out.println(imports);
        System.out.println(className);
        System.out.println(superClass);
        System.out.println(fields);
        System.out.println(methods);
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
            name = (node.substring(node.indexOf("=")+1,node.indexOf(","))).trim();
            method.setType(this.parseType(node.substring(node.lastIndexOf("=")+1,node.indexOf("]")).trim()));
        }

        i++;
        node = arr[i];

        while (!(node.contains("METHOD_DECLARATION") || node.contains("MAIN")) && i < arr.length-1) {
            if(node.contains("PARAMETER"))this.addParamOrVar(method,node, true);
            else if(node.contains("VAR_DECLARATION"))this.addParamOrVar(method,node,false);
            else if(node.contains("ASSIGNMENT"))this.verifyAssignment(method,node);
            i++;
            node=arr[i];
        }
        if(this.methods.containsKey(name))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, "Method "+name+" already declared"));
        else
            this.methods.put(name,method);
        return --i;
    }

    private void verifyAssignment(Method method, String node) {
        System.out.println("aaaaaaaaa");
        String var = (node.substring(node.indexOf("=")+1,node.indexOf(","))).trim();
        if(!method.getLocalVarNames().contains(var))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, "Variable "+var+" isn't declared"));
        String[] assignment = (node.substring(node.lastIndexOf("=")+1,node.lastIndexOf("]"))).trim().split("[+ -*/&&]+");
        System.out.println(var+" assigned "+ Arrays.toString(assignment));
        for (String s : assignment) {
            if(s.equals("true")||s.equals("false")||s.equals("this"))
                continue;
            try {
                Integer.parseInt(s);
            } catch (Exception e) {
                continue;
            }
            if(!method.getLocalVarNames().contains(s))
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, "Variable "+s+" isn't declared"));
        }
    }

    private void addParamOrVar(Method method, String node, boolean isParam) {
        String name = (node.substring(node.indexOf("=")+1,node.indexOf(","))).trim();
        Type type = parseType((node.substring(node.lastIndexOf("=")+1,node.lastIndexOf("]"))));
        if(isParam)method.addParameters(new Symbol(type,name));
        else method.addLocalVariables(new Symbol(type,name));
    }

    private void addField(String node) {
        String name = (node.substring(node.indexOf("=")+1,node.indexOf(","))).trim();
        Type type = parseType((node.substring(node.lastIndexOf("=")+1,node.lastIndexOf("]"))));
        Symbol field = new Symbol(type,name);
        if(this.fields.contains(field))
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, 0, "Variable "+name+" already declared"));
        else
            this.fields.add(new Symbol(type,name));
    }

    private Type parseType(String node) {
        return node.contains("array") ? new Type((node.substring(0,node.indexOf(" "))),true) : new Type(node,false);
    }

    private void addClassName(String node) {
        if(node.contains(",")) {
            this.className = (node.substring(node.indexOf("=")+1,node.lastIndexOf(",")));
            this.superClass = (node.substring(node.lastIndexOf("=")+1,node.lastIndexOf("]")));
        }
        else this.className = (node.substring(node.indexOf("=")+1,node.lastIndexOf("]")));

    }

    private void addImport(String node) {
        this.imports.add(node.substring(node.indexOf("=")+1,node.lastIndexOf("]")));
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

    @Override
    public List<String> getMethods() {
        List<String> methodNames = new ArrayList<>();
        methodNames.addAll(methods.keySet());
        return methodNames;
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
