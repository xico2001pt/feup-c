package pt.up.fe.comp.analysis.stages;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import java.util.List;

public class MethodImportCheckVisitor extends AJmmVisitor<List<Report>, Boolean> {
    String methodSignature;
    SymbolTable symbolTable;
    List<String> imports;

    public MethodImportCheckVisitor(SymbolTable symbolTable, String methodSignature, List<String> imports) {
        this.symbolTable = symbolTable;
        this.methodSignature = methodSignature;
        this.imports = imports;
        addVisit(AstNode.PARAMETER, this::visitDeclaration);
        addVisit(AstNode.VAR_DECLARATION, this::visitDeclaration);
        addVisit(AstNode.CLASS_METHOD, this::visitClassMethod);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode node, List<Report> reports){
        for(var child : node.getChildren()){
            visit(child, reports);
        }
        return true;
    }

    private Boolean visitClassMethod(JmmNode node, List<Report> reports){
        String name = node.getJmmChild(0).get("name");

        if(!(imports.contains(name) || name.equals("this")
                || name.equals(symbolTable.getClassName()) || name.equals(symbolTable.getSuper()))){
            // Not static call, name refers to a variable
            for(Symbol s : symbolTable.getLocalVariables(methodSignature)){
                if(s.getName().equals(name)){
                    return true;
                }
            }
            for(Symbol s : symbolTable.getParameters(methodSignature)){
                if(s.getName().equals(name)){
                    return true;
                }
            }
            for(Symbol s : symbolTable.getFields()){
                if(s.getName().equals(name)){
                    return true;
                }
            }
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                "Class " + name + " has not been imported."));
        }
        return true;
    }

    private Boolean visitDeclaration(JmmNode node, List<Report> reports){
        String type = node.get("type");
        boolean isArray = type.endsWith("[]");
        if (isArray) {
            type = type.substring(0, type.length() - 2);
        }
        if(type.equals("int") || type.equals("boolean") || type.equals("String")){
            return true;
        }
        if(!imports.contains(type) && !type.equals(symbolTable.getClassName())){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                "Class " + type + " has not been imported."));
        }
        return true;
    }

}