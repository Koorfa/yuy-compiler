import java.util.HashMap;
import java.util.Map;

public class Keywords {

    private static final Map<String, TokenType> keywords = new HashMap<>();

    static {
        keywords.put("var", TokenType.VAR);
        keywords.put("is", TokenType.IS);
        keywords.put("type", TokenType.TYPE);

        keywords.put("integer", TokenType.INTEGER);
        keywords.put("real", TokenType.REAL);
        keywords.put("boolean", TokenType.BOOLEAN);

        keywords.put("true", TokenType.TRUE);
        keywords.put("false", TokenType.FALSE);

        keywords.put("record", TokenType.RECORD);
        keywords.put("array", TokenType.ARRAY);
        keywords.put("end", TokenType.END);

        keywords.put("while", TokenType.WHILE);
        keywords.put("loop", TokenType.LOOP);

        keywords.put("for", TokenType.FOR);
        keywords.put("in", TokenType.IN);
        keywords.put("reverse", TokenType.REVERSE);

        keywords.put("if", TokenType.IF);
        keywords.put("then", TokenType.THEN);
        keywords.put("else", TokenType.ELSE);

        keywords.put("print", TokenType.PRINT);

        keywords.put("routine", TokenType.ROUTINE);

        keywords.put("and", TokenType.AND);
        keywords.put("or", TokenType.OR);
        keywords.put("xor", TokenType.XOR);
        keywords.put("not", TokenType.NOT);

        keywords.put("break", TokenType.BREAK);
        keywords.put("continue", TokenType.CONTINUE);
    }

    public static TokenType getTokenType(String word) {
        return keywords.get(word);
    }
}
