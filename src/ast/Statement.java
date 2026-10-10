package ast;

public abstract class Statement extends ASTNode {
    @Override
    public abstract void print(String indent);
}
