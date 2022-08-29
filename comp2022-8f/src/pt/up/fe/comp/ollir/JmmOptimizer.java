package pt.up.fe.comp.ollir;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.ollir.optimizations.constant_folding.ConstantFoldingMethodVisitor;
import pt.up.fe.comp.ollir.optimizations.constant_folding.ConstantFoldingVisitor;
import pt.up.fe.comp.ollir.optimizations.constant_propagation.ConstantPropagationVisitor;
import pt.up.fe.comp.ollir.optimizations.if_while_removal.IfWhileRemoverVisitor;
import pt.up.fe.comp.ollir.optimizations.register_allocation.GraphColoringSolver;
import pt.up.fe.comp.ollir.optimizations.register_allocation.InterferenceGraphCreator;
import pt.up.fe.comp.ollir.optimizations.register_allocation.LivenessAnalyser;
import pt.up.fe.comp.ollir.optimizations.register_allocation.VariableSplitter;
import pt.up.fe.comp.ollir.optimizations.register_allocation.Web;
import pt.up.fe.comp.ollir.optimizations.unused_assignment_removing.UnusedAssignmentRemoverVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.OllirErrorException;

public class JmmOptimizer implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        OllirGenerator ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());
        try {
            ollirGenerator.visit(semanticsResult.getRootNode());
        } catch(Exception e){
            // Might occur due to unsupported features like method overloading
            List<Report> reports = new ArrayList<>();
            reports.add(new Report(ReportType.ERROR, Stage.LLIR, -1, "OLLIR parse exception occurred."));
            throw(e);

        }
        String ollirCode = ollirGenerator.getCode();

        System.out.println("OLLIR code:\n");
        printOllirCode(ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    // Prints ollircode with indentation
    private void printOllirCode(String ollirCode){
        int indent = 0;
        boolean indentNextLine = false;
        for(char c : ollirCode.toCharArray()){
            if(c == '\n'){
                System.out.println();
                indentNextLine = true;
            } else if(c == '}'){
                indent--;
                System.out.print(" ".repeat(indent*2));
                System.out.print(c);
                indentNextLine = false;
            } else {
                if(indentNextLine){
                    System.out.print(" ".repeat(indent*2));
                    indentNextLine = false;
                }
                System.out.print(c);
            }
            if(c == '{')
                indent++;
        }
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        Map<String, String> config = semanticsResult.getConfig();
        
        if(config.getOrDefault("optimizeAll", "false").equals("true")){
            astOptimizeAll(semanticsResult);
        } else if(config.getOrDefault("optimize", "false").equals("true")){
            astOptimizeBasic(semanticsResult);
        } 

        return semanticsResult;
    }

    private void astOptimizeAll(JmmSemanticsResult semanticsResult){
        JmmNode rootNode = semanticsResult.getRootNode();

        boolean updated;
        int i = 1;
        do {
            updated = false;

            System.out.println("\nOptimization round " + i);
            ConstantPropagationVisitor constantPropagationVisitor = new ConstantPropagationVisitor(semanticsResult.getSymbolTable());
            boolean result = constantPropagationVisitor.visit(rootNode);
            updated |= result;
            System.out.println("Constant propagation - " + result);
            
            ConstantFoldingVisitor constantFoldingVisitor = new ConstantFoldingVisitor();
            result = constantFoldingVisitor.visit(rootNode);
            updated |= result;
            System.out.println("Constant folding - " + result);

            IfWhileRemoverVisitor ifWhileRemoverVisitor = new IfWhileRemoverVisitor();
            result = ifWhileRemoverVisitor.visit(rootNode);
            updated |= result;
            System.out.println("If/While removal - " + result);

            i++;
        } while(updated);
    
        UnusedAssignmentRemoverVisitor unusedAssignmentRemoverVisitor = new UnusedAssignmentRemoverVisitor(semanticsResult.getSymbolTable());
        unusedAssignmentRemoverVisitor.visit(rootNode);
    }

    private void astOptimizeBasic(JmmSemanticsResult semanticsResult){
        boolean updated = false;
        do {
            ConstantPropagationVisitor constantPropagationVisitor = new ConstantPropagationVisitor(semanticsResult.getSymbolTable());
            updated = constantPropagationVisitor.visit(semanticsResult.getRootNode());
        } while(updated);
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        if(!ollirResult.getConfig().containsKey("registerAllocation")){
            return ollirResult;
        }
        String numberOfRegistersString = ollirResult.getConfig().get("registerAllocation");
        if(numberOfRegistersString == null){
            return ollirResult;
        }
        int numberOfRegisters = Integer.parseInt(numberOfRegistersString);
        if(numberOfRegisters < 0){
            return ollirResult;
        }
        numberOfRegisters = numberOfRegisters == 0 ? 99 : numberOfRegisters; // If -r = 0 we'll try to minimize the number of registers

        for (Method method : ollirResult.getOllirClass().getMethods()) {
            method.buildCFG();
            Node node = method.getBeginNode();
            
            LivenessAnalyser livenessAnalyser = new LivenessAnalyser(node, method.getParams());
            Set<Web> webs = livenessAnalyser.getWebs();
            
            VariableSplitter variableSplitter = new VariableSplitter(webs);
            variableSplitter.split(method);

            InterferenceGraphCreator interferenceGraphCreator = new InterferenceGraphCreator(webs);
            GraphColoringSolver graphColoringSolver = new GraphColoringSolver(interferenceGraphCreator.createGraph(), numberOfRegisters);
            boolean canColor = graphColoringSolver.solve();
            if(!canColor){
                ollirResult.getReports().add(new Report(ReportType.ERROR, Stage.OPTIMIZATION, -1, "Insufficient registers"));
                return ollirResult;
            }
            Map<String, Integer> registerMap = graphColoringSolver.getVariableColorMap();

            int offset = method.isStaticMethod() ? 0 : 1;
    
            method.buildVarTable();
            
            for(var entry : method.getVarTable().entrySet()){
                String varName = entry.getKey();
                if(registerMap.containsKey(varName)){
                    entry.getValue().setVirtualReg(registerMap.get(varName) + offset);
                } else {
                    entry.getValue().setVirtualReg(offset);
                }
            }
        }

        return ollirResult;
    }
}
