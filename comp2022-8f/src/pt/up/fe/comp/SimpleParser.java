package pt.up.fe.comp;

import java.util.Collections;
import java.util.Map;

import pt.up.fe.comp.ParseException;
import pt.up.fe.comp.ast.LineColAnnotator;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParser;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;


/**
 * Copyright 2022 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

public class SimpleParser implements JmmParser {

    @Override
    public JmmParserResult parse(String jmmCode, Map<String, String> config) {

        try {

            JmmGrammarParser parser = new JmmGrammarParser(SpecsIo.toInputStream(jmmCode));
            parser.Start();

            return getJmmParserResult(config, parser);

        } catch (ParseException e) {
            Token token = e.getToken();
            String msg = "Exception during parsing: " + token + " at line " + token.getBeginLine() + " and column " + token.getBeginColumn();
            return JmmParserResult.newError(Report.newError(Stage.SYNTATIC, token.getBeginLine(), token.getBeginColumn(), msg, e));
        } catch (Exception e) {
            return JmmParserResult.newError(Report.newError(Stage.SYNTATIC, -1, -1, "Exception during parsing", e));
        }
    }

    @Override
    public JmmParserResult parse(String jmmCode, String startingRule, Map<String, String> config) {
        try {
            JmmGrammarParser parser = new JmmGrammarParser(SpecsIo.toInputStream(jmmCode));
            SpecsSystem.invoke(parser, startingRule);

            return getJmmParserResult(config, parser);

        } catch (Exception e) {
            return JmmParserResult.newError(Report.newError(Stage.SYNTATIC, -1, -1, "Exception during parsing", e));
        }
    }

    private JmmParserResult getJmmParserResult(Map<String, String> config, JmmGrammarParser parser) {
        Node root = parser.rootNode();
        root.dump("");

        if (!(root instanceof JmmNode)) {
            return JmmParserResult.newError(new Report(ReportType.WARNING, Stage.SYNTATIC, -1,
                    "JmmNode interface not yet implemented, returning null root node"));
        }

        LineColAnnotator annotator = new LineColAnnotator();
        annotator.visit((JmmNode) root);

        return new JmmParserResult((JmmNode) root, Collections.emptyList(), config);
    }
}
