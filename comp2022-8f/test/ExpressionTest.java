import org.junit.Test;
import pt.up.fe.comp.TestUtils;

public class ExpressionTest {
    public void test(String stringToParse, Boolean shouldPass) {
        var parserResult = TestUtils.parse(stringToParse, "Expression");
        if(shouldPass){
            TestUtils.noErrors(parserResult);
        } else {
            TestUtils.mustFail(parserResult);
        }
    }
}
