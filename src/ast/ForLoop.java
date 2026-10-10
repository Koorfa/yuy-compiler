package ast;

public class ForLoop extends Statement {
    public String variable;
    public Expression start;
    public Expression end;
    public boolean reverse;
    public Body body;

    public ForLoop(String variable, Expression start, Expression end,
            boolean reverse, Body body) {
        this.variable = variable;
        this.start = start;
        this.end = end;
        this.reverse = reverse;
        this.body = body;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "ForLoop");
        System.out.println(indent + "  Variable:");
        System.out.println(indent + "    " + variable);
        System.out.println(indent + "  Start:");
        start.print(indent + "    ");
        if (end != null) {
            System.out.println(indent + "  End:");
            end.print(indent + "    ");
        }
        System.out.println(indent + "  Reverse: " + reverse);
        System.out.println(indent + "  Body:");
        body.print(indent + "    ");
    }
}
