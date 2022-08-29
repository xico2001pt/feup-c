import org.junit.Test;
import pt.up.fe.comp.TestUtils;

public class IntegerLiteralTest {
    public void test(String stringToParse, Boolean shouldPass) {
        var parserResult = TestUtils.parse(stringToParse, "IntegerLiteral");
        if(shouldPass){
            TestUtils.noErrors(parserResult);
        } else {
            TestUtils.mustFail(parserResult);
        }
    }
    
    /*
    @Test
    public void integer(){
        test("0", true);
        test("1", true);
        test("21398213", true);
        test("123A", false);
        test("1 2", false);
        test("1.2", false);
    }

    @Test
    public void binary(){
        test("0b0", true);
        test("0b1", true);
        test("0b10101101", true);
        test("0b", false);
        test("0b2", false);
        test("0b10120", false);
    }

    @Test
    public void octal(){
        test("00", true);
        test("01", true);
        test("07654321013", true);
        test("08", false);
    }

    @Test
    public void hexadecimal(){
        test("0xABC", true);
        test("0x0", true);
        test("0x0123456789ABCDEF", true);
        test("0x0123456789ABCDEFG", false);
        test("0x", false);
    }
    */
}
