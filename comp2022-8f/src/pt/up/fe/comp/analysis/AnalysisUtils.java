package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

public class AnalysisUtils {
    public static boolean isNativeType(SymbolTable table, Type type){
        String typeName = type.getName();
        if(typeName.equals(table.getClassName()) || typeName.equals(table.getSuper())){
            return true;
        }
        return typeName.equals("int") || typeName.equals("boolean") || typeName.equals("String");
    }

    public static boolean isAssignable(SymbolTable table, JmmType lhs, JmmType rhs){
        if(rhs.equals(lhs)){
            return true;
        }
        if(lhs.getName().equals(table.getSuper()) && rhs.getName().equals(table.getClassName())){
            return lhs.isArray() == rhs.isArray();
        }
        if(!isNativeType(table, rhs)){
            if(lhs.getName().equals(table.getClassName()) || lhs.getName().equals(table.getSuper())){
                return table.getSuper() != null; // class extends another class
            }
            return true;
        }
        return false;
    }
}
