package pt.up.fe.comp;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import pt.up.fe.comp.analysis.JmmAnalyser;
import pt.up.fe.comp.jasmin.OllirToJasmin;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.ollir.JmmOptimizer;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // read the input code
        if (args.length < 1) {
            throw new RuntimeException("Expected at least one argument, a path to an existing input file.");
        }
        File inputFile = new File(args[0]);
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + args[0] + "'.");
        }
        String input = SpecsIo.read(inputFile);

        
        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("optimizeAll", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        for(int i = 1; i < args.length; ++i){
            if(args[i].equals("-o")){
                config.put("optimize", "true");
            }
            if(args[i].equals("-a")){
                config.put("optimizeAll", "true");
            }
            if(args[i].equals("-r")){
                i++;
                config.put("registerAllocation", args[i]);
            }
        }

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(input,  config);

        // Check if there are parsing errors
        parserResult.getReports().stream()
            .filter(report -> report.getType() == ReportType.ERROR)
            .findFirst()
            .ifPresent(report -> {
                if (report.getException().isPresent()) {
                    System.out.println(report.getMessage());
                }
            });

        JmmNode rootNode = parserResult.getRootNode();
        if (rootNode != null) {
            System.out.println(rootNode.toTree());
        } else {
            System.out.println("Program finished due to parser error.");
            return;
        }
        
        // Analysis Stage
        JmmAnalyser analyser = new JmmAnalyser();
        JmmSemanticsResult analysisResult = analyser.semanticAnalysis(parserResult);

        // Check if there are semantic errors
        var analysisErrors = analysisResult.getReports().stream()
                .filter(report -> report.getType() == ReportType.ERROR).collect(Collectors.toList());
        if(analysisErrors.size() > 0){
            analysisErrors.stream().findFirst().ifPresent(report -> {
                if (!report.getMessage().isEmpty()) {
                    System.out.println("Error during semantic analysis at line " + report.getLine()
                            + " and column " + report.getColumn() + ".");
                    System.out.println(report.getMessage());
                }
            });
            System.out.println("Program finished due to semantic error.");
            return;
        }

        // Optimizer
        JmmOptimization optimizer = new JmmOptimizer();

        // AST optimization
        analysisResult = optimizer.optimize(analysisResult);

        // AST to OLLIR
        OllirResult ollirResult = optimizer.toOllir(analysisResult);
        if(ollirResult == null){
            System.out.println("Program finished due to error in conversion to ollir.");
            return;
        }

        // OLLIR optimization
        ollirResult = optimizer.optimize(ollirResult);
        if(!ollirResult.getReports().isEmpty()){
            System.out.println("Program finished due to error ollir optimization.");
            System.out.println(ollirResult.getReports().get(0).getMessage());
            return;
        }
    
        TestUtils.noErrors(ollirResult);
        ollirResult.getOllirClass().buildCFGs();

        // OLLIR to Jasmin
        OllirToJasmin converter = new OllirToJasmin();
        JasminResult result = converter.toJasmin(ollirResult);

        result.compile();
        result.run();
    }
}
