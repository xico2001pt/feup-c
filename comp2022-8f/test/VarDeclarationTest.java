import org.junit.Test;
import pt.up.fe.comp.TestUtils;

public class VarDeclarationTest {
    public void test(String stringToParse, Boolean shouldPass) {
        var parserResult = TestUtils.parse(stringToParse, "VarDeclaration");
        if(shouldPass){
            TestUtils.noErrors(parserResult);
        } else {
            TestUtils.mustFail(parserResult);
        }
    }  
    
    @Test
    public void integer(){
        test("int example;", true);
        test("int example", false);
    }

    @Test
    public void integerArray(){
        test("int[] example;", true);
    }

    @Test
    public void bool(){
        test("boolean example;", true);
    }

    @Test
    public void identifier(){
        test("SomeIdentifier example;", true);
        test("Some Identifier example;", false);
    }

    @Test
    public void empty(){
        test("example;", false);
        test(";", false);
    }
}
