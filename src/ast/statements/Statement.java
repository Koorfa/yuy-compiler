package ast.statements;

import ast.ASTNode;

public abstract class Statement extends ASTNode {
    @Override
    public abstract void print(String indent);
}
