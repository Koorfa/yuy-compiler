package ast;

public class ContinueStatement extends Statement {
    
    @Override
    public void print(String indent) {
        System.out.println(indent + "ContinueStatement");
    }
}