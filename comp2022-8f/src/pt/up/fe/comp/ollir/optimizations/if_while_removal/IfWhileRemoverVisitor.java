package pt.up.fe.comp.ollir.optimizations.if_while_removal;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class IfWhileRemoverVisitor extends AJmmVisitor<Boolean, Boolean> {
    public IfWhileRemoverVisitor(){
        addVisit(AstNode.MAIN_METHOD_DECLARATION, this::visitMethodDeclaration);
        addVisit(AstNode.INSTANCE_METHOD_DECLARATION, this::visitMethodDeclaration);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode node, Boolean dummy){
        boolean updated = false;
        for(var child : node.getChildren()){
            updated |= visit(child, true);
        }
        return updated;
    }

    private Boolean visitMethodDeclaration(JmmNode node, Boolean dummy){
        IfWhileRemoverMethodVisitor visitor = new IfWhileRemoverMethodVisitor();
        return visitor.visit(node, true);
    }    
}
