package pt.up.fe.comp.ollir;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.stream.Collectors;

public class OllirGenerator extends AJmmVisitor<Boolean, Integer> {
    private final StringBuilder code;
    private final SymbolTable symbolTable;

    public OllirGenerator(SymbolTable symbolTable){
        this.code = new StringBuilder();
        this.symbolTable = symbolTable;

        addVisit(AstNode.PROGRAM, this::visitProgram);
        addVisit(AstNode.CLASS_DECLARATION, this::visitClassDecl);
        addVisit(AstNode.MAIN_METHOD_DECLARATION, this::visitMainMethodDecl);
        addVisit(AstNode.INSTANCE_METHOD_DECLARATION, this::visitInstanceMethodDecl);
    }

    public String getCode(){
        return code.toString();
    }

    private Integer visitProgram(JmmNode node, Boolean dummy){
        for(String importString : symbolTable.getImports()){
            code.append("import ").append(importString).append(";\n");
        }

        for(var child : node.getChildren()){
            visit(child);
        }
        return 0;
    }

    private Integer visitClassDecl(JmmNode node, Boolean dummy){
        code.append("public ").append(symbolTable.getClassName());
        var superClass = symbolTable.getSuper();
        if(superClass != null){
            code.append(" extends ").append(superClass);
        }

        code.append(" {\n");

        for(Symbol s : symbolTable.getFields()){
            code.append(".field private ").append(OllirUtils.getCode(s)).append(";\n");
        }

        code.append(".construct ").append(symbolTable.getClassName())
            .append("().V{\n  invokespecial(this, \"<init>\").V ;\n}\n\n");
        
        for(var child : node.getChildren()){
            visit(child);
        }

        code.append("}\n");

        return 0;
    }

    private Integer visitMainMethodDecl(JmmNode node, Boolean dummy){
        code.append(".method public static main(args.array.String).V {\n");
        generateMethodStatements(node, "main");
        code.append("ret.V;\n}\n\n");
        return 0;
    }

    private Integer visitInstanceMethodDecl(JmmNode node, Boolean dummy){
        String methodSignature = node.get("name");
        code.append(".method public ");
        code.append(methodSignature).append("(");

        List<Symbol> params = symbolTable.getParameters(methodSignature);
        String paramCode = params.stream()
                .map(OllirUtils::getCode)
                .collect(Collectors.joining(", "));

        code.append(paramCode).append(").");
        code.append(OllirUtils.getCode(symbolTable.getReturnType(methodSignature)));

        code.append("{\n");
        generateMethodStatements(node, methodSignature);
        code.append("}\n\n");
        return 0;
    }

    private void generateMethodStatements(JmmNode node, String methodSignature) {
        int lastParamIndex = -1;
        for(int i = 0; i < node.getNumChildren(); ++i){
            if(node.getJmmChild(i).getKind().equals("Parameter")) {
                lastParamIndex = i;
            }
        }

        String ollirReturnType = OllirUtils.getCode(symbolTable.getReturnType(methodSignature));
        var stmts = node.getChildren().subList(lastParamIndex+1, node.getNumChildren());

        OllirStatementGenerator stmtGenerator = new OllirStatementGenerator(symbolTable, methodSignature);
        for(var stmt : stmts){
            OllirStatement ollirStatement = stmtGenerator.visit(stmt, new OllirGeneratorHint(methodSignature, ollirReturnType, false));
            code.append(ollirStatement.getCodeBefore());
        }
    }
}
