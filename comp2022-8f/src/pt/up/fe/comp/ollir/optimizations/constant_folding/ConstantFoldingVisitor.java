package pt.up.fe.comp.ollir.optimizations.constant_folding;

import java.util.Optional;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ConstantFoldingVisitor extends AJmmVisitor<Boolean, Boolean> {
    public ConstantFoldingVisitor(){
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
        ConstantFoldingMethodVisitor visitor = new ConstantFoldingMethodVisitor();
        visitor.visit(node, true);
        return visitor.wasUpdated();
    }
}
