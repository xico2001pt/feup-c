package pt.up.fe.comp.ollir.optimizations.constant_propagation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;


import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.ollir.optimizations.constant_folding.AssignedIdsCollector;

public class ConstantPropagationMethodVisitor extends AJmmVisitor<Boolean, Boolean> {
    Map<String, JmmNode> constantMap;
    SymbolTable symbolTable;
    String methodSignature;

    public ConstantPropagationMethodVisitor(SymbolTable symbolTable, String methodSignature){
        this.constantMap = new HashMap<>();
        this.symbolTable = symbolTable;
        this.methodSignature = methodSignature;

        addVisit(AstNode.ID, this::visitId);
        addVisit(AstNode.ASSIGNMENT, this::visitAssignment);
        addVisit(AstNode.CLASS_METHOD, this::visitClassMethod);
        addVisit(AstNode.WHILE_STATEMENT, this::visitWhileStatement);
        addVisit(AstNode.IF_STATEMENT, this::visitIfStatement);
        setDefaultVisit(this::defaultVisit);
    }

    private boolean defaultVisit(JmmNode node, Boolean dummy){
        boolean updated = false;
        for(var stmt : node.getChildren()) {
            updated |= visit(stmt, true);
        }
        return updated;
    }

    private boolean visitAssignment(JmmNode node, Boolean dummy){
        String name = node.get("name");
        JmmNode childNode = node.getJmmChild(0);
        boolean childUpdated = visit(childNode);
        childNode = node.getJmmChild(0);
        String childNodeKind = childNode.getKind();
        if(childNodeKind.equals("IntLiteral") || childNodeKind.equals("Bool")){
            JmmNode newNode = new JmmNodeImpl(childNodeKind);
            newNode.put("value", childNode.get("value"));
            childNode.getOptional("type").ifPresent(t -> newNode.put("type", t));
            constantMap.put(name, newNode);
        } else {
            if(constantMap.containsKey(name)){
                constantMap.remove(name);
            }
        }
        return childUpdated;
    }

    private boolean visitId(JmmNode node, Boolean dummy){
        String name = node.get("name");
        
        if(node.getJmmParent().getKind().equals("ClassMethod")){
            // If the node's parent is ClassMethod then id doesn't represent a variable name
            return false;
        }

        if(constantMap.containsKey(name)){
            node.replace(constantMap.get(name));
            return true;
        }

        return false;
    }

    private boolean visitClassMethod(JmmNode node, Boolean dummy){
        boolean updated = false;

        JmmNode arguments = node.getJmmChild(1);
        for(int i = 0; i < arguments.getNumChildren(); ++i){
            updated |= visit(arguments.getJmmChild(i));
            JmmNode argument = arguments.getJmmChild(i);
            if(argument.getKind().equals("Id")){
                String name = argument.get("name");
                if(this.constantMap.containsKey(name)){ // Can't guarantee there's no side effects in method
                    this.constantMap.remove(name);
                }
            }
        }
        for(var symbol : this.symbolTable.getFields()){
            boolean existsLocalVar = false;
            for(var localVar : this.symbolTable.getLocalVariables(this.methodSignature)){
                if(symbol.getName().equals(localVar.getName())){
                    existsLocalVar = true;
                    continue;
                }
            }
            if(!existsLocalVar){
                this.constantMap.remove(symbol.getName());
            }
        }

        return updated;
    }

    private boolean visitWhileStatement(JmmNode node, Boolean dummy){
        JmmNode conditionChild = node.getJmmChild(0);
        JmmNode conditionValueChild = conditionChild.getJmmChild(0);
        JmmNode statements = node.getJmmChild(1);

        if(conditionValueChild.getKind().equals("Id")){
            String name = conditionValueChild.get("name");
            if(this.constantMap.containsKey(name)){
                if(!containsVariableUsage(statements, name)){ // Variable is not altered inside the while
                    visit(conditionChild); // This will swap the node with a const
                    return true;
                }  
            }
        }

        ConditionVisitor conditionVisitor = new ConditionVisitor(constantMap);
        Optional<Integer> result = conditionVisitor.visit(conditionValueChild);
        result.ifPresent(i -> {
            if(i == 1){
                node.put("doWhile", "true");
            }
        });

        AssignedIdsCollector usedAssignmentsCollector = new AssignedIdsCollector();
        List<String> assignments = new ArrayList<>();
        usedAssignmentsCollector.visit(statements, assignments);
        for(String id : assignments){
            this.constantMap.remove(id);
        }

        Map<String, JmmNode> backupConstantMap = new HashMap<>();
        backupConstantMap.putAll(this.constantMap);
        boolean updated = this.visit(statements);
        this.constantMap = backupConstantMap; // 

        return updated;
    }

    private boolean visitIfStatement(JmmNode node, Boolean dummy){
        JmmNode conditionChild = node.getJmmChild(0);
        boolean updated = visit(conditionChild);

        Map<String, JmmNode> ifConstantMap = new HashMap<>();
        Map<String, JmmNode> elseConstantMap = new HashMap<>();
        ifConstantMap.putAll(this.constantMap);
        elseConstantMap.putAll(this.constantMap);

        this.constantMap = ifConstantMap;
        updated |= visit(node.getJmmChild(1));
        this.constantMap = elseConstantMap;
        updated |= visit(node.getJmmChild(2));

        this.constantMap = new HashMap<>();
        for(Entry<String, JmmNode> entry : ifConstantMap.entrySet()){ // Keep only the constants that exist and have same value across the two if branches
            if(elseConstantMap.containsKey(entry.getKey())){
                String value1 = entry.getValue().get("value");
                String value2 = elseConstantMap.get(entry.getKey()).get("value");
                if(value1.equals(value2)){
                    this.constantMap.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return updated;
    }

    private boolean containsVariableUsage(JmmNode node, String variableName){
        if(node.getKind().equals("Id")){
            return node.get("name").equals(variableName);
        }
        for(JmmNode child : node.getChildren()){
            if(containsVariableUsage(child, variableName)){
                return true;
            }
        }
        return false;
    }
}
