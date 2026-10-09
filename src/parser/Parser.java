import java.util.List;
import java.util.ArrayList;



public class Parser {

    private final List<Token> tokens;
    private int position;
    private Token currentToken;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
        this.currentToken = tokens.get(0);
    }

    public Token peek() {
        return currentToken;
    }

    public Token peekNext() {
        if (position + 1 < tokens.size()) {
            return tokens.get(position + 1);
        }

        return tokens.get(tokens.size() - 1);
    }

    public boolean check(TokenType type) {
        return currentToken.getType() == type;
    }

    public Token consume(TokenType type) {
        if (!check(type)) {
            throw error("Expected " + type + ", but found " + currentToken.getType());
        }

        Token token = currentToken;

        if (position < tokens.size() - 1) {
            position++;
            currentToken = tokens.get(position);
        }

        return token;
    }

    public boolean match(TokenType type) {
        if (check(type)) {
            consume(type);
            return true;
        }

        return false;
    }

    public void skipSeparators() {
        while (check(TokenType.SEMICOLON)) {
            consume(TokenType.SEMICOLON);
        }
    }

    private ParseException error(String message) {
        return new ParseException(
                "Parser error at line "
                        + currentToken.getLine()
                        + ", column "
                        + currentToken.getColumn()
                        + ": "
                        + message
        );
    }

    public Expression parseExpression() {
        Expression left = parseRelation();

        while (check(TokenType.AND)
                || check(TokenType.OR)
                || check(TokenType.XOR)) {

            TokenType operator = currentToken.getType();
            consume(operator);

            Expression right = parseRelation();

            left = new BinaryExpression(left, operator, right);
        }

        return left;
    }

    public Expression parseRelation() {
        Expression left = parseSimple();

        if (check(TokenType.LESS)
                || check(TokenType.LESS_EQUAL)
                || check(TokenType.GREATER)
                || check(TokenType.GREATER_EQUAL)
                || check(TokenType.EQUAL)
                || check(TokenType.NOT_EQUAL)) {

            TokenType operator = currentToken.getType();
            consume(operator);

            Expression right = parseSimple();

            return new BinaryExpression(left, operator, right);
        }

        return left;
    }

    public Expression parseSimple() {
        Expression left = parseFactor();

        while (check(TokenType.PLUS)
                || check(TokenType.MINUS)) {

            TokenType operator = currentToken.getType();
            consume(operator);

            Expression right = parseFactor();

            left = new BinaryExpression(left, operator, right);
        }

        return left;
    }

    public Expression parseFactor() {
        Expression left = parseSummand();

        while (check(TokenType.MULTIPLY)
                || check(TokenType.DIVIDE)
                || check(TokenType.MODULO)) {

            TokenType operator = currentToken.getType();
            consume(operator);

            Expression right = parseSummand();

            left = new BinaryExpression(left, operator, right);
        }

        return left;
    }

        public Expression parseSummand() {
            if (match(TokenType.LEFT_PAREN)) {
                Expression expression = parseExpression();
                consume(TokenType.RIGHT_PAREN);
                return expression;
            }

            return parsePrimary();
        }

        public Expression parsePrimary() {

            if (match(TokenType.PLUS)) {
                return new UnaryExpression(
                        TokenType.PLUS,
                        parsePrimary()
                );
            }

            if (match(TokenType.MINUS)) {
                return new UnaryExpression(
                        TokenType.MINUS,
                        parsePrimary()
                );
            }

            if (match(TokenType.NOT)) {
                return new UnaryExpression(
                        TokenType.NOT,
                        parsePrimary()
                );
            }

            if (check(TokenType.INTEGER_LITERAL)) {
                Token token = consume(TokenType.INTEGER_LITERAL);
                return new IntegerLiteral((Integer) token.getValue());
            }

            if (check(TokenType.REAL_LITERAL)) {
                Token token = consume(TokenType.REAL_LITERAL);
                return new RealLiteral((Double) token.getValue());
            }

            if (match(TokenType.TRUE)) {
                return new BooleanLiteral(true);
            }

            if (match(TokenType.FALSE)) {
                return new BooleanLiteral(false);
            }

            if (check(TokenType.IDENTIFIER)) {
                if (peekNext().getType() == TokenType.LEFT_PAREN) {
                    return parseRoutineCallExpression();
                }

                return parseModifiablePrimary();
            }

            throw error("Expected expression");
        }

        public ModifiablePrimary parseModifiablePrimary() {
            Token token = consume(TokenType.IDENTIFIER);

            ModifiablePrimary result =
                    new Identifier(token.getLexeme());

            while (check(TokenType.DOT)
                    || check(TokenType.LEFT_BRACKET)) {

                if (match(TokenType.DOT)) {
                    Token field = consume(TokenType.IDENTIFIER);

                    result = new FieldAccess(
                            result,
                            field.getLexeme()
                    );
                } else {
                    consume(TokenType.LEFT_BRACKET);

                    Expression index = parseExpression();

                    consume(TokenType.RIGHT_BRACKET);

                    result = new ArrayAccess(
                            result,
                            index
                    );
                }
            }

            return result;
        }

        public RoutineCallExpression parseRoutineCallExpression() {
            Token name = consume(TokenType.IDENTIFIER);

            consume(TokenType.LEFT_PAREN);

            List<Expression> arguments = new ArrayList<>();

            if (!check(TokenType.RIGHT_PAREN)) {
                arguments.add(parseExpression());

                while (match(TokenType.COMMA)) {
                    arguments.add(parseExpression());
                }
            }

            consume(TokenType.RIGHT_PAREN);

            return new RoutineCallExpression(
                    name.getLexeme(),
                    arguments
            );
        }



    }