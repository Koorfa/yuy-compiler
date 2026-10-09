public class UnaryExpression extends Expression {

    private final TokenType operator;
    private final Expression operand;

    public UnaryExpression(TokenType operator, Expression operand) {
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "UnaryExpression: " + operator);
        operand.print(indent + "    ");
    }
}