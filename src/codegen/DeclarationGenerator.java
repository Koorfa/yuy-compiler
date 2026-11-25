package codegen;

import ast.ASTNode;
import ast.Body;
import ast.Program;
import ast.declarations.Declaration;
import ast.declarations.ParameterDeclaration;
import ast.declarations.RoutineDeclaration;
import ast.declarations.VariableDeclaration;
import ast.statements.ForLoop;
import ast.statements.IfStatement;
import ast.statements.ReturnStatement;
import ast.statements.WhileLoop;
import ast.types.Type;
import java.util.Objects;
import semantic.Symbol;

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
                    "Invalid generated class name: " + ownerClass);
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

            context.getWriter().directive(
                    ".field public static " + variable.name
                            + " " + descriptor);

            VariableManager.Variable location = context.getVariableManager().declareGlobal(
                    variable.name,
                    ownerClass + "/" + variable.name,
                    descriptor);

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
            if (declaration instanceof VariableDeclaration variable) {
                Type type = prep.symbolFor(variable).getType();
                if (variable.initializer != null
                        || ArrayRecordSupport.isAggregate(type, context.getTypeMapper(), context.getSymbolTable())) {
                    hasInitializers = true;
                    break;
                }
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
            if (variable.initializer == null
                    && !ArrayRecordSupport.isAggregate(symbol.getType(), context.getTypeMapper(),
                            context.getSymbolTable())) {
                continue;
            }

            if (variable.initializer != null) {
                expressions.generate(variable.initializer);
            } else {
                generateArrayOrRecord(symbol.getType());
            }

            context.getVariableManager().storeVariable(symbol);
            stack.pop();

            stack.assertDepth(
                    0,
                    "global initializer of " + variable.name);
        }

        writer.emit("return");
        writer.endMethod(stack.getMaxDepth(), 0);
    }

    private void generateArrayOrRecord(Type type) {
        String spec = ArrayRecordSupport.spec(type, context.getTypeMapper(), context.getSymbolTable());
        String descriptor = context.getTypeMapper().getJasminType(type);
        context.getWriter().emit("ldc \"" + spec + "\"");
        context.getStackTracker().push("A");
        context.getWriter().emit("invokestatic YuyRuntime/allocate(Ljava/lang/String;)Ljava/lang/Object;");
        context.getStackTracker().pop();
        context.getStackTracker().push("A");
        String cast = descriptor.startsWith("L") && descriptor.endsWith(";")
                ? descriptor.substring(1, descriptor.length() - 1)
                : descriptor;
        context.getWriter().emit("checkcast " + cast);
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
                "public static " + routine.name + descriptor);

        try {
            for (ParameterDeclaration parameter : routine.parameters) {
                variables.registerLocal(prep.symbolFor(parameter));
            }

            statements.generateBody(routine.body);

            if (routine.returnType == null) {
                writer.emit("return");
            } else {
                if (!allPathsReturn(routine.body)) {
                    throw new CodeGenException(
                            "Function has no return statement: " + routine.name);
                }
                writer.emit("aconst_null");
                stack.push("A");
                writer.emit("athrow");
                stack.pop();
            }

            stack.assertDepth(0, "end of routine " + routine.name);

            writer.endMethod(
                    stack.getMaxDepth(),
                    variables.getMaxLocals());

        } finally {
            variables.endRoutine();
            context.setCurrentRoutineName(null);
            context.setCurrentReturnType(null);
        }
    }

    private boolean allPathsReturn(Body body) {
        if (body == null) {
            return false;
        }

        for (ASTNode node : body.nodes) {
            if (statementReturnsOnAllPaths(node)) {
                return true;
            }
        }

        return false;
    }

    private boolean statementReturnsOnAllPaths(ASTNode node) {
        if (node instanceof ReturnStatement) {
            return true;
        }

        if (node instanceof IfStatement conditional) {
            if (conditional.elseBody == null) {
                return false;
            }
            boolean thenReturns = allPathsReturn(conditional.thenBody);
            boolean elseReturns = allPathsReturn(conditional.elseBody);
            return thenReturns && elseReturns;
        }

        if (node instanceof WhileLoop || node instanceof ForLoop) {
            return false;
        }

        return false;
    }
}
