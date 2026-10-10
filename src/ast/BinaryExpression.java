package ast;

import lexer.TokenType;

public class BinaryExpression extends Expression {

    private final Expression left;
    private final TokenType operator;
    private final Expression right;

    public BinaryExpression(Expression left, TokenType operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "BinaryExpression: " + operator);
        left.print(indent + "    ");
        right.print(indent + "    ");
    }
}