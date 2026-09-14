import java.util.ArrayList;
import java.util.List;

public class Lexer {
    private final String source;
    private int position;
    private int line;
    private int column;

    public Lexer(String source) {
        this.source = source;
        this.position = 0;
        this.line = 1;
        this.column = 1;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (!isEOF()) {
            skipUnnecessary();
            if (isEOF())
                break;
            tokens.add(nextToken());
        }

        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private Token nextToken() {
        char c = peekChar();
        if (Character.isLetter(c)) {
            return readIdentifier();
        }

        if (Character.isDigit(c)) {
            return readNumber();
        }

        switch (c) {
            case '+':
                return makeToken(TokenType.PLUS, "+");
            case '-':
                return makeToken(TokenType.MINUS, "-");
            case '*':
                return makeToken(TokenType.MULTIPLY, "*");
            case '%':
                return makeToken(TokenType.MODULO, "%");
            case '(':
                return makeToken(TokenType.LEFT_PAREN, "(");
            case ')':
                return makeToken(TokenType.RIGHT_PAREN, ")");
            case '[':
                return makeToken(TokenType.LEFT_BRACKET, "[");
            case ']':
                return makeToken(TokenType.RIGHT_BRACKET, "]");
            case ';':
                return makeToken(TokenType.SEMICOLON, ";");
            case ',':
                return makeToken(TokenType.COMMA, ",");
            case ':':
                if (peekNextChar() == '=') {
                    takeForward();
                    return makeToken(TokenType.ASSIGN, ":=");
                }
                return makeToken(TokenType.COLON, ":");
            case '/':
                if (peekNextChar() == '=') {
                    takeForward();
                    return makeToken(TokenType.NOT_EQUAL, "/=");
                }
                return makeToken(TokenType.DIVIDE, "/");
            case '<':
                if (peekNextChar() == '=') {
                    takeForward();
                    return makeToken(TokenType.LESS_EQUAL, "<=");
                }
                return makeToken(TokenType.LESS, "<");

            case '>':
                if (peekNextChar() == '=') {
                    takeForward();
                    return makeToken(TokenType.GREATER_EQUAL, ">=");
                }
                return makeToken(TokenType.GREATER, ">");
            case '.':
                if (peekNextChar() == '.') {
                    takeForward();
                    return makeToken(TokenType.RANGE, "..");
                }
                return makeToken(TokenType.DOT, ".");
            case '=':
                if (peekNextChar() == '>') {
                    takeForward();
                    return makeToken(TokenType.ARROW, "=>");
                }
                return makeToken(TokenType.EQUAL, "=");
            default:
                error("Unexpected character: '" + c + "'");
                return null;
        }
    }

    private Token readIdentifier() {
        int startLine = line;
        int startColumn = column;
        StringBuilder sb = new StringBuilder();

        while (!isEOF()) {
            char c = peekChar();
            if (Character.isLetterOrDigit(c) || c == '_') {
                sb.append(takeForward());
            } else {
                break;
            }
        }

        String lexeme = sb.toString();
        TokenType type = Keywords.getTokenType(lexeme);
        if (type == null) {
            type = TokenType.IDENTIFIER;
        }
        return new Token(type, lexeme, startLine, startColumn);
    }

    private Token readNumber() {
        int startLine = line;
        int startColumn = column;
        StringBuilder sb = new StringBuilder();
        boolean isReal = false;

        while (!isEOF()) {
            char c = peekChar();
            if (Character.isDigit(c)) {
                sb.append(takeForward());
            } else if (c == '.' && !isReal) {
                if (peekNextChar() == '.') {
                    break;
                }
                isReal = true;
                sb.append(takeForward());
            } else {
                break;
            }
        }

        String lexeme = sb.toString();

        try {

            if (isReal) {
                double value = Double.parseDouble(lexeme);
                return new Token(TokenType.REAL_LITERAL, lexeme, startLine, startColumn, value);
            } else {
                int value = Integer.parseInt(lexeme);
                return new Token(TokenType.INTEGER_LITERAL, lexeme, startLine, startColumn, value);
            }
        } catch (NumberFormatException e) {
            error("Number too large: '" + lexeme + "'", startLine, startColumn);
            return null;
        }
    }

    private void error(String message) {
        throw new LexerException(
                "Lexer error at line " + line + ", column " + column + ": " + message);
    }

    private void error(String message, int errorLine, int errorColumn) {
        throw new LexerException(
                "Lexer error at line " + errorLine + ", column " + errorColumn + ": " + message);
    }

    private Token makeToken(TokenType type, String lexeme) {
        int tokenLine = line;
        int tokenColumn = column;

        if (lexeme.length() == 2) {
            tokenColumn--;
        }

        Token token = new Token(type, lexeme, tokenLine, tokenColumn);
        takeForward();
        return token;
    }

    private char peekChar() {
        if (isEOF()) {
            return '\0';
        }
        return source.charAt(position);
    }

    private char peekNextChar() {
        if (position + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(position + 1);
    }

    private char takeForward() {
        char c = peekChar();
        position++;
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private void skipUnnecessary() {
        while (!isEOF()) {
            char c = peekChar();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                takeForward();
            } else if (c == '#') {
                while (!isEOF() && peekChar() != '\n') {
                    takeForward();
                }
            } else {
                break;
            }
        }
    }

    private boolean isEOF() {
        return position >= source.length();
    }
}
