package pt.up.fe.comp.ollir.optimizations.register_allocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.specs.comp.ollir.AssignInstruction;
import org.specs.comp.ollir.BinaryOpInstruction;
import org.specs.comp.ollir.CallInstruction;
import org.specs.comp.ollir.CallType;
import org.specs.comp.ollir.CondBranchInstruction;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.InstructionType;
import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.NodeType;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.GetFieldInstruction;
import org.specs.comp.ollir.PutFieldInstruction;
import org.specs.comp.ollir.UnaryOpInstruction;
import org.specs.comp.ollir.ReturnInstruction;
import org.specs.comp.ollir.SingleOpInstruction;
import org.specs.comp.ollir.Type;

public class LivenessAnalyser {
    private final List<Node> nodesList;
    private final List<Set<String>> useList;
    private final List<Set<String>> defList;
    private final List<Set<String>> inList;
    private final List<Set<String>> outList;
    private final HashMap<String, Set<Web>> webs;
    
    public LivenessAnalyser(Node beginNode, List<Element> parameters){
        this.nodesList = new ArrayList<>();
        this.useList = new ArrayList<>();
        this.defList = new ArrayList<>();
        this.inList = new ArrayList<>();
        this.outList = new ArrayList<>();
        this.webs = new HashMap<>();

        this.addNode(beginNode);
        this.initNodes();

        for(var param : parameters){
            Operand operand = (Operand) param;
            if(nodesList.size() > 0){
                defList.get(0).add(operand.getName());
            }
            this.webs.put(operand.getName(), new HashSet<>());
        }

        this.livenessAnalyse();
        this.createWebs();
    }

    private void addNode(Node node){
        if (!nodesList.contains(node)){
            if (node.getNodeType() == NodeType.INSTRUCTION){
                nodesList.add(node);
            }
            for(Node succ : node.getSuccessors()){
                addNode(succ);
            }
        }
    }

    private void initNodes() {
        for(Node node : nodesList){
            if(node.getNodeType() == NodeType.INSTRUCTION){
                Instruction instruction = (Instruction) node;

                // If assign add to def
                Set<String> def = new HashSet<>();
                if (instruction instanceof AssignInstruction) {
                    AssignInstruction assignInstruction = (AssignInstruction) instruction;
                    String variableName = ((Operand) assignInstruction.getDest()).getName();
                    def.add(variableName);
                    this.webs.put(variableName, new HashSet<>());   // Everytime a variable is defined a new entry in webs is created
                }
                defList.add(def);

                // For any instruction collectUses
                Set<String> use = new HashSet<>();
                this.collectUses(instruction, use);
                useList.add(use);

                // Initialize in and out to empty set
                inList.add(new HashSet<>());
                outList.add(new HashSet<>());
            }
        }
    }

    private void collectUses(Instruction instruction, Set<String> use){
        switch (instruction.getInstType()) {
            case ASSIGN:
                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                this.collectUses(assignInstruction.getRhs(), use);
                break;
            case BINARYOPER:
                BinaryOpInstruction binOp = (BinaryOpInstruction) instruction;
                Element lhsOperand = binOp.getLeftOperand();
                if(!lhsOperand.isLiteral()){
                    use.add(((Operand) lhsOperand).getName());
                }
                Element rhsOperand = binOp.getRightOperand();
                if(!rhsOperand.isLiteral()){
                    use.add(((Operand) rhsOperand).getName());
                }
                break;
            case BRANCH:
                CondBranchInstruction condInstruction = (CondBranchInstruction) instruction;
                this.collectUses(condInstruction.getCondition(), use);
                break;
            case CALL:
                CallInstruction callInstruction = (CallInstruction) instruction;
                Operand firstOperand = (Operand) callInstruction.getFirstArg();
                if(firstOperand.getType().getTypeOfElement() != ElementType.THIS && callInstruction.getInvocationType() != CallType.invokestatic){
                    use.add(firstOperand.getName());
                }
                if(callInstruction.getListOfOperands() != null){
                    for(var operand : callInstruction.getListOfOperands()){
                        if(!operand.isLiteral()) {
                            use.add(((Operand) operand).getName());
                        }
                    }
                }
                break;
            case GETFIELD:
                GetFieldInstruction getFieldInstruction = (GetFieldInstruction) instruction;
                Element getFieldClass = getFieldInstruction.getFirstOperand();
                if(getFieldClass.getType().getTypeOfElement() != ElementType.THIS){
                    use.add(((Operand) getFieldClass).getName());
                }
                break;
            case NOPER:
                SingleOpInstruction singleOpInstruction = (SingleOpInstruction) instruction;
                Element singleOperand = singleOpInstruction.getSingleOperand();
                if (!singleOperand.isLiteral()) {
                    use.add(((Operand) singleOperand).getName());
                }
                break;
            case PUTFIELD:
                PutFieldInstruction putFieldInstruction = (PutFieldInstruction) instruction;
                Element putFieldOperand = putFieldInstruction.getFirstOperand();
                if(putFieldOperand.getType().getTypeOfElement() != ElementType.THIS){
                    use.add(((Operand) putFieldOperand).getName());
                }
                Element putFieldInstructionThirdOperand = putFieldInstruction.getThirdOperand();
                if(!putFieldInstructionThirdOperand.isLiteral()){
                    use.add(((Operand) putFieldInstructionThirdOperand).getName());
                }
                break;
            case RETURN:
                ReturnInstruction returnInstruction = (ReturnInstruction) instruction;
                if (returnInstruction.hasReturnValue()) {
                    Element returnOperand = returnInstruction.getOperand();
                    if(!returnOperand.isLiteral()) {
                        use.add(((Operand) returnOperand).getName());
                    }
                }
                break;
            case UNARYOPER:
                UnaryOpInstruction unaryOpInstruction = (UnaryOpInstruction) instruction;
                Element unaryOpOperand = unaryOpInstruction.getOperand();
                if(!unaryOpOperand.isLiteral()){
                    use.add(((Operand) unaryOpInstruction.getOperand()).getName());    
                }
                break;
            default:
                break;
        }
    }

