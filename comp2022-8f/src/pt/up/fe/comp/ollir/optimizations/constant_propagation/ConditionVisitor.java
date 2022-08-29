package pt.up.fe.comp.ollir.optimizations.constant_propagation;

import java.util.Map;
import java.util.Optional;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ConditionVisitor extends AJmmVisitor<Boolean, Optional<Integer>> {
    Map<String, JmmNode> constantMap;

    public ConditionVisitor(Map<String, JmmNode> constantMap){
        this.constantMap = constantMap;

        this.addVisit(AstNode.INT_LITERAL, this::visitIntLiteral);
        this.addVisit(AstNode.BOOL, this::visitBool);
        this.addVisit(AstNode.ID, this::visitId);
        this.addVisit(AstNode.UNARY_OP, this::visitUnaryOp);
        this.addVisit(AstNode.BINARY_OP, this::visitBinaryOp);
        this.setDefaultVisit(this::defaultVisit);
    }

    public Optional<Integer> defaultVisit(JmmNode node, Boolean dummy){
        for(var child : node.getChildren()){
            visit(child, true);
        }
        return Optional.empty();
    }

    public Optional<Integer> visitIntLiteral(JmmNode node, Boolean dummy) {
        String type = node.get("type");
        String stringValue = node.get("value");
        int value = 0;
        switch(type){
            case "decimal": value = Integer.parseInt(stringValue);
                break;
            case "binary": value = Integer.parseInt(stringValue, 2);
                break;
            case "octal": value = Integer.parseInt(stringValue, 8);
                break;
            case "hexadecimal": value = Integer.parseInt(stringValue, 16);
                break;
        }
        return Optional.of(value);
    }

    public Optional<Integer> visitBool(JmmNode node, Boolean dummy){
        Integer value = node.get("value").equals("true") ? 1 : 0;
        return Optional.of(value);
    }

    public Optional<Integer> visitId(JmmNode node, Boolean dummy){
        String idName = node.get("name");
        if(this.constantMap.containsKey(idName)){
            return visit(this.constantMap.get(idName));
        }
        return Optional.empty();
    }

    public Optional<Integer> visitUnaryOp(JmmNode node, Boolean dummy){
        Optional<Integer> child = visit(node.getJmmChild(0));
        if(child.isEmpty()){
            return Optional.empty();
        } else {
            Integer value = child.get();
            return Optional.of(1 - value);
        }
    }

    public Optional<Integer> visitBinaryOp(JmmNode node, Boolean dummy){
        String op = node.get("op");
        
        Optional<Integer> firstChild = visit(node.getJmmChild(0));
        Optional<Integer> secondChild = visit(node.getJmmChild(1));

        if(firstChild.isEmpty() || secondChild.isEmpty()){
            return Optional.empty();
        }
        Integer value = 0;
        switch(op){
            case "AND":
                value = firstChild.get() & secondChild.get();
                break;
            case "LOW":
                value = firstChild.get() < secondChild.get() ? 1 : 0;
                break;
            case "ADD":
                value = firstChild.get() + secondChild.get();
                break;
            case "SUB":
                value = firstChild.get() - secondChild.get();
                break;
            case "MUL":
                value = firstChild.get() * secondChild.get();
                break;
            case "DIV": 
                value = firstChild.get() / secondChild.get();
                break;
        }
        return Optional.of(value);
    }
}
