package pt.up.fe.comp.ollir.optimizations.constant_folding;

import java.lang.StackWalker.Option;
import java.util.Optional;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

public class ConstantFoldingMethodVisitor extends AJmmVisitor<Boolean, Optional<Integer>> {
    Boolean updated;

    public ConstantFoldingMethodVisitor(){
        this.updated = false;
        
        addVisit(AstNode.INT_LITERAL, this::visitIntLiteral);
        addVisit(AstNode.BOOL, this::visitBool);
        addVisit(AstNode.UNARY_OP, this::visitUnaryOp);
        addVisit(AstNode.BINARY_OP, this::visitBinaryOp);
        addVisit(AstNode.EXPRESSION_IN_PARENTHESES, this::visitExpressionInParentheses);
        setDefaultVisit(this::defaultVisit);
    }

    private Optional<Integer> defaultVisit(JmmNode node, Boolean dummy){
        for(var stmt : node.getChildren()) {
            visit(stmt, true);
        }
        return Optional.empty();
    }

    private Optional<Integer> visitIntLiteral(JmmNode node, Boolean dummy){
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

    private Optional<Integer> visitBool(JmmNode node, Boolean dummy){
        String stringValue = node.get("value");
        Integer value = stringValue.equals("true") ? 1 : 0;
        return Optional.of(value);
    }

    private Optional<Integer> visitUnaryOp(JmmNode node, Boolean dummy){
        JmmNode child = node.getJmmChild(0);
        Optional<Integer> optional = visit(child);
        
        if(optional.isPresent()){
            node.removeJmmChild(child);
            JmmNode newChild = new JmmNodeImpl("Bool");
            newChild.put("value",  optional.get() == 1 ? "false" : "true"); // !
            node.replace(newChild);
            updated = true;
        }
        return optional;
    }

    private Optional<Integer> visitBinaryOp(JmmNode node, Boolean dummy){        
        Optional<Integer> firstChild = visit(node.getJmmChild(0));
        Optional<Integer> secondChild = visit(node.getJmmChild(1));

        if(firstChild.isEmpty() || secondChild.isEmpty()){
            return applyBinaryOpArithmeticSimplifications(node, firstChild, secondChild);
        }

        String op = node.get("op");
        JmmNode newNode = null;
        Integer value = 0;
        switch(op){
            case "AND":
                newNode = new JmmNodeImpl("Bool");
                value = firstChild.get() & secondChild.get();
                break;
            case "LOW":
                newNode = new JmmNodeImpl("Bool");
                value = firstChild.get() < secondChild.get() ? 1 : 0;
                break;
            case "ADD":
                newNode = new JmmNodeImpl("IntLiteral");
                value = firstChild.get() + secondChild.get();
                newNode.put("type", "decimal");
                break;
            case "SUB":
                newNode = new JmmNodeImpl("IntLiteral");
                value = firstChild.get() - secondChild.get();
                newNode.put("type", "decimal");
                break;
            case "MUL":
                newNode = new JmmNodeImpl("IntLiteral");
                value = firstChild.get() * secondChild.get();
                newNode.put("type", "decimal");
                break;
            case "DIV": 
                newNode = new JmmNodeImpl("IntLiteral");
                value = firstChild.get() / secondChild.get();
                newNode.put("type", "decimal");
                break;
        }

        if(node != null){
            if(newNode.getKind().equals("Bool")){
                newNode.put("value", value==1 ? "true" : "false");
            } else {
                newNode.put("value", Integer.toString(value));
            }
            node.replace(newNode);
            updated = true;
            return Optional.of(value);
        }
        return Optional.empty();
    }

    private Optional<Integer> visitExpressionInParentheses(JmmNode node, Boolean dummy){
        JmmNode child = node.getJmmChild(0);
        Optional<Integer> result = visit(child);
        node.replace(child);
        updated = true;
        return result;
    }

    // Only one of the childs should be present
    private Optional<Integer> applyBinaryOpArithmeticSimplifications(JmmNode node, Optional<Integer> firstChild, Optional<Integer> secondChild){
        Integer value;
        boolean isFirstChild = true;
        if(firstChild.isPresent()){
            value = firstChild.get();
        } else if(secondChild.isPresent()) {
            value = secondChild.get();
            isFirstChild = false;
        } else {
            return Optional.empty();
        }


        String op = node.get("op");
        JmmNode newNode;
        switch(op){
            case "AND":
                if(value == 0){ // false && x = false
                    newNode = new JmmNodeImpl("Bool");
                    newNode.put("value", "false");
                    node.replace(newNode);
                    updated = true;
                    return Optional.of(0);
                }
                break;
            case "ADD":
                if(value == 0){ // x+0 = x
                    node.replace(node.getJmmChild(isFirstChild ? 1 : 0));
                    updated = true;
                }
                break;
            case "SUB":
                if(value == 0 && !isFirstChild){ // x - 0 can be simplified, 0 - x cannot
                    node.replace(node.getJmmChild(1));
                    updated = true;
                }
                break;
            case "MUL":
                if(value == 0){ // x*0 = 0
                    newNode = new JmmNodeImpl("IntLiteral");
                    newNode.put("type", "decimal");
                    newNode.put("value", "0");
                    node.replace(newNode);
                    updated = true;
                    return Optional.of(0);
                }
                if(value == 1){ // x*1 = x
                    node.replace(node.getJmmChild(isFirstChild ? 1 : 0));
                    updated = true;
                }
                break;
            case "DIV": 
                if(value == 1 && !isFirstChild){ // x / 1 = x
                    node.replace(node.getJmmChild(0));
                    updated = true;
                } 
                if(value == 0 && isFirstChild){ // 0 / x = 0
                    newNode = new JmmNodeImpl("IntLiteral");
                    newNode.put("type", "decimal");
                    newNode.put("value", "0");
                    node.replace(newNode);
                    updated = true;
                    return Optional.of(0);
                }
                break;
        }
        return Optional.empty();
    }

    public Boolean wasUpdated() {
        return updated;
    }
    
}
