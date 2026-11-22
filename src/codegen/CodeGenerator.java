
package codegen;

import ast.Program;
import semantic.SymbolTable;
import java.util.Objects;

public final class CodeGenerator {

    public String generate(Program program, SymbolTable symbols) {
        Objects.requireNonNull(program, "program");
        Objects.requireNonNull(symbols, "symbols");

        CodeGenPrepPass prep = new CodeGenPrepPass(symbols);
        prep.prepare(program);

        JasminWriter writer = new JasminWriter();
        TypeMapper mapper = new TypeMapper(symbols);

        VariableManager variables = new VariableManager(
                writer,
                new LocalAllocator(),
                mapper
        );

        CodegenContext context = new CodegenContext(
                symbols,
                writer,
                new LabelManager(),
                new StackTracker(),
                variables
        );

        ExpressionGenerator expressions =
                new ExpressionGenerator(context);

        StatementGenerator statements =
                new StatementGenerator(context, expressions, prep);

        DeclarationGenerator declarations =
                new DeclarationGenerator(context, prep, "Main");

        writer.directive(".class public Main");
        writer.directive(".super java/lang/Object");

        declarations.generateGlobalFields(program);
        declarations.generateGlobalInitializer(program, expressions);
        declarations.generateRoutines(program, statements);

        return writer.getCode();
    }
}