    private void livenessAnalyse() {
        boolean updated;
        do{
            updated = false;
            for(int i = 0; i < nodesList.size(); ++i){
                Node node = nodesList.get(i);
                Set<String> in = this.inList.get(i);
                Set<String> out = this.outList.get(i);
                Set<String> newOut = new HashSet<>();

                for(Node succ : node.getSuccessors()){
                    int listIndex = this.nodesList.indexOf(succ);
                    if(listIndex != -1){
                        newOut.addAll(this.inList.get(listIndex));
                    }
                }

                Set<String> newIn = new HashSet<>(this.useList.get(i));

                // out[i] - def[i]
                Set<String> outWithoutDef = new HashSet<>(newOut);
                outWithoutDef.removeAll(this.defList.get(i));

                newIn.addAll(outWithoutDef);

                if(!(in.equals(newIn) && out.equals(newOut))){
                    updated = true;
                }
                
                this.inList.set(i, newIn);
                this.outList.set(i, newOut);
            }
        } while(updated);
    }

    private void createWebs(){
        for (var entry : this.webs.entrySet()) {
            int webId = 0;
            for(int i = 0; i < this.nodesList.size(); ++i){
                Node node = nodesList.get(i);
                Set<String> def = this.defList.get(i);
                if (def.contains(entry.getKey())){
                    Instruction instruction = (Instruction) node;
                    if(instruction.getInstType() == InstructionType.ASSIGN){
                        AssignInstruction assignInstruction = (AssignInstruction) instruction;
                        Type type = assignInstruction.getTypeOfAssign();
                        Web web = new Web(entry.getKey(), type.toString(), webId++);
                        this.propagateWeb(node, web);
                        entry.getValue().add(web);
                    }
                }
            }
            
            boolean updated;
            List<Web> webList = new ArrayList<>(entry.getValue());
            do {
                updated = false;
                Set<Web> killSet = new HashSet<>();
                
                for(int i = 0; i < webList.size(); ++i){
                    Web firstWeb = webList.get(i);
                    for(int j = i + 1; j < webList.size(); ++j){
                        Web secondWeb = webList.get(j);
                        if(!firstWeb.disjoint(secondWeb)){
                            secondWeb.merge(firstWeb);
                            killSet.add(firstWeb);
                            updated = true;
                        }
                    }
                }
                for(var w : killSet){
                    webList.remove(w);
                }
            } while(updated);
            this.webs.put(entry.getKey(), new HashSet<>(webList));
        }
        
        for(var entry : this.webs.entrySet()){
            System.out.print(entry.getKey() + " - ");
            for(var w : entry.getValue()){
                System.out.print("{");
                for(var k : w.getInstructions()){
                    System.out.print(k + ", ");
                }
                System.out.print("}");
            }
            System.out.println();
        }
    }
    
    public Set<Web> getWebs(){
        Set<Web> webs = new HashSet<>();
        for(var entry : this.webs.entrySet()){
            for(var web : entry.getValue()){
                if(!web.getInstructions().isEmpty()){
                    webs.add(web);
                }
            }
        }
        return webs;
    }

    private void propagateWeb(Node node, Web web){
        int nodeIndex = this.nodesList.indexOf(node);
        if(node.getNodeType() == NodeType.INSTRUCTION && !web.contains(node.getId())){
            boolean inOut = this.outList.get(nodeIndex).contains(web.getVariableName());
            boolean used = this.useList.get(nodeIndex).contains(web.getVariableName());
            if(used || inOut){
                web.add(node.getId());
                if(inOut){
                    for(Node succ : node.getSuccessors()){
                        this.propagateWeb(succ, web);
                    }
                }
            }
        }
    }
}
