import java.util.List;



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


}