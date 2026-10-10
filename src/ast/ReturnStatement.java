package ast;

public class ReturnStatement extends Statement {
    public Expression value;
    
    public ReturnStatement(Expression value) {
        this.value = value;
    }
    
    @Override
    public void print(String indent) {
        System.out.println(indent + "ReturnStatement");
        value.print(indent + "  ");
    }
}