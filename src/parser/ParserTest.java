import java.util.List;

public class ParserTest {

    public static void main(String[] args) {
        String source = "var x : integer is 5";

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();

        Parser parser = new Parser(tokens);

        System.out.println("Current: " + parser.peek());
        System.out.println("Next: " + parser.peekNext());

        Expression expression =
                new BinaryExpression(
                        new IntegerLiteral(5),
                        TokenType.PLUS,
                        new BinaryExpression(
                                new IntegerLiteral(3),
                                TokenType.MULTIPLY,
                                new IntegerLiteral(2)
                        )
                );

        System.out.println("AST:");
        expression.print("");

    }


}