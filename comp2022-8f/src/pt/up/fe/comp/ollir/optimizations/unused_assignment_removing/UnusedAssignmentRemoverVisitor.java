package pt.up.fe.comp.ollir.optimizations.unused_assignment_removing;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class UnusedAssignmentRemoverVisitor extends AJmmVisitor<Boolean, Boolean> {
    SymbolTable symbolTable;

    public UnusedAssignmentRemoverVisitor(SymbolTable symbolTable){
        this.symbolTable = symbolTable;
        addVisit(AstNode.MAIN_METHOD_DECLARATION, this::visitMainMethodDeclaration);
        addVisit(AstNode.INSTANCE_METHOD_DECLARATION, this::visitInstanceMethodDeclaration);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode node, Boolean dummy){
        boolean updated = false;
        for(var child : node.getChildren()){
            updated |= visit(child, true);
        }
        return updated;
    }

    private Boolean visitMainMethodDeclaration(JmmNode node, Boolean dummy){
        UnusedAssignmentRemoverMethodVisitor visitor = new UnusedAssignmentRemoverMethodVisitor("main", symbolTable);
        visitor.visit(node, true);
        return visitor.removeUnusedNodes();
    }

    private Boolean visitInstanceMethodDeclaration(JmmNode node, Boolean dummy){
        String methodSignature = node.get("name");
        UnusedAssignmentRemoverMethodVisitor visitor = new UnusedAssignmentRemoverMethodVisitor(methodSignature, symbolTable);
        visitor.visit(node, true);
        return visitor.removeUnusedNodes();
    }    
}
