package pt.up.fe.comp.analysis.stages;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class ImportCheckVisitor extends AJmmVisitor<List<Report>, Boolean> {
    SymbolTable symbolTable;
    List<String> imports; // List of imported classes names (not fully qualified name)

    public ImportCheckVisitor(SymbolTable symbolTable) { 
        this.symbolTable = symbolTable;
        this.imports = new ArrayList<>();
        for(String name : symbolTable.getImports()){
            String[] splitImport = name.split("\\.");
            String className = splitImport[splitImport.length - 1]; 
            imports.add(className);
        }
        
        addVisit(AstNode.VAR_DECLARATION, this::visitVarDeclaration);
        addVisit(AstNode.MAIN_METHOD_DECLARATION, this::visitMainMethodDeclaration);
        addVisit(AstNode.INSTANCE_METHOD_DECLARATION, this::visitInstanceMethodDeclaration);
        addVisit(AstNode.CLASS_DECLARATION, this::visitClassDeclaration);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode node, List<Report> reports){
        for(var child : node.getChildren()){
            visit(child, reports);
        }
        return true;
    }

    private Boolean visitClassDeclaration(JmmNode node, List<Report> reports){
        Optional<String> baseClassName = node.getOptional("baseClassName");
        baseClassName.ifPresent(name -> {
            if(!imports.contains(name)){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                "Super class " + name + " has not been imported."));
            }
        });
        return defaultVisit(node, reports);
    }

    private Boolean visitVarDeclaration(JmmNode node, List<Report> reports){
        String type = node.get("type");
        boolean isArray = type.endsWith("[]");
        if (isArray) {
            type = type.substring(0, type.length() - 2);
        }
        if(type.equals("int") || type.equals("boolean") || type.equals("String")){
            return true;
        }
        if(!imports.contains(type) && !type.equals(symbolTable.getClassName())){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                "Class " + type + " has not been imported."));
        }
        return true;
    }

    private Boolean visitMainMethodDeclaration(JmmNode node, List<Report> reports){
        MethodImportCheckVisitor visitor = new MethodImportCheckVisitor(symbolTable, "main", imports);
        visitor.visit(node, reports);
        return true;
    }

    private Boolean visitInstanceMethodDeclaration(JmmNode node, List<Report> reports){
        String methodSignature = node.get("name");
        MethodImportCheckVisitor visitor = new MethodImportCheckVisitor(symbolTable, methodSignature, imports);
        visitor.visit(node, reports);
        return true;
    }
}
