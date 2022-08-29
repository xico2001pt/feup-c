package pt.up.fe.comp.analysis;

import pt.up.fe.comp.analysis.stages.ImportCheckVisitor;
import pt.up.fe.comp.analysis.stages.TypeCheckVisitor;
import pt.up.fe.comp.analysis.table.SymbolTableBuilder;
import pt.up.fe.comp.analysis.table.SymbolTableCollector;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult; 
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;
import java.util.ArrayList;
 
public class JmmAnalyser implements JmmAnalysis { 
    @Override 
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {         
        SymbolTableBuilder symbolTable = new SymbolTableBuilder();
        SymbolTableCollector collector = new SymbolTableCollector();
        collector.visit(parserResult.getRootNode(), symbolTable);

        List<Report> reports = collector.getReports();

        TypeCheckVisitor analysisVisitor = new TypeCheckVisitor(symbolTable);
        analysisVisitor.visit(parserResult.getRootNode(), reports);

        ImportCheckVisitor importVisitor = new ImportCheckVisitor(symbolTable);
        importVisitor.visit(parserResult.getRootNode(), reports);

        return new JmmSemanticsResult(parserResult, symbolTable, reports); 
    } 
}