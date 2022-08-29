package pt.up.fe.comp.analysis.table;
import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.analysis.JmmMethod;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;

public class MethodCollector extends AJmmVisitor<Boolean, Boolean> {
    public List<Report> reports;
    private JmmMethod method;

    public MethodCollector(JmmNode rootNode) {
        this.reports = new ArrayList();

        addVisit(AstNode.MAIN_METHOD_DECLARATION, this::visitMainMethodDeclaration);
        addVisit(AstNode.INSTANCE_METHOD_DECLARATION, this::visitMethodDeclaration);
        addVisit(AstNode.PARAMETER, this::visitParameter);
        addVisit(AstNode.VAR_DECLARATION, this::visitLocalVariable);

        visit(rootNode);
    }

    public JmmMethod getMethod() {
        return this.method;
    }

    public List<Report> getReports() {
        return reports;
    }

    private Boolean visitMainMethodDeclaration(JmmNode mainMethodDeclaration, Boolean dummy) {
        this.method = new JmmMethod("main", new Type("void", false));
        for (var child : mainMethodDeclaration.getChildren()) {
            visit(child);
        }
        return true;
    }

    private Boolean visitMethodDeclaration(JmmNode methodDeclaration, Boolean dummy) {
        String signature = methodDeclaration.get("name");
        String typeName = methodDeclaration.get("type");
        boolean isArray = typeName.endsWith("[]");
        if (isArray) {
            typeName = typeName.substring(0, typeName.length() - 2);
        }
        Type type = new Type(typeName, isArray);
        this.method = new JmmMethod(signature, type);
        for (var child : methodDeclaration.getChildren()) {
            visit(child);
        }
        return true;
    }
    
    private Symbol varDeclarationToSymbol(JmmNode varDeclaration){
        String type = varDeclaration.get("type");
        boolean isArray = type.endsWith("[]");
        if (isArray) {
            type = type.substring(0, type.length() - 2);
        }
        return new Symbol(new Type(type, isArray), varDeclaration.get("name"));
    }
    
    private Boolean visitLocalVariable(JmmNode varDeclaration, Boolean dummy) {
        if(!alreadyDeclared(varDeclaration, varDeclaration.get("name"))){
            this.method.addLocalVariable(this.varDeclarationToSymbol(varDeclaration));
            return true;
        }
        return false;
    }

    private Boolean visitParameter(JmmNode parameterDeclaration, Boolean dummy) {
        if(!alreadyDeclared(parameterDeclaration, parameterDeclaration.get("name"))){
            this.method.addParameter(this.varDeclarationToSymbol(parameterDeclaration));
            return true;
        }
        return false;
    }

    private boolean alreadyDeclared(JmmNode node, String name){
        for(var s : this.method.getLocalVariables()){
            if(s.getName().equals(name)){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Variable " + name + " has already been declared"));
                return true;
            }
        }
        for(var s : this.method.getParameters()){
            if(s.getName().equals(name)){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")), "Variable " + name + " has already been declared"));
                return true;
            }
        }
        return false;
    }
}