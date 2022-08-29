package pt.up.fe.comp.analysis.table;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SymbolTableCollector extends AJmmVisitor<SymbolTableBuilder, Boolean> {
    List<Report> reports;

    public SymbolTableCollector() {
        this.reports = new ArrayList<>();
        addVisit(AstNode.PROGRAM, this::visitProgram);
        addVisit(AstNode.IMPORT_DECL, this::visitImportDecl);
        addVisit(AstNode.CLASS_DECLARATION, this::visitClassDeclaration);
        addVisit(AstNode.VAR_DECLARATION, this::visitVarDeclaration);
        addVisit(AstNode.MAIN_METHOD_DECLARATION, this::visitMethodDeclaration);
        addVisit(AstNode.INSTANCE_METHOD_DECLARATION, this::visitMethodDeclaration);
    }

    public Boolean visitProgram(JmmNode program, SymbolTableBuilder symbolTable) {
        for (var child : program.getChildren()) {
            visit(child, symbolTable);
        }
        return true;
    }

    private Boolean visitImportDecl(JmmNode importDecl, SymbolTableBuilder symbolTable) {
        var packageString = importDecl.get("name");
        var subpackagesString = importDecl.getChildren().stream()
                .map(id -> id.get("name"))
                .collect(Collectors.joining("."));
        var importString = packageString + (subpackagesString.isEmpty() ? "" : "." + subpackagesString);
        symbolTable.addImport(importString);
        return true;
    }

    private Boolean visitClassDeclaration(JmmNode classDeclaration, SymbolTableBuilder symbolTable) {
        symbolTable.setClassName(classDeclaration.get("className"));
        Optional<String> baseClassName = classDeclaration.getOptional("baseClassName");
        baseClassName.ifPresent(symbolTable::setSuperName);

        for (var child : classDeclaration.getChildren()) {
            visit(child, symbolTable);
        }

        return true;
    }

    private Boolean visitVarDeclaration(JmmNode varDeclaration, SymbolTableBuilder symbolTable){
        String type = varDeclaration.get("type");
        boolean isArray = type.endsWith("[]");
        if (isArray) {
            type = type.substring(0, type.length() - 2);
        }
        symbolTable.addField(new Symbol(new Type(type, isArray), varDeclaration.get("name")));
        return true;
    }

    private Boolean visitMethodDeclaration(JmmNode methodDeclaration, SymbolTableBuilder symbolTable) {
        MethodCollector collector = new MethodCollector(methodDeclaration);
        symbolTable.addMethod(collector.getMethod());
        this.reports.addAll(collector.getReports());
        return true;
    }

    public List<Report> getReports(){
        return this.reports;
    }
}