package pt.up.fe.comp.ollir;

public class OllirStatement {
    String codeBefore;
    String resultVariable;

    public OllirStatement(String codeBefore, String resultVariable){
        this.codeBefore = codeBefore;
        this.resultVariable = resultVariable;
    }

    public String getCodeBefore() {
        return codeBefore;
    }

    public String getResultVariable() {
        return resultVariable;
    }
}
