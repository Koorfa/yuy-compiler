package parser;

import ast.*;
import ast.declarations.*;
import ast.expressions.*;
import ast.statements.*;
import ast.types.*;
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

    /**
     * Parses declarations until the end-of-file token is reached.
     *
     * @return the program containing the parsed declarations
     * @throws ParseException if the input contains an invalid declaration
     */
    public Program parseProgram() {
        List<Declaration> declarations = new ArrayList<>();
        while (!check(TokenType.EOF)) {
            declarations.add(parseDeclaration());
            skipSeparators();
        }
        return new Program(declarations);
    }

    /**
     * Parses one variable, type, or routine declaration.
     *
     * @return the parsed declaration
     * @throws ParseException if the next token does not begin a declaration
     */
    public Declaration parseDeclaration() {
        if (check(TokenType.VAR) || check(TokenType.TYPE)) {
            return parseSimpleDeclaration();
        } else if (check(TokenType.ROUTINE)) {
            return parseRoutineDeclaration();
        } else {
            throw error("Expected 'var', 'type', or 'routine'");
        }
    }

    /**
     * Parses a variable or type declaration.
     *
     * @return the parsed simple declaration
     * @throws ParseException if the next token is neither {@code var} nor {@code type}
     */
    public SimpleDeclaration parseSimpleDeclaration() {
        if (match(TokenType.VAR)) {
            return parseVariableDeclaration();
        } else if (match(TokenType.TYPE)) {
            return parseTypeDeclaration();
        }
        throw error("Expected 'var' or 'type'");
    }

    /**
     * Parses a variable declaration of the form {@code var name [: type] [is expression]}.
     * At least a type or an initializer must be present.
     *
     * @return the parsed variable declaration
     * @throws ParseException if the declaration is missing both a type and an initializer
     */
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

    /**
     * Parses a named type declaration of the form {@code type name is type}.
     *
     * @return the parsed type declaration
     * @throws ParseException if the declaration does not match the required syntax
     */
    public TypeDeclaration parseTypeDeclaration() {
        String name = consume(TokenType.IDENTIFIER).getLexeme();
        consume(TokenType.IS);
        Type type = parseType();
        return new TypeDeclaration(name, type);
    }

    /**
     * Parses a routine declaration, including its header and optional body.
     * A body may be a block introduced by {@code is} or a single expression
     * introduced by {@code ->}.
     *
     * @return the parsed routine declaration
     * @throws ParseException if the routine declaration is malformed
     */
    public RoutineDeclaration parseRoutineDeclaration() {
        consume(TokenType.ROUTINE);
        RoutineHeader header = parseRoutineHeader();

        Body body = null;

        if (match(TokenType.IS)) {
            body = parseBody();
            consume(TokenType.END);
        } else if (match(TokenType.ARROW)) {
            Expression expr = parseExpression();
            ReturnStatement ret = new ReturnStatement(expr);
            List<ASTNode> nodes = new ArrayList<>();
            nodes.add(ret);
            body = new Body(nodes);
        }

        return new RoutineDeclaration(header.name, header.parameters,
                                    header.returnType, body);
    }

    /**
     * Holds the name, parameters, and optional result type parsed from a routine header.
     */
    public static class RoutineHeader {
        /** The routine's identifier. */
        public String name;
        /** The routine's parameters, in declaration order. */
        public List<ParameterDeclaration> parameters;
        /** The result type, or {@code null} when no result type is declared. */
        public Type returnType;

        /**
         * Creates a parsed routine header.
         *
         * @param name the routine's identifier
         * @param parameters the routine's parameters
         * @param returnType the result type, or {@code null} when absent
         */
        public RoutineHeader(String name, List<ParameterDeclaration> parameters, Type returnType) {
            this.name = name;
            this.parameters = parameters;
            this.returnType = returnType;
        }
    }

    /**
     * Parses a routine header: its name, parameter list, and optional result type.
     *
     * @return the parsed header
     * @throws ParseException if the header is malformed
     */
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

    /**
     * Parses a possibly empty, comma-separated list of routine parameters.
     *
     * @return the parsed parameters, in source order
     * @throws ParseException if any parameter is malformed
     */
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

    /**
     * Parses one parameter declaration of the form {@code name: type}.
     *
     * @return the parsed parameter
     * @throws ParseException if the parameter is malformed
     */
    public ParameterDeclaration parseParameterDeclaration() {
        String name = consume(TokenType.IDENTIFIER).getLexeme();
        consume(TokenType.COLON);
        Type type = parseType();
        return new ParameterDeclaration(name, type);
    }

    public Body parseBody() {
        List<ASTNode> nodes = new ArrayList<>();

        while (!check(TokenType.END) && !check(TokenType.ELSE) && !check(TokenType.EOF)) {
            if (check(TokenType.VAR) || check(TokenType.TYPE)) {
                nodes.add(parseSimpleDeclaration());
            } else if (check(TokenType.ARROW)) {
                nodes.add(parseReturnStatement());
            } else {
                nodes.add(parseStatement());
            }
            skipSeparators();
        }
        return new Body(nodes);
    }

    /**
     * Parses a primitive, array, record, or identifier-based type.
     *
     * @return the parsed type
     * @throws ParseException if the next token does not begin a type
     */
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

    /**
     * Parses one of the built-in scalar types: {@code integer}, {@code real},
     * or {@code boolean}.
     *
     * @return the parsed primitive type
     * @throws ParseException if the next token is not a primitive type
     */
    public PrimitiveType parsePrimitiveType() {
        String typeName = currentToken.getLexeme();
        consume(currentToken.getType());
        return new PrimitiveType(typeName);
    }

    /**
     * Parses an array type of the form {@code array [size] elementType};
     * the bracketed size expression is optional.
     *
     * @return the parsed array type
     * @throws ParseException if the array type is malformed
     */
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

    /**
     * Parses a record type containing {@code var} member declarations and
     * terminated by {@code end}.
     *
     * @return the parsed record type
     * @throws ParseException if a member or the terminating {@code end} is missing
     */
    public RecordType parseRecordType() {
        consume(TokenType.RECORD);
        List<VariableDeclaration> members = new ArrayList<>();

        while (!check(TokenType.END) && !check(TokenType.EOF)) {
            consume(TokenType.VAR);
            members.add(parseVariableDeclaration());
            skipSeparators();
        }

        consume(TokenType.END);
        return new RecordType(members);
    }

    /**
     * Parses an identifier used as a type name.
     *
     * @return the parsed identifier type
     * @throws ParseException if the next token is not an identifier
     */
    public IdentifierType parseIdentifierType() {
        String name = consume(TokenType.IDENTIFIER).getLexeme();
        return new IdentifierType(name);
    }

    public Statement parseStatement() {
        if (check(TokenType.IF)) return parseIfStatement();
        if (check(TokenType.WHILE)) return parsWhileLoop();
        if (check(TokenType.FOR)) return parseForLoop();
        if (check(TokenType.PRINT)) return parsePrintStatement();
        if (check(TokenType.BREAK)) return parseBreakStatement();
        if (check(TokenType.CONTINUE)) return parseContinueStatement();
        if (check(TokenType.IDENTIFIER)) return parseAssignment();
        throw error("Exprected statement");
    }

    public IfStatement parseIfStatement() {
        consume(TokenType.IF);
        Expression condition = parseExpression();
        consume(TokenType.THEN);
        Body thenBody = parseBody();
        Body elseBody = null;
        if (match(TokenType.ELSE)) {
            elseBody = parseBody();
        }
        consume(TokenType.END);
        return new IfStatement(condition, thenBody, elseBody);
    }

    public WhileLoop parsWhileLoop() {
        consume(TokenType.WHILE);
        Expression condition = parseExpression();
        consume(TokenType.LOOP);
        Body body = parseBody();
        consume(TokenType.END);
        return new WhileLoop(condition, body);
    }

    public ForLoop parseForLoop() {
        consume(TokenType.FOR);
        String variable = consume(TokenType.IDENTIFIER).getLexeme();
        consume(TokenType.IN);
        Expression start = parseExpression();
        Expression end = null;
        if (match(TokenType.RANGE)) {
            end = parseExpression();
        }
        boolean reverse = match(TokenType.REVERSE);
        consume(TokenType.LOOP);
        Body body = parseBody();
        consume(TokenType.END);
        return new ForLoop(variable, start, end, reverse, body);
    }

    public PrintStatement parsePrintStatement() {
        consume(TokenType.PRINT);
        List<Expression> expressions = new ArrayList<>();
        expressions.add(parseExpression());

        while (match(TokenType.COMMA)) {
            expressions.add(parseExpression());
        }
        return new PrintStatement(expressions);
    }

    public BreakStatement parseBreakStatement() {
        consume(TokenType.BREAK);
        return new BreakStatement();
    }

    public ContinueStatement parseContinueStatement() {
        consume(TokenType.CONTINUE);
        return new ContinueStatement();
    }

    public Statement parseAssignment() {
        if (peekNext().getType() == TokenType.LEFT_PAREN) {
            return parseRoutineCallStatement();
        }

        ModifiablePrimary target = parseModifiablePrimary();
        consume(TokenType.ASSIGN);
        Expression value = parseExpression();
        return new Assignment(target, value);
    }

    public RoutineCallStatement parseRoutineCallStatement() {
        String name = consume(TokenType.IDENTIFIER).getLexeme();
        consume(TokenType.LEFT_PAREN);
        List<Expression> args = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            args.add(parseExpression());
            while (match(TokenType.COMMA)) {
                args.add(parseExpression());
            }
        }
        consume(TokenType.RIGHT_PAREN);
        return new RoutineCallStatement(name, args);
    }

    public ReturnStatement parseReturnStatement() {
        consume(TokenType.ARROW);
        Expression value = parseExpression();
        return new ReturnStatement(value);
    }
}