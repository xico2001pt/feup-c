package pt.up.fe.comp.analysis.stages;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.report.Report;
import java.util.List;

public class TypeCheckVisitor extends AJmmVisitor<List<Report>, Boolean> {
    SymbolTable symbolTable;

    public TypeCheckVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        addVisit(AstNode.MAIN_METHOD_DECLARATION, this::visitMainMethodDeclaration);
        addVisit(AstNode.INSTANCE_METHOD_DECLARATION, this::visitInstanceMethodDeclaration);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode node, List<Report> reports){
        for(var child : node.getChildren()){
            visit(child, reports);
        }
        return true;
    }

    private Boolean visitMainMethodDeclaration(JmmNode node, List<Report> reports){
        MethodTypeCheckVisitor visitor = new MethodTypeCheckVisitor(symbolTable, "main");
        visitor.visit(node, reports);
        return true;
    }

    private Boolean visitInstanceMethodDeclaration(JmmNode node, List<Report> reports){
        String methodSignature = node.get("name");
        MethodTypeCheckVisitor visitor = new MethodTypeCheckVisitor(symbolTable, methodSignature);
        visitor.visit(node, reports);
        return true;
    }
}
