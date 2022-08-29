package pt.up.fe.comp.analysis.table;

import pt.up.fe.comp.analysis.JmmMethod;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SymbolTableBuilder implements SymbolTable {
    List<String> importList;
    String className;
    String superName;
    List<Symbol> fields;
    Map<String, JmmMethod> methods;    // methodSignature -> Method Class

    public SymbolTableBuilder() {
        this.importList = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.methods = new HashMap<>();
    }

    public List<String> getImports(){
        return this.importList;
    }

    public void addImport(String importName){
        this.importList.add(importName);
    }

    public String getClassName(){
        return this.className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSuper(){
        return this.superName;
    }

    public void setSuperName(String superName) {
        this.superName = superName;
    }
    
    public List<Symbol> getFields(){
        return this.fields;
    }

    public void addField(Symbol field) {
        this.fields.add(field);
    }

    public List<String> getMethods(){
        return new ArrayList<>(new ArrayList<>(this.methods.keySet()));
    }

    public void addMethod(JmmMethod method) {
        this.methods.put(method.getSignature(), method);
    }

    public Type getReturnType(String methodSignature) {
        return this.methods.get(methodSignature).getReturnType();
    }

    public List<Symbol> getParameters(String methodSignature) {
        return this.methods.get(methodSignature).getParameters();
    }

    public List<Symbol> getLocalVariables(String methodSignature){
        return this.methods.get(methodSignature).getLocalVariables();
    }
}