package ast.expressions;

import ast.ASTNode;
import ast.types.Type;
import semantic.Conversion;

public abstract class Expression extends ASTNode{
    
    // anotations for semantic analyzer
    public Type resolvedType;
    public Conversion conversion = Conversion.NONE;

    @Override
    public abstract void print(String indent);

}