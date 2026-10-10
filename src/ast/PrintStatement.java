package ast;

import java.util.List;

public class PrintStatement extends Statement {
    public List<Expression> expressions;

    public PrintStatement(List<Expression> expressions) {
        this.expressions = expressions;
    }

    @Override 
    public void print(String indent) {
        System.out.println(indent + "PrintStatement");
        for (Expression expr : expressions) {
            expr.print(indent + "  ");
        }
    }
}
