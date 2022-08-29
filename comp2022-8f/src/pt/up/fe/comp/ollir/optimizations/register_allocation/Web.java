package pt.up.fe.comp.ollir.optimizations.register_allocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Web {
    Set<Integer> instructions;
    String variableName;
    String type;
    int id;

    public Web(Set<Integer> instructions, String variableName, String type, int id){
        this.instructions = instructions;
        this.variableName = variableName;
        this.id = id;
    }

    public Web(String variableName, String type, int id){
        this.instructions = new HashSet<>();
        this.variableName = variableName;
        this.type = type;
        this.id = id;
    }

    public Set<Integer> getInstructions() {
        return instructions;
    }

    public String getVariableName() {
        return variableName;
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void merge(Web web){
        this.instructions.addAll(web.getInstructions());
        this.id = Math.min(this.id, web.getId());
    }

    public boolean contains(Integer instruction){
        return this.instructions.contains(instruction);
    }

    public void add(Integer instruction){
        this.instructions.add(instruction);
    }

    public boolean disjoint(Web web){
        return Collections.disjoint(this.instructions, web.getInstructions());
    }

    @Override
    public String toString() {
        return variableName + "_" + id;
    }
}
