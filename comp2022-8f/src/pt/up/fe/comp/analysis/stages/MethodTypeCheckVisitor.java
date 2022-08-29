package pt.up.fe.comp.analysis.stages;
import pt.up.fe.comp.analysis.AnalysisUtils;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.analysis.JmmType;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MethodTypeCheckVisitor extends AJmmVisitor<List<Report>, JmmType> {
    String methodSignature;
    SymbolTable symbolTable;
    Map<Symbol, Boolean> localVariables; // Boolean denotes if the variable has been initialized or not
    List<Symbol> parametersAndFields; // Field initialization check is not implemented because it depends on the method call order

    public MethodTypeCheckVisitor(SymbolTable symbolTable, String methodSignature) {
        this.symbolTable = symbolTable;
        this.methodSignature = methodSignature;

        localVariables = new HashMap<>();
        for(var symbol : symbolTable.getLocalVariables(methodSignature)){
            localVariables.put(symbol, false);
        }
        parametersAndFields = new ArrayList<>();
        parametersAndFields.addAll(symbolTable.getParameters(methodSignature));
        parametersAndFields.addAll(symbolTable.getFields());

        addVisit(AstNode.INT_LITERAL, this::visitIntLiteral);
        addVisit(AstNode.ID, this::visitId);
        addVisit(AstNode.LENGTH_OP, this::visitLengthOp);
        addVisit(AstNode.BINARY_OP, this::visitBinaryOp);
        addVisit(AstNode.CLASS_METHOD, this::visitClassMethod);
        addVisit(AstNode.CONDITION, this::visitCondition);
        addVisit(AstNode.ASSIGNMENT, this::visitAssignment);
        addVisit(AstNode.ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(AstNode.ARRAY_INITIALIZATION, this::visitArrayInitialization);
        addVisit(AstNode.CLASS_INITIALIZATION, this::visitClassInitialization);
        addVisit(AstNode.BOOL, this::visitBool);
        addVisit(AstNode.UNARY_OP, this::visitUnaryOp);
        addVisit(AstNode.EXPRESSION_IN_PARENTHESES, this::visitExpressionInParentheses);
        addVisit(AstNode.ARRAY_ASSIGNMENT, this::visitArrayAssignment);
        addVisit(AstNode.ARGUMENT, this::visitArgument);
        addVisit(AstNode.RETURN_EXPRESSION, this::visitReturnExpression);
        setDefaultVisit(this::defaultVisit);
    }

    private JmmType defaultVisit(JmmNode node, List<Report> reports){
        for(var child : node.getChildren()){
            visit(child, reports);
        }
        return new JmmType(null, false);
    }

    private JmmType visitIntLiteral(JmmNode node, List<Report> reports){
        return new JmmType("int", false);
    }

    private Symbol getSymbolByName(String name){
        for(Symbol s : this.localVariables.keySet()){
            if(name.equals(s.getName())){
                return s;
            }
        }
        return getParameterOrField(name);
    }

    private Symbol getParameterOrField(String name) {
        if(methodSignature.equals("main")){
            // Can't use fields in static context
            for(Symbol s : symbolTable.getParameters("main")){
                if(name.equals(s.getName())){
                    return s;
                }
            }
        } else {
            for(Symbol s : this.parametersAndFields){
                if(name.equals(s.getName())){
                    return s;
                }
            }
        }
        return null;
    }

    // Same as getSymbolByName(String) but adds error reports if variable was not initialized
    private Symbol getSymbolByName(String name, JmmNode node, List<Report> reports){
        for(Symbol s : this.localVariables.keySet()){
            if(name.equals(s.getName())){
                if(this.localVariables.get(s) == false){ // Not initialized
                    reports.add(createSemanticError(node, "Variable " + name + " is not initialized"));
                }
                return s;
            }
        }

        return getParameterOrField(name);
    }

    private JmmType visitId(JmmNode node, List<Report> reports){
        String name = node.get("name");
        
        if(node.getJmmParent().getKind().equals("ClassMethod")){
            // If the node's parent is ClassMethod then id doesn't represent a variable name
            return new JmmType(null, false);
        }

        if(name.equals("this")){
            if(symbolTable.getSuper() != null){
                return new JmmType(symbolTable.getClassName(), false);
            } else {
                return new JmmType(symbolTable.getClassName(), false, true);
            }
        } else {
            Symbol symbol = getSymbolByName(name, node, reports);
            if(symbol == null){
                reports.add(createSemanticError(node, "Symbol " + name + " is not defined." ));
                return new JmmType("", false);
            }
            return new JmmType(symbol.getType());
        }
    }

    private JmmType visitLengthOp(JmmNode node, List<Report> reports){
        JmmNode child = node.getJmmChild(0);
        JmmType childType = visit(child, reports);
        if(!childType.isArray()){
            reports.add(createSemanticError(node, "Symbol doesn't support the .length op because it is not an array."));
            return new JmmType("", false);
        }
        return new JmmType("int", false);
    }

    private JmmType visitBinaryOp(JmmNode node, List<Report> reports){
        String op = node.get("op");
        JmmType firstChildType = visit(node.getJmmChild(0), reports);
        JmmType secondChildType = visit(node.getJmmChild(1), reports);

        JmmType boolType = new JmmType("boolean", false);
        JmmType intType = new JmmType("int",false);

        switch(op){
            case "AND":
                if(firstChildType.equals(boolType) && secondChildType.equals(boolType)){
                    return boolType;
                } else {
                    reports.add(createSemanticError(node, "Invalid types for '&&' op"));
                }
                break;
            case "LOW":
                if(firstChildType.equals(intType) && secondChildType.equals(intType)){
                    return boolType;
                } else {
                    reports.add(createSemanticError(node, "Invalid types for '<' op"));
                }
                break;
            case "ADD":
            case "SUB":
            case "MUL":
            case "DIV": 
                if(firstChildType.equals(intType) && secondChildType.equals(intType)){
                    return intType;
                }
                else{ 
                    reports.add(createSemanticError(node, "Invalid type for " + op ));
                }
        }
        return new JmmType("", false);
    }

    private JmmType visitClassMethod(JmmNode node, List<Report> reports){
        String methodName = node.get("name");

        JmmNode classIdNode = node.getJmmChild(0);
        String className = classIdNode.get("name");

        JmmNode argumentsNode = node.getJmmChild(1);

        boolean knownMethod = false;
        if(className.equals("this") || className.equals(symbolTable.getClassName())){
            if(methodSignature.equals("main")){
                reports.add(createSemanticError(node, "Cannot invoke non-static method from a static context"));
                return new JmmType("", false, false);
            }
            knownMethod = true;
        } else {
            Symbol symbol = getSymbolByName(className, node, reports);
            if(symbol != null){
                Type symbolType = symbol.getType();
                String symbolTypeName = symbolType.getName();
                if(symbolType.isArray()){
                    reports.add(createSemanticError(node, "Method cannot be invoked because " + className + " is an array"));
                    return new JmmType("", false, false);
                } else if(symbolTypeName.equals(symbolTable.getClassName())) {
                    knownMethod = true;
                }
            }
        }

        if(knownMethod){ // Method information is known because method is registered in the symbolTable
            List<Report> methodCallReports = new ArrayList<>();
            if(symbolTable.getMethods().contains(methodName)) {
                List<Symbol> methodParameters = symbolTable.getParameters(methodName);
                if (methodParameters.size() != argumentsNode.getNumChildren()) {
                    methodCallReports.add(createSemanticError(node, "Invalid number of arguments for method " + methodName + " expected " + methodParameters.size() + " arguments but got " + argumentsNode.getNumChildren() + " instead"));
                } else {
                    for (int i = 0; i < methodParameters.size(); ++i) {
                        Type parameterType = methodParameters.get(i).getType();
                        JmmType argumentType = visit(argumentsNode.getJmmChild(i), reports);
                        if (!argumentType.equals(parameterType)) {
                            methodCallReports.add(createSemanticError(node, "Argument type doesn't match required parameter type for method " + methodName));
                        }
                    }
                }
            } else {
                methodCallReports.add(createSemanticError(node, "Method " + methodName + " does not exist."));
            }

            if(symbolTable.getSuper() == null){ // Class doesn't extend another class
                if(methodCallReports.isEmpty()){ // There's been no errors detected
                    return new JmmType(symbolTable.getReturnType(methodName));
                } else {
                    reports.addAll(methodCallReports);
                    return new JmmType("", false, false);
                }
            }
            return new JmmType(null, false, true); // Assume the type is correct (method in super)
        }
        return new JmmType(null, false, true); // Assume the type is correct
    }

    private JmmType visitCondition(JmmNode node, List<Report> reports){
        JmmType childType = visit(node.getJmmChild(0), reports);
        if(!childType.equals(new JmmType("boolean", false))){
            reports.add(createSemanticError(node, "Condition is not a boolean"));
        }
        return new JmmType(null, false);
    }

    private JmmType visitAssignment(JmmNode node, List<Report> reports){
        JmmType childType = visit(node.getJmmChild(0), reports);
        Symbol symbol = getSymbolByName(node.get("name"));
        if(symbol == null){
            reports.add(createSemanticError(node, "Symbol " + node.get("name") + " hasn't been declared."));
            return new JmmType(null, false);
        }

        if(!AnalysisUtils.isAssignable(symbolTable, new JmmType(symbol.getType()), childType)){
            reports.add(createSemanticError(node, "Invalid assignment type for symbol " + symbol.getName()));
        }

        this.localVariables.computeIfPresent(symbol, (k, v) -> true); // Set variable as initialized in hashmap
        return new JmmType(null, false);
    }

    private JmmType visitArrayAssignment(JmmNode node, List<Report> reports){
        JmmType indexType = visit(node.getJmmChild(0), reports);
        if(!indexType.equals(new JmmType("int", false))){
            reports.add(createSemanticError(node, "Invalid type for array index"));
        }

        Symbol symbol = getSymbolByName(node.get("name"));
        if(symbol == null){
            reports.add(createSemanticError(node, "Symbol " + node.get("name") + " hasn't been declared."));
            return new JmmType(null, false);
        }
        Type symbolType = symbol.getType();
        JmmType assignType = visit(node.getJmmChild(1), reports);

        if(!symbolType.isArray()){
            reports.add(createSemanticError(node, "Symbol " + symbol.getName() + " is not an array"));
        }
        if(!assignType.equals(new JmmType(symbol.getType().getName(), false))){
            reports.add(createSemanticError(node, "Invalid type for array assignment"));
        }
        this.localVariables.computeIfPresent(symbol, (k, v) -> true);
        return new JmmType(null, false);
    }

    private JmmType visitArrayAccess(JmmNode node, List<Report> reports){
        String arrayName = node.getJmmChild(0).get("name");
        Symbol arraySymbol = getSymbolByName(arrayName, node, reports);

        if(arraySymbol == null){
            reports.add(createSemanticError(node, "Symbol " + arrayName + " not defined."));
            return new JmmType("", false);
        }

        JmmType type = new JmmType(arraySymbol.getType());

        if(!type.isArray()){
            reports.add(createSemanticError(node, "Symbol " + arrayName + " is not an array."));
        }
        if(!(visit(node.getJmmChild(1), reports).equals(new JmmType("int", false)))){
            reports.add(createSemanticError(node, "Invalid array index"));
        }
        return new JmmType(type.getName(), false);
    }

    private JmmType visitArrayInitialization(JmmNode node, List<Report> reports){
        JmmType childType = visit(node.getJmmChild(0), reports);
        if(!childType.equals(new JmmType("int", false))){
            reports.add(createSemanticError(node, "Invalid type for array size"));
        }
        return new JmmType("int", true);
    }

    private JmmType visitClassInitialization(JmmNode node, List<Report> reports){
        return new JmmType(node.get("name"), false);
    }

    private JmmType visitBool(JmmNode node, List<Report> reports){
        return new JmmType("boolean", false);
    }

    private JmmType visitUnaryOp(JmmNode node, List<Report> reports){
        if(!node.get("op").equals("NEG")){ // Only NEG is supported by the jmm grammar
            return new JmmType(null, false);
        }
        JmmType childType = visit(node.getJmmChild(0), reports);
        if(!childType.equals(new JmmType("boolean",false))){
            reports.add(createSemanticError(node, "Invalid type for NEG op"));
        }
        return childType;
    }

    private JmmType visitExpressionInParentheses(JmmNode node, List<Report> reports){
        return visit(node.getJmmChild(0), reports);
    }

    private JmmType visitArgument(JmmNode node, List<Report> reports){
        return visit(node.getJmmChild(0), reports);
    }

    private JmmType visitReturnExpression(JmmNode node, List<Report> reports){
        JmmType childType = visit(node.getJmmChild(0), reports);
        if(!childType.equals(symbolTable.getReturnType(this.methodSignature))){
            reports.add(createSemanticError(node, "Incompatible return type"));
        }
        return new JmmType(null, false);
    }

    private Report createSemanticError(JmmNode node, String message){
        return new Report(ReportType.ERROR, Stage.SEMANTIC,
                Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                message);
    }
}
