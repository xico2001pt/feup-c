package pt.up.fe.comp.ollir.optimizations.if_while_removal;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class IfWhileRemoverMethodVisitor extends AJmmVisitor<Boolean, Boolean> {
    public IfWhileRemoverMethodVisitor(){
        this.setDefaultVisit(this::defaultVisit);
        this.addVisit(AstNode.WHILE_STATEMENT, this::visitWhileStatement);
        this.addVisit(AstNode.IF_STATEMENT, this::visitIfStatement);
    }
    
    public boolean defaultVisit(JmmNode node, Boolean dummy){
        boolean updated = false;
        for(JmmNode child : node.getChildren()){
            updated |= visit(child, true);
        }
        return updated;
    }

    public boolean visitIfStatement(JmmNode node, Boolean dummy){
        JmmNode conditionNode = node.getJmmChild(0);
        JmmNode conditionValueNode = conditionNode.getJmmChild(0);
        JmmNode ifNode = node.getJmmChild(1);
        JmmNode elseNode = node.getJmmChild(2);

        if(conditionValueNode.getKind().equals("Bool")){
            Boolean value = conditionValueNode.get("value").equals("true");
            if(value == true){
                node.replace(ifNode);
            } else {
                node.replace(elseNode);
            }
            return true;
        }
        return false;
    }

    public boolean visitWhileStatement(JmmNode node, Boolean dummy){
        JmmNode conditionNode = node.getJmmChild(0);
        JmmNode conditionValueNode = conditionNode.getJmmChild(0);

        if(conditionValueNode.getKind().equals("Bool")){
            if(conditionValueNode.get("value").equals("false")){
                JmmNode parent = node.getJmmParent();
                parent.removeJmmChild(node);
                return true;
            }
        }
        return false;
    }
}
