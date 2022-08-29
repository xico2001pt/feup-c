package pt.up.fe.comp.ast;

import pt.up.fe.specs.util.SpecsStrings;

public enum AstNode {
    PROGRAM,
    MAIN_METHOD_DECLARATION,
    INSTANCE_METHOD_DECLARATION,
    CLASS_DECLARATION,
    INT_LITERAL,
    ID,
    VAR_DECLARATION,
    LENGTH_OP,
    BINARY_OP,
    UNARY_OP,
    CLASS_METHOD,
    CONDITION,
    ASSIGNMENT,
    ARRAY_ACCESS,
    ARRAY_INITIALIZATION,
    CLASS_INITIALIZATION,
    BOOL,
    PARAMETER,
    EXPRESSION_IN_PARENTHESES,
    ARRAY_ASSIGNMENT,
    ARGUMENTS,
    ARGUMENT,
    RETURN_EXPRESSION,
    IMPORT_DECL,
    STATEMENT_EXPRESSION,
    IF_STATEMENT,
    WHILE_STATEMENT,
    STATEMENT_SCOPE;

    private final String name;

    private AstNode(){
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    @Override
    public String toString(){
        return name;
    }
}
