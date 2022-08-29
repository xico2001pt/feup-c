package pt.up.fe.comp.ast;

import pt.up.fe.comp.Node;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

public class LineColAnnotator extends PreorderJmmVisitor<Boolean, Boolean> {
    public LineColAnnotator(){
        setDefaultVisit(this::annotate);
    }

    private Boolean annotate(JmmNode node, Boolean dummy){
        var baseNode = (Node) node;
        node.put("line", Integer.toString(baseNode.getBeginLine()));
        node.put("col", Integer.toString(baseNode.getBeginColumn()));
        return true;
    }
}
