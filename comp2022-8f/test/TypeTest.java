import org.junit.Test;
import pt.up.fe.comp.TestUtils;

public class TypeTest {
    public void test(String stringToParse, Boolean shouldPass) {
        var parserResult = TestUtils.parse(stringToParse, "Type");
        if(shouldPass){
            TestUtils.noErrors(parserResult);
        } else {
            TestUtils.mustFail(parserResult);
        }
    }

    /*
    @Test
    public void integer(){
        test("int", true);
    }

    @Test
    public void integerArray(){
        test("int[]", true);
    }

    @Test
    public void integerArrayWithSpaces(){
        test("int [    ]", true);
    }

    @Test
    public void bool(){
        test("boolean", true);
    }

    @Test
    public void identifier(){
        test("SomeIdentifier", true);
        test("identifier", true);
        test("!", false);
    }

    @Test
    public void empty(){
        test("", false);
    }*/
    
}
