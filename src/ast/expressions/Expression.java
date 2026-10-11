package ast.expressions;

import ast.ASTNode;

public abstract class Expression extends ASTNode{
    
    @Override
    public abstract void print(String indent);

}