package ast;

public class Assignment extends Statement {
    public ModifiablePrimary target;
    public Expression value;

    public Assignment(ModifiablePrimary target, Expression value) {
        this.target = target;
        this.value = value;
    }

    @Override 
    public void print(String indent) {
        System.out.println(indent + "Assignment");
        System.out.println(indent + "  target:");
        target.print(indent + "    ");
        System.out.println(indent + "  value:");
        value.print(indent + "    ");
    }
}
