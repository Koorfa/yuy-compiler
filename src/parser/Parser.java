package parser;

import ast.*;
import java.util.ArrayList;
import java.util.List;
import lexer.Token;
import lexer.TokenType;

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

    public Program parseProgram() {
        List<Declaration> declarations = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            declarations.add(parseDeclaration());
            skipSeparators();
        }
        return new Program(declarations);
    }

    public Declaration parseDeclaration() {
        if (check(TokenType.VAR) || check(TokenType.TYPE)) {
            return parseSimpleDeclaration();
        } else if (check(TokenType.ROUTINE)) {
            return parseRoutineDeclaration();
        } else {
            throw error("Expected 'var', 'type', or 'routine'");
        }
    }

    public SimpleDeclaration parseSimpleDeclaration() {
        if (match(TokenType.VAR)) {
            return parseVariableDeclaration();
        } else if (match(TokenType.TYPE)) {
            return parseTypeDeclaration();
        }
        throw error("Expected 'var' or 'type'");
    }

    public VariableDeclaration parseVariableDeclaration() {
        String name = consume(TokenType.IDENTIFIER).getLexeme();
        Type type = null;
        Expression initializer = null;

        if (match(TokenType.COLON)) {
            type = parseType();
        }

        if (match(TokenType.IS)) {
            initializer = parseExpression();
        }

        if (type == null && initializer == null) {
            throw error("Variable must have either a type or an initializer");
        }

        return new VariableDeclaration(name, type, initializer);
    }

    public TypeDeclaration parseTypeDeclaration() {
        String name = consume(TokenType.IDENTIFIER).getLexeme();
        consume(TokenType.IS);
        Type type = parseType();
        return new TypeDeclaration(name, type);
    }

    public RoutineDeclaration parseRoutineDeclaration() {
        consume(TokenType.ROUTINE);
        RoutineHeader header = parseRoutineHeader();

        Body body = null;
        Expression expressionBody = null;

        if (match(TokenType.IS)) {
            body = parseBody();
            consume(TokenType.END);
        } else if (match(TokenType.ARROW)) {
            expressionBody = parseExpression();
        }

        return new RoutineDeclaration(header.name, header.parameters,
                                    header.returnType, body, expressionBody);
    }

    public static class RoutineHeader {
        public String name;
        public List<ParameterDeclaration> parameters;
        public Type returnType;

        public RoutineHeader(String name, List<ParameterDeclaration> parameters, Type returnType) {
            this.name = name;
            this.parameters = parameters;
            this.returnType = returnType;
        }
    }

    public RoutineHeader parseRoutineHeader() {
        String name = consume(TokenType.IDENTIFIER).getLexeme();
        consume(TokenType.LEFT_PAREN);
        List<ParameterDeclaration> parameters = parseParameters();
        consume(TokenType.RIGHT_PAREN);

        Type returnType = null;
        if (match(TokenType.COLON)) {
            returnType = parseType();
        }

        return new RoutineHeader(name, parameters, returnType);
    }

    public Body parseRoutineBody() {
        return parseBody();
    }

    public List<ParameterDeclaration> parseParameters() {
        List<ParameterDeclaration> params = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            params.add(parseParameterDeclaration());
            while (match(TokenType.COMMA)) {
                params.add(parseParameterDeclaration());
            }
        }
        return params;
    }

    public ParameterDeclaration parseParameterDeclaration() {
        String name = consume(TokenType.IDENTIFIER).getLexeme();
        consume(TokenType.COLON);
        Type type = parseType();
        return new ParameterDeclaration(name, type);
    }

    public Body parseBody() {
        List<SimpleDeclaration> declarations = new ArrayList<>();
        List<Statement> statements = new ArrayList<>();

        while (!check(TokenType.END) && !check(TokenType.EOF)) {
            if (check(TokenType.VAR) || check(TokenType.TYPE)) {
                declarations.add(parseSimpleDeclaration());
            } else {
                statements.add(parseStatement());
            }
            skipSeparators();
        }
        return new Body(declarations, statements);
    }

    public Type parseType() {
        if (check(TokenType.INTEGER) || check(TokenType.REAL) || check(TokenType.BOOLEAN)) {
            return parsePrimitiveType();
        } else if (check(TokenType.ARRAY)) {
            return parseArrayType();
        } else if (check(TokenType.RECORD)) {
            return parseRecordType();
        } else if (check(TokenType.IDENTIFIER)) {
            return parseIdentifierType();
        }
        throw error("Expected type (integer, real, boolean, array, record, or identifier)");
    }

    public PrimitiveType parsePrimitiveType() {
        String typeName = currentToken.getLexeme();
        consume(currentToken.getType());
        return new PrimitiveType(typeName);
    }

    public ArrayType parseArrayType() {
        consume(TokenType.ARRAY);
        Expression size = null;

        if (match(TokenType.LEFT_BRACKET)) {
            size = parseExpression();
            consume(TokenType.RIGHT_BRACKET);
        }

        Type elementType = parseType();
        return new ArrayType(size, elementType);
    }

    public RecordType parseRecordType() {
        consume(TokenType.RECORD);
        List<VariableDeclaration> members = new ArrayList<>();

        while (!check(TokenType.END) && !check(TokenType.EOF)) {
            members.add(parseVariableDeclaration());
            skipSeparators();
        }

        consume(TokenType.END);
        return new RecordType(members);
    }

    public IdentifierType parseIdentifierType() {
        String name = consume(TokenType.IDENTIFIER).getLexeme();
        return new IdentifierType(name);
    }

    public Statement parseStatement() {
        throw new UnsupportedOperationException("67");
    }

}