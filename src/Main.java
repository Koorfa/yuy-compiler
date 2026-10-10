import ast.Program;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import lexer.Lexer;
import lexer.LexerException;
import lexer.Token;
import parser.Parser;
import parser.ParseException;

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

            Parser parser = new Parser(tokens);
            Program program = parser.parseProgram();

            program.print("");
        } catch (LexerException e) {
            System.out.println("Lexer error: " + e.getMessage());
        } catch (ParseException e) {
            System.out.println("Parser error: " + e.getMessage());
        }

        System.out.println();
    }
}