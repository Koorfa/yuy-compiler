
import ast.Program;
import codegen.CodeGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lexer.Lexer;
import lexer.LexerException;
import lexer.Token;
import optimizer.ConstantExpressionOptimizer;
import optimizer.UnreachableCodeOptimizer;
import parser.ParseException;
import parser.Parser;
import semantic.DeclarationsAnalyzer;
import semantic.SemanticException;
import semantic.StatementsAnalyzer;

public class Main {

    public static void main(String[] args) {
        if (args.length == 3 && args[0].equals("--jasmin")) {
            try {
                String source = Files.readString(Path.of(args[1]));
                generateJasmin(source, Path.of(args[2]));
            } catch (IOException e) {
                System.err.println("File error: " + e.getMessage());
                System.exit(1);
            } catch (RuntimeException e) {
                System.err.println("Compilation error: " + e.getMessage());
                System.exit(1);
            }
            return;
        }

        if (args.length != 1) {
            System.out.println(
                    "Usage: java Main <source.yuy>\n" +
                            "   or: java Main --jasmin <source.yuy> <output.j>"
            );
            return;
        }

        try {
            String source = Files.readString(Path.of(args[0]));
            runTest(source);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static void generateJasmin(String source, Path output)
            throws IOException {

        Program program = new Parser(
                new Lexer(source).tokenize()
        ).parseProgram();

        DeclarationsAnalyzer declarations = new DeclarationsAnalyzer();
        declarations.analyze(program);

        new StatementsAnalyzer(
                declarations.getSymbolTable()
        ).analyze(program);

        String jasmin = new CodeGenerator().generate(
                program, declarations.getSymbolTable()
        );

        Files.writeString(output, jasmin);
        System.out.println("Generated Jasmin: " + output.toAbsolutePath());
    }

    private static void runTest(String source) {
        try {
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            Program program = parser.parseProgram();

            DeclarationsAnalyzer pass1 = new DeclarationsAnalyzer();
            pass1.analyze(program);

            StatementsAnalyzer pass2 =
                    new StatementsAnalyzer(pass1.getSymbolTable());
            pass2.analyze(program);

            ConstantExpressionOptimizer constOptimizer =
                    new ConstantExpressionOptimizer();
            constOptimizer.optimize(program);

            UnreachableCodeOptimizer unreachableOptimizer =
                    new UnreachableCodeOptimizer();
            unreachableOptimizer.optimize(program);

            program.print("");

        } catch (LexerException | ParseException | SemanticException e) {
            System.out.println("Compilation error: " + e.getMessage());
        }
    }
}
