package pt.up.fe.comp.ollir;

public class OllirGeneratorHint {
    String expectedType; // Expected OLLIR type (bool, array.i32, etc...)
    String methodSignature; // Name of the current method
    Boolean needsTemporaryVar; // If child needs to allocate a temporary variable or not

    public OllirGeneratorHint(String methodSignature, String expectedType, Boolean needsTemporaryVar){
        this.expectedType = expectedType;
        this.methodSignature = methodSignature;
        this.needsTemporaryVar = needsTemporaryVar;
    }

    public OllirGeneratorHint(String methodSignature){
        this.methodSignature = methodSignature;
        this.expectedType = "";
        this.needsTemporaryVar = true;
    }

    public Boolean needsTemporaryVar() {
        return needsTemporaryVar;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getExpectedType() {
        return expectedType;
    }
}
