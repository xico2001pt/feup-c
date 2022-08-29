package pt.up.fe.comp.ollir.optimizations.register_allocation;

import org.specs.comp.ollir.AssignInstruction;
import org.specs.comp.ollir.BinaryOpInstruction;
import org.specs.comp.ollir.CallInstruction;
import org.specs.comp.ollir.CallType;
import org.specs.comp.ollir.CondBranchInstruction;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.GetFieldInstruction;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.PutFieldInstruction;
import org.specs.comp.ollir.ReturnInstruction;
import org.specs.comp.ollir.SingleOpInstruction;
import org.specs.comp.ollir.UnaryOpInstruction;

import com.javacc.output.Translator.SymbolTable;

import java.util.HashSet;
import java.util.Set;

public class VariableSplitter {
    private final Set<Web> webs;

    public VariableSplitter(Set<Web> webs){
        this.webs = webs;
    }

    public void split(Method method){
        for(Instruction instruction : method.getInstructions()){
            this.rename(instruction, instruction.getId());
        }
        for(Element elem : method.getParams()){
            Operand operand = (Operand) elem;
            operand.setName(operand.getName() + "_0");
        }
    }

    private void rename(Instruction instruction, int instructionId){
        // websWithThisInstruction
        // when we use operand lookup websWithInstruction
    
        Set<Web> websWithInstruction = new HashSet<>();
        for (Web web : webs){
            if(web.getInstructions().contains(instructionId)){
                websWithInstruction.add(web);
            }
        }

        switch (instruction.getInstType()) {
            case ASSIGN:
                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                this.lookupVarAndReplace((Operand) assignInstruction.getDest(), websWithInstruction);
                this.rename(assignInstruction.getRhs(), assignInstruction.getId());
                break;
            case BINARYOPER:
                BinaryOpInstruction binOp = (BinaryOpInstruction) instruction;
                Element lhsOperand = binOp.getLeftOperand();
                if(!lhsOperand.isLiteral()){
                    this.lookupVarAndReplace((Operand) lhsOperand, websWithInstruction);
                }
                Element rhsOperand = binOp.getRightOperand();
                if(!rhsOperand.isLiteral()){
                    this.lookupVarAndReplace((Operand) rhsOperand, websWithInstruction);
                }
                break;
            case BRANCH:
                CondBranchInstruction condInstruction = (CondBranchInstruction) instruction;
                this.rename(condInstruction.getCondition(), condInstruction.getId());
                break;
            case CALL:
                CallInstruction callInstruction = (CallInstruction) instruction;
                Operand firstOperand = (Operand) callInstruction.getFirstArg();
                if(firstOperand.getType().getTypeOfElement() != ElementType.THIS && callInstruction.getInvocationType() != CallType.invokestatic){
                    this.lookupVarAndReplace(firstOperand, websWithInstruction);
                }
                if(callInstruction.getListOfOperands() != null){
                    for(var operand : callInstruction.getListOfOperands()){
                        if(!operand.isLiteral()) {
                            this.lookupVarAndReplace((Operand) operand, websWithInstruction);
                        }
                    }
                }
                break;
            case GETFIELD:
                GetFieldInstruction getFieldInstruction = (GetFieldInstruction) instruction;
                Element getFieldClass = getFieldInstruction.getFirstOperand();
                if(getFieldClass.getType().getTypeOfElement() != ElementType.THIS){
                    this.lookupVarAndReplace((Operand) getFieldClass, websWithInstruction);
                }
                break;
            case NOPER:
                SingleOpInstruction singleOpInstruction = (SingleOpInstruction) instruction;
                Element singleOperand = singleOpInstruction.getSingleOperand();
                if (!singleOperand.isLiteral()) {
                    this.lookupVarAndReplace(((Operand) singleOperand), websWithInstruction);
                }
                break;
            case PUTFIELD:
                PutFieldInstruction putFieldInstruction = (PutFieldInstruction) instruction;
                Element putFieldOperand = putFieldInstruction.getFirstOperand();
                if(putFieldOperand.getType().getTypeOfElement() != ElementType.THIS){
                    this.lookupVarAndReplace((Operand) putFieldOperand, websWithInstruction);
                }
                Element putFieldInstructionThirdOperand = putFieldInstruction.getThirdOperand();
                if(!putFieldInstructionThirdOperand.isLiteral()){
                    this.lookupVarAndReplace((Operand) putFieldInstructionThirdOperand, websWithInstruction);
                }
                break;
            case RETURN:
                ReturnInstruction returnInstruction = (ReturnInstruction) instruction;
                if (returnInstruction.hasReturnValue()) {
                    Element returnOperand = returnInstruction.getOperand();
                    if(!returnOperand.isLiteral()) {
                        this.lookupVarAndReplace((Operand) returnOperand, websWithInstruction);
                    }
                }
                break;
            case UNARYOPER:
                UnaryOpInstruction unaryOpInstruction = (UnaryOpInstruction) instruction;
                Element unaryOpOperand = unaryOpInstruction.getOperand();
                if(!unaryOpOperand.isLiteral()){
                    this.lookupVarAndReplace((Operand) unaryOpInstruction.getOperand(), websWithInstruction);    
                }
                break;
            default:
                break;
        }
    }

    private void lookupVarAndReplace(Operand operand, Set<Web> webs){
        for (Web web : webs) {
            String varName = operand.getName();
            if (web.getVariableName().equals(varName)){
                operand.setName(varName + "_" + web.getId());
            }
        }
    }
}
