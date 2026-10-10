package ast;

public class WhileLoop extends Statement {
    public Expression condition;
    public Body body;

    public WhileLoop(Expression condition, Body body) {
        this.condition = condition;
        this.body = body;
    }

    @Override 
    public void print(String indent) {
        System.out.println(indent + "WhileLoop");
        System.out.println(indent + "  Condition:");
        condition.print(indent + "    ");
        System.out.println(indent + "  Body:");
        body.print(indent + "    ");
    }
}
