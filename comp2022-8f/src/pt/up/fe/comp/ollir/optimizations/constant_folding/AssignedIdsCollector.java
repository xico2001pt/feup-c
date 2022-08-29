package pt.up.fe.comp.ollir.optimizations.constant_folding;

import java.util.List;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class AssignedIdsCollector extends AJmmVisitor<List<String>, Boolean> {
    public AssignedIdsCollector(){
        this.setDefaultVisit(this::defaultVisit);
        this.addVisit(AstNode.ASSIGNMENT, this::visitAssignment);
    }

    private Boolean defaultVisit(JmmNode node, List<String> ids){
        for(var child : node.getChildren()){
            visit(child, ids);
        }
        return true;
    }

    private Boolean visitAssignment(JmmNode node, List<String> ids){
        ids.add(node.get("name"));
        return true;
    }
}
