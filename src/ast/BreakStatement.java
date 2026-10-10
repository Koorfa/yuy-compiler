package ast;

public class BreakStatement extends Statement {
    
    @Override
    public void print(String indent) {
        System.out.println(indent + "BreakStatement");
    }
}