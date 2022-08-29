package pt.up.fe.comp.ollir.optimizations.unused_assignment_removing;

import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

public class UnusedAssignmentRemoverMethodVisitor extends AJmmVisitor<Boolean, Boolean> {
    String methodSignature;
    Map<String, Boolean> usageMap;
    Map<String, JmmNode> nameToAssignmentNode;

    public UnusedAssignmentRemoverMethodVisitor(String methodSignature, SymbolTable symbolTable){
        this.methodSignature = methodSignature;
        
        this.usageMap = new HashMap<>();
        this.nameToAssignmentNode = new HashMap<>();
        for(Symbol s : symbolTable.getLocalVariables(methodSignature)){
            this.usageMap.put(s.getName(), false);
        }

        addVisit(AstNode.ID, this::visitId);
        addVisit(AstNode.ASSIGNMENT, this::visitAssignment);
        addVisit(AstNode.ARRAY_ASSIGNMENT, this::visitArrayAssignment);
        setDefaultVisit(this::defaultVisit);
    }

    private boolean defaultVisit(JmmNode node, Boolean dummy){
        for(var stmt : node.getChildren()) {
            visit(stmt, true);
        }
        return true;
    }

    private boolean visitAssignment(JmmNode node, Boolean dummy){
        String name = node.get("name");
        this.nameToAssignmentNode.put(name, node);
        visit(node.getJmmChild(0));
        return false;
    }

    private boolean visitId(JmmNode node, Boolean dummy){
        String name = node.get("name");
        
        if(usageMap.containsKey(name)){
            usageMap.put(name, true);
        }
        return true;
    }

    private boolean visitArrayAssignment(JmmNode node, Boolean dummy){
        String name = node.get("name");
        
        if(usageMap.containsKey(name)){
            usageMap.put(name, true);
        }
        return true;
    }

    // Removes all unused assignment nodes that were detected during the visit
    // Returns true if there was any removal, false otherwise
    public Boolean removeUnusedNodes(){
        boolean updated = false;
        for(Map.Entry<String, Boolean> entry : this.usageMap.entrySet()){
            if(!entry.getValue()){ // Unused
                updated = true;

                String variableName = entry.getKey();
                if(nameToAssignmentNode.containsKey(variableName)){ // Just for safety
                    JmmNode assignmentNode = nameToAssignmentNode.get(variableName);
                    JmmNode parent = assignmentNode.getJmmParent();
                    parent.removeJmmChild(assignmentNode);
                }
            }
        }
        return updated;
    }
}
