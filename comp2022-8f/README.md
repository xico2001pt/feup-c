# Compilers Project

## Group 8F

|Name|Number|Self-assessment Grade|Contribution Grade|
|---|---|---|---|
|Adriano Soares|201904873|20|25%|
|Ângela Cruz|201806781|20|25%|
|Filipe Campos|201905609|20|25%|
|Francisco Cerqueira|201905337|20|25%|

**Global grade of the project**: 20

## Summary

The project's main intent was to put to practice the theorical principals of the Compilers course. In order to do so we've implemented an executable capable of compiling our own Java-- language with JavaCC, Java, Ollir and Jasmin programming languages. Our main features are semantic analysis with syntatic error checking, code generation with Ollir and Jasmin as an intermediate language and further code optimizations.

## Semantic Analysis

- Variables must be initialized before being utilized.
- All used symbols must be defined.
- The `.length` operator can only be used on arrays.
- All operations (`+`,`-`,`*`,`/`,`<`,`&&`,`!`) must be used on arguments of the correct type.
- Non-static methods cannot be invoked from a static context.
- The number of arguments passed to a function call must be correct (if the function is defined in the current class).
- The arguments passed to a function call must have the correct type (if the function is defined in the current class).
- Local methods must exist in order to be called (this.foo() is invalid if foo is not defined).
- Conditions must have a boolean value.
- Assignment lhs and rhs type must match (if the types are known).
- Array size and index must be an integer.
- Array assignment and initialization can only be done on arrays.
- Return type must match the declared return type.
- Classes used in the code must be imported.

### Code Generation

We start by performing the generation of the `abstract syntax tree` with the aid of the JavaCC language. During this process we annotate leaves and nodes with any useful information to be used in the next step (e.g. variable names, operation type).

With the AST defined in the previous step we perform a top-down analysis in order to build our `symbol table`, a table containing information about the occurrence of variable names.

Following that, we start the `semantic analysis` just to make sure our Java-- code acts according to the rules defined above.

If the code has no semantic errors it's then converted into `Ollir code` and then to `Jasmin` that'll be useful to generate JVM instructions readable by a Java VM.

Jasmin code generation makes sure that the proper `limits` for the `stack` and `locals` (registers) are used.

### Optimizations

By default, after generating the ollir code successfully, our Jasmin generator applies the following optimization:

**JVM Instructions** - implemented `ìinc`, `bipush`, `sipush`, `iconst_[0..5]`, `iload_[0..5]`, `istore_[0..5]` and boolean comparisons with 0 in conditional instructions are now `iflt`, `ifle`, `ifgt` and `ifge` accordingly.

Depending on the flags used the compiler will apply some optimizations to improve the efficiency of the generated code.

`-r N_REGISTERS`

The number of registers used will be limited to the number of registers specified (Or unlimited if N_REGISTERS=0), the compiler will always attempt to minimize the number of registers used, and, if impossible to use only N_REGISTERS an error will be shown.

`-o`

With this flag enabled the compiler will apply constant propagation, during which, the while loops will be annotated with information about whether or not they can be converted to do while loops. This conversion will happen during the ollir code generation.

`-a`

With this flag enabled the compiler will iteratively apply the following optimizations until there's nothing left to optimize:

**Constant Folding and Basic Arithmetic Simplification** - operations with constant values will be folded and the following arithmetic simplifications will be applied: 
- `x && false = false`
- `x + 0 = x`
- `x - 0 = x`
- `x * 0 = 0`
- `x * 1 = x`
- `x / 1 = x`
- `0 / x = 0`

**If/While Removal (Dead code elimination)** - if statements with constant conditions will be removed, being substituted by the code inside the branch that always executes.  While loops with a constant `false` value will be removed from the code since they'll never be executed.

**Unused Assignment Removal (Dead Code Elimination)** - all assignments that are never used will be removed.

## Pros

The tool has a wide set of optimizations that enable the creation of very efficient JVM code.

## Cons

Some parts of the source code are hard to read and could be refactored in order to improve the technical quality of the project and enable the development of more features.
