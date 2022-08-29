package pt.up.fe.comp.ollir.optimizations.register_allocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.specs.comp.ollir.Node;

/*
 * 
 * Coloring a graph with N colors
• If degree < N (degree of a node = # of edges)
– Node can always be colored
– After coloring the rest of the nodes, you’ll have at least one color
left to color the current node
• If degree ≥ N
– still may be colorable with N colors
– exact solution is NP complete

• Remove nodes that have degree < N
– Push the removed nodes onto a stack
• If all the nodes have degree ≥ N
– Find a node to spill (no color for that node)
– Remove that node
• When empty, start the coloring step
– pop a node from stack back
– Assign it a color that is different from its connected nodes (since
degree < N, a color should exist)
 */


public class GraphColoringSolver {
    Set<Pair<Node, String>> nodes;
    Map<Node, Integer> colors;
    int numberOfColors;

    public GraphColoringSolver(Set<Pair<Node, String>> nodes, int numberOfColors) {
        this.nodes = nodes;
        this.numberOfColors = numberOfColors;
        this.colors = new HashMap<>();
    }

    public boolean solve() {
        Stack<Pair<Node, String>> stack = new Stack<>();
        for(var node : this.nodes){
            if(node.first.getSuccessors().size() >= this.numberOfColors){
                return false;
            }
            stack.push(node);
        }

        while(!stack.isEmpty()){
            Pair<Node, String> pair = stack.pop();
            Node node = pair.first;            

            Set<Integer> usedColors = new HashSet<>();
            for (var succ : node.getSuccessors()){
                if(this.colors.containsKey(succ)){
                    usedColors.add(this.colors.get(succ));
                }
            }

            int color = 0;
            while(true){
                if(!usedColors.contains(color)){
                    break;
                }
                color++;
            }
            
            this.colors.put(node, color);
        }
        
        return true;
    }

    public Map<String, Integer> getVariableColorMap(){
        Map<String, Integer> variableColorMap = new HashMap<>();
        for(var pair : this.nodes){
            variableColorMap.put(pair.second, this.colors.get(pair.first));
        }
        return variableColorMap;
    }
}