
package codegen;

import ast.Program;
import semantic.SymbolTable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CodeGenerator {
    public static final String OUTPUT_CLASS = "YuyProgram";

    public Map<String, String> generate(Program program, SymbolTable symbols) {
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
                variables,
                mapper
        );

        ExpressionGenerator expressions =
                new ExpressionGenerator(context);

        StatementGenerator statements =
                new StatementGenerator(context, expressions, prep);

        Map<String, String> artifacts = new LinkedHashMap<>();
        artifacts.putAll(new RecordClassGenerator(context.getTypeMapper(), symbols).generate(program));

        DeclarationGenerator declarations =
                new DeclarationGenerator(context, prep, OUTPUT_CLASS);

        writer.directive(".class public " + OUTPUT_CLASS);
        writer.directive(".super java/lang/Object");

        declarations.generateGlobalFields(program);
        declarations.generateGlobalInitializer(program, expressions);
        declarations.generateRoutines(program, statements);

        artifacts.put(OUTPUT_CLASS + ".j", writer.getCode());

        return artifacts;
    }
}
