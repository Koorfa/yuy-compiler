public class Token {
    private TokenType type;
    private String lexeme;
    private int line;
    private int column;
    private Object value;

    public Token(TokenType type, String lexeme, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
        this.value = null;
    }

    public Token(TokenType type, String lexeme, int line, int column, Object value) {
        this.type = type;
        this.lexeme = lexeme;
        this.line = line;
        this.column = column;
        this.value = value;
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + line + ":" + column + " " + value;
    }
}
