
package codegen;

import ast.Program;
import ast.declarations.Declaration;
import ast.declarations.VariableDeclaration;
import semantic.Symbol;

import java.util.Objects;

import ast.declarations.RoutineDeclaration;
import ast.declarations.ParameterDeclaration;
import ast.statements.ReturnStatement;

public final class DeclarationGenerator {
    private final CodegenContext context;
    private final CodeGenPrepPass prep;
    private final String ownerClass;

    public DeclarationGenerator(CodegenContext context,
                                CodeGenPrepPass prep,
                                String ownerClass) {
        this.context = Objects.requireNonNull(context, "context");
        this.prep = Objects.requireNonNull(prep, "prep");

        if (ownerClass == null ||
                !ownerClass.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            throw new CodeGenException(
                    "Invalid generated class name: " + ownerClass
            );
        }

        this.ownerClass = ownerClass;
    }


    public void generateGlobalFields(Program program) {
        Objects.requireNonNull(program, "program");

        for (Declaration declaration : program.declarations) {
            if (!(declaration instanceof VariableDeclaration variable)) {
                continue;
            }

            Symbol symbol = prep.symbolFor(variable);

            String descriptor = context.getTypeMapper()
                    .getJasminType(symbol.getType());

            if (!"I".equals(descriptor) && !"D".equals(descriptor)) {
                throw new CodeGenException(
                        "Global type not yet implemented for "
                                + variable.name + ": " + descriptor
                );
            }

            context.getWriter().directive(
                    ".field public static " + variable.name
                            + " " + descriptor
            );

            VariableManager.Variable location =
                    context.getVariableManager().declareGlobal(
                            variable.name,
                            ownerClass + "/" + variable.name,
                            descriptor
                    );

            context.getVariableManager().bind(symbol, location);
        }
    }


    public void generateGlobalInitializer(
            Program program,
            ExpressionGenerator expressions) {

        Objects.requireNonNull(program, "program");
        Objects.requireNonNull(expressions, "expressions");

        boolean hasInitializers = false;

        for (Declaration declaration : program.declarations) {
            if (declaration instanceof VariableDeclaration variable
                    && variable.initializer != null) {
                hasInitializers = true;
                break;
            }
        }

        if (!hasInitializers) {
            return;
        }

        JasminWriter writer = context.getWriter();
        StackTracker stack = context.getStackTracker();

        stack.reset();
        writer.beginMethod("static <clinit>()V");

        for (Declaration declaration : program.declarations) {
            if (!(declaration instanceof VariableDeclaration variable)
                    || variable.initializer == null) {
                continue;
            }

            Symbol symbol = prep.symbolFor(variable);

            String descriptor = context.getTypeMapper()
                    .getJasminType(symbol.getType());

            if (!"I".equals(descriptor) && !"D".equals(descriptor)) {
                throw new CodeGenException(
                        "Global initializer type not supported: "
                                + variable.name
                );
            }

            expressions.generate(variable.initializer);

            context.getVariableManager().storeVariable(symbol);
            stack.pop();

            stack.assertDepth(
                    0,
                    "global initializer of " + variable.name
            );
        }

        writer.emit("return");
        writer.endMethod(stack.getMaxDepth(), 0);
    }



    public void generateRoutines(
            Program program,
            StatementGenerator statements) {

        Objects.requireNonNull(program, "program");
        Objects.requireNonNull(statements, "statements");

        for (Declaration declaration : program.declarations) {
            if (declaration instanceof RoutineDeclaration routine
                    && routine.body != null) {
                generateRoutine(routine, statements);
            }
        }
    }


    private void generateRoutine(
            RoutineDeclaration routine,
            StatementGenerator statements) {

        JasminWriter writer = context.getWriter();
        VariableManager variables = context.getVariableManager();
        StackTracker stack = context.getStackTracker();

        variables.beginRoutine();
        stack.reset();

        context.setCurrentRoutineName(routine.name);
        context.setCurrentReturnType(routine.returnType);

        String descriptor = context.getTypeMapper()
                .getJasminMethodDescriptor(routine);

        writer.beginMethod(
                "public static " + routine.name + descriptor
        );

        try {
            for (ParameterDeclaration parameter : routine.parameters) {
                variables.registerLocal(prep.symbolFor(parameter));
            }

            statements.generateBody(routine.body);

            if (routine.returnType == null) {
                writer.emit("return");
            } else if (routine.body.nodes.isEmpty()
                    || !(routine.body.nodes.get(
                    routine.body.nodes.size() - 1)
                    instanceof ReturnStatement)) {
                throw new CodeGenException(
                        "Function must end with return: " + routine.name
                );
            }

            stack.assertDepth(0, "end of routine " + routine.name);

            writer.endMethod(
                    stack.getMaxDepth(),
                    variables.getMaxLocals()
            );

        } finally {
            variables.endRoutine();
            context.setCurrentRoutineName(null);
            context.setCurrentReturnType(null);
        }
    }

}

