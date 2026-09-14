public enum TokenType {

    // Identifiers and literals
    IDENTIFIER,
    INTEGER_LITERAL,
    REAL_LITERAL,

    // Keywords
    VAR,
    IS,
    TYPE,
    INTEGER,
    REAL,
    BOOLEAN,
    TRUE,
    FALSE,
    RECORD,
    ARRAY,
    END,
    WHILE,
    LOOP,
    FOR,
    IN,
    REVERSE,
    IF,
    THEN,
    ELSE,
    PRINT,
    ROUTINE,
    BREAK,
    CONTINUE,
    AND,
    OR,
    XOR,
    NOT,

    // Operators
    PLUS,
    MINUS,
    MULTIPLY,
    DIVIDE,
    MODULO,
    EQUAL,
    NOT_EQUAL,
    LESS,
    LESS_EQUAL,
    GREATER,
    GREATER_EQUAL,
    ASSIGN,
    ARROW,

    // Punctuation
    COLON,
    COMMA,
    DOT,
    RANGE,
    SEMICOLON,
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACKET,
    RIGHT_BRACKET,

    EOF
}