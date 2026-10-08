import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            Path path = Paths.get(args[0]);
            String content = Files.readString(path);
            System.out.println(path);
            runTest(content);
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Please provide a file path as an argument.");
        }
    }

    private static void runTest(String source) {
        try {
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();

            for (Token token : tokens) {
                System.out.println(token);
            }
        } catch (LexerException e) {
            System.out.println(e.getMessage());
        }

        System.out.println();
    }
}