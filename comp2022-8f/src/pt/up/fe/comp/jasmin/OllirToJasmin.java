package pt.up.fe.comp.jasmin;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class OllirToJasmin implements JasminBackend {
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        JasminGenerator generator = new JasminGenerator(ollirResult.getOllirClass());
        return generator.convert();
    }
}