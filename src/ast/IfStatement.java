package ast;

public class IfStatement extends Statement {
    public Expression condition;
    public Body thenBody;
    public Body elseBody;

    public IfStatement(Expression condition, Body thenBody, Body elseBody) {
        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    @Override 
    public void print(String indent) {
        System.out.println(indent + "IfStatement");
        System.out.println(indent + "  Condition:");
        condition.print(indent + "    ");
        System.out.println(indent + "  Then:");
        thenBody.print(indent + "    ");
        if (elseBody != null) {
            System.out.println(indent + "  Else:");
            elseBody.print(indent + "    ");
        }
    }
}
