package codegen;

import java.util.Stack;
import java.util.Objects;

import ast.ASTNode;
import ast.Body;
import ast.declarations.RoutineDeclaration;
import ast.declarations.VariableDeclaration;
import ast.expressions.*;
import ast.types.*;
import semantic.Symbol;
import semantic.SymbolCategory;
import ast.statements.Statement;
import ast.statements.Assignment;
import ast.statements.BreakStatement;
import ast.statements.ContinueStatement;
import ast.statements.ForLoop;
import ast.statements.IfStatement;
import ast.statements.PrintStatement;
import ast.statements.ReturnStatement;
import ast.statements.RoutineCallStatement;
import ast.statements.WhileLoop;

public final class StatementGenerator {
    private final CodegenContext context;
    private final ExpressionGenerator expressions;
    private final CodeGenPrepPass prep;
    private final Stack<LoopContext> loops = new Stack<>();


    public StatementGenerator(
            CodegenContext context,
            ExpressionGenerator expressions,
            CodeGenPrepPass prep) {

        this.context = Objects.requireNonNull(context, "context");
        this.expressions = Objects.requireNonNull(expressions, "expressions");
        this.prep = Objects.requireNonNull(prep, "prep");
    }


    public void generate(Statement statement) {
        switch (statement) {
            case null -> throw new NullPointerException("statement");
            case Assignment s -> generateAssignment(s);
            case IfStatement s -> generateIf(s);
            case WhileLoop s -> generateWhile(s);
            case ForLoop s -> generateFor(s);
            case BreakStatement s -> generateBreak();
            case ContinueStatement s -> generateContinue();
            case ReturnStatement s -> generateReturn(s);
            case PrintStatement s -> generatePrint(s);
            case RoutineCallStatement s -> generateRoutineCall(s);
            default -> throw new CodeGenException("Unsupported statement: " + statement.getClass().getName());
        } 
    }

    public void generateBody(Body body) {
        if (body == null)
            return;
        for (ASTNode node : body.nodes) {
            if (node instanceof Statement statement) {
                generate(statement);
            } else if (node instanceof VariableDeclaration declaration) {
                generateLocalDeclaration(declaration);
            } else {
                throw new CodeGenException("Unsupported body node: " + node.getClass().getName());
            }
        }
    }

    private void generateLocalDeclaration(VariableDeclaration declaration) {
        Type type = declaration.type != null ? declaration.type
                : declaration.initializer == null ? null : declaration.initializer.resolvedType;
        if (type == null)
            throw new CodeGenException("Cannot determine type of local '" + declaration.name + "'");
        Symbol symbol = prep.symbolFor(declaration);
        if (symbol == null) {
            throw new CodeGenException("Unresolved local variable: " + declaration.name);
        }
        context.getVariableManager().registerLocal(symbol);
        if (declaration.initializer != null) {
            expressions.generate(declaration.initializer);
            context.getVariableManager().storeVariable(symbol);
            context.getStackTracker().pop();
        }
    }

    private void generateAssignment(Assignment assignment) {
        ModifiablePrimary target = assignment.target;
        if (target instanceof Identifier id) {
            if (id.resolvedSymbol == null)
                throw new CodeGenException("Unresolved assignment target: " + id.getName());
            expressions.generate(assignment.value);
            context.getVariableManager().storeVariable(id.resolvedSymbol);
            context.getStackTracker().pop();
            return;
        }
        if (target instanceof ArrayAccess array) {
            expressions.generate(array.getArray());

            expressions.generate(array.getIndex());

            context.getWriter().emit("iconst_1");
            context.getStackTracker().push("I");

            context.getWriter().emit("isub");

            context.getStackTracker().pop();
            context.getStackTracker().pop();
            context.getStackTracker().push("I");

            expressions.generate(assignment.value);

            String elementType = context.getTypeMapper().getJasminType(array.resolvedType);

            context.getWriter().emit(arrayStoreOpcode(elementType));

            context.getStackTracker().pop();
            context.getStackTracker().pop();
            context.getStackTracker().pop();

            return;
        }
        if (target instanceof FieldAccess field) {
            expressions.generate(field.getTarget());
            expressions.generate(assignment.value);
            String owner = context.getTypeMapper().getRecordClassName(field.getTarget().resolvedType);
            if (owner.startsWith("L") && owner.endsWith(";"))
                owner = owner.substring(1, owner.length() - 1);
            String descriptor = context.getTypeMapper().getJasminType(field.resolvedType);
            context.getWriter().emit("putfield " + owner + "/" + field.getFieldName() + " " + descriptor);
            context.getStackTracker().pop();
            context.getStackTracker().pop();
            return;
        }
        throw new CodeGenException("Unsupported assignment target: " + target.getClass().getSimpleName());
    }

    private void generateIf(IfStatement statement) {
        String thenLabel = label("if_then");
        String elseLabel = label("if_else");
        String endLabel = label("if_end");

        generateCondition(
                statement.condition,
                thenLabel,
                elseLabel);

        StackTracker tracker = context.getStackTracker();
        StackTracker.StackState entryState = tracker.snapshot();

        context.getWriter().emitLabel(thenLabel);
        generateScopedBody(statement.thenBody);

        StackTracker.StackState thenState = tracker.snapshot();

        context.getWriter().emit("goto " + endLabel);

        tracker.restore(entryState);

        context.getWriter().emitLabel(elseLabel);

        if (statement.elseBody != null) {
            generateScopedBody(statement.elseBody);
        }

        tracker.assertCompatible(
                thenState,
                "if branch merge");

        context.getWriter().emitLabel(endLabel);
    }

    private void generateWhile(WhileLoop loop) {
        String conditionLabel = label("while_condition");
        String bodyLabel = label("while_body");
        String exitLabel = label("while_end");
        context.getWriter().emitLabel(conditionLabel);
        generateCondition(loop.condition, bodyLabel, exitLabel);
        context.getWriter().emitLabel(bodyLabel);
        loops.push(new LoopContext(exitLabel, conditionLabel));
        try {
            generateScopedBody(loop.body);
        } finally {
            loops.pop();
        }
        context.getWriter().emit("goto " + conditionLabel);
        context.getWriter().emitLabel(exitLabel);
    }

    private void generateFor(ForLoop loop) {
        context.getVariableManager().enterScope();

        try {
            if (loop.end == null) {
                generateArrayForEach(loop);
            } else {
                generateIntegerRangeFor(loop);
            }
        } finally {
            context.getVariableManager().exitScope();
        }
    }

    private void generateIntegerRangeFor(ForLoop loop) {
        Symbol loopSymbol = prep.symbolFor(loop);

        if (loopSymbol == null) {
            throw new CodeGenException(
                    "Unresolved for-loop variable: " + loop.variable);
        }

        int loopSlot = context.getVariableManager().registerLocal(loopSymbol);

        int startSlot = context.getVariableManager().allocateTemporary(1);
        int endSlot = context.getVariableManager().allocateTemporary(1);

        String conditionLabel = label("for_condition");
        String continueLabel = label("for_continue");
        String exitLabel = label("for_end");

        expressions.generate(loop.start);

        context.getWriter().emit("istore " + startSlot);
        context.getStackTracker().pop();

        expressions.generate(loop.end);

        context.getWriter().emit("istore " + endSlot);
        context.getStackTracker().pop();

        context.getWriter().emit(
                "iload " + (loop.reverse ? endSlot : startSlot));
        context.getStackTracker().push("I");

        context.getVariableManager().storeVariable(loopSymbol);
        context.getStackTracker().pop();

        context.getWriter().emitLabel(conditionLabel);

        context.getWriter().emit("iload " + loopSlot);
        context.getStackTracker().push("I");

        context.getWriter().emit(
                "iload " + (loop.reverse ? startSlot : endSlot));
        context.getStackTracker().push("I");

        context.getWriter().emit(
                (loop.reverse ? "if_icmplt " : "if_icmpgt ") + exitLabel);

        context.getStackTracker().pop();
        context.getStackTracker().pop();

        loops.push(new LoopContext(exitLabel, continueLabel));

        try {
            generateBody(loop.body);
        } finally {
            loops.pop();
        }

        context.getWriter().emitLabel(continueLabel);

        context.getWriter().emit(
                "iinc " + loopSlot + (loop.reverse ? " -1" : " 1"));

        context.getWriter().emit("goto " + conditionLabel);
        context.getWriter().emitLabel(exitLabel);
    }

    private void generateArrayForEach(ForLoop loop) {
        Type sourceType = semantic.TypeConformance.resolveType(
                loop.start.resolvedType,
                context.getSymbolTable());

        if (!(sourceType instanceof ArrayType arrayType)) {
            throw new CodeGenException("For-each source is not a resolved array");
        }

        Type elementType = semantic.TypeConformance.resolveType(
                arrayType.elementType,
                context.getSymbolTable());

        String jasminElementType = context.getTypeMapper().getJasminType(elementType);

        Symbol loopSymbol = prep.symbolFor(loop);

        if (loopSymbol == null) {
            throw new CodeGenException("Unresolved for-each variable: " + loop.variable);
        }

        context.getVariableManager().registerLocal(loopSymbol);

        int arraySlot = context.getVariableManager().allocateTemporary(1);

        int indexSlot = context.getVariableManager().allocateTemporary(1);

        String conditionLabel = label("foreach_condition");
        String continueLabel = label("foreach_continue");
        String exitLabel = label("foreach_end");

        expressions.generate(loop.start);

        context.getWriter().emit("astore " + arraySlot);
        context.getStackTracker().pop();

        if (loop.reverse) {
            context.getWriter().emit("aload " + arraySlot);
            context.getStackTracker().push("A");

            context.getWriter().emit("arraylength");
            context.getStackTracker().pop();
            context.getStackTracker().push("I");

            context.getWriter().emit("iconst_1");
            context.getStackTracker().push("I");

            context.getWriter().emit("isub");
            context.getStackTracker().pop();
            context.getStackTracker().pop();
            context.getStackTracker().push("I");
        } else {
            context.getWriter().emit("iconst_0");
            context.getStackTracker().push("I");
        }

        context.getWriter().emit("istore " + indexSlot);
        context.getStackTracker().pop();

        context.getWriter().emitLabel(conditionLabel);

        context.getWriter().emit("iload " + indexSlot);
        context.getStackTracker().push("I");

        if (loop.reverse) {
            context.getWriter().emit("iflt " + exitLabel);
            context.getStackTracker().pop();
        } else {
            context.getWriter().emit("aload " + arraySlot);
            context.getStackTracker().push("A");

            context.getWriter().emit("arraylength");
            context.getStackTracker().pop();
            context.getStackTracker().push("I");

            context.getWriter().emit("if_icmpge " + exitLabel);

            context.getStackTracker().pop();
            context.getStackTracker().pop();
        }

        context.getWriter().emit("aload " + arraySlot);
        context.getStackTracker().push("A");

        context.getWriter().emit("iload " + indexSlot);
        context.getStackTracker().push("I");

        context.getWriter().emit(
                arrayLoadOpcode(jasminElementType));

        context.getStackTracker().pop();
        context.getStackTracker().pop();

        context.getStackTracker().push(
                stackTypeForDescriptor(jasminElementType));

        context.getVariableManager().storeVariable(loopSymbol);
        context.getStackTracker().pop();

        loops.push(new LoopContext(exitLabel, continueLabel));

        try {
            generateBody(loop.body);
        } finally {
            loops.pop();
        }

        context.getWriter().emitLabel(continueLabel);

        context.getWriter().emit(
                "iinc " + indexSlot + (loop.reverse ? " -1" : " 1"));

        context.getWriter().emit("goto " + conditionLabel);

        context.getWriter().emitLabel(exitLabel);
    }

    private void generateCondition(Expression condition, String trueLabel, String falseLabel) {
        expressions.generate(condition);
        context.getWriter().emit("ifne " + trueLabel);
        context.getStackTracker().pop();
        context.getWriter().emit("goto " + falseLabel);
    }

    private void generateBreak() {
        if (loops.isEmpty())
            throw new CodeGenException("break outside a loop");
        context.getWriter().emit("goto " + loops.peek().getBreakLabel());
    }

    private void generateContinue() {
        if (loops.isEmpty())
            throw new CodeGenException("continue outside a loop");
        context.getWriter().emit("goto " + loops.peek().getContinueLabel());
    }

    private void generateReturn(ReturnStatement statement) {
        if (statement.value == null) {
            context.getWriter().emit("return");
            return;
        }
        expressions.generate(statement.value);
        String returnType = context.getTypeMapper().getJasminType(
                context.getCurrentReturnType() != null ? context.getCurrentReturnType() : statement.value.resolvedType);
        context.getWriter().emit(returnOpcode(returnType));
        context.getStackTracker().pop();
    }

    private void generatePrint(PrintStatement statement) {
        for (Expression expression : statement.expressions) {
            context.getWriter().emit("getstatic java/lang/System/out Ljava/io/PrintStream;");
            context.getStackTracker().push("A");
            expressions.generate(expression);
            String descriptor = printDescriptor(expression);
            context.getWriter().emit("invokevirtual java/io/PrintStream/println(" + descriptor + ")V");
            context.getStackTracker().pop();
            context.getStackTracker().pop();
        }
    }

    private String printDescriptor(Expression expression) {
        Type type = expression.resolvedType;
        if (type == null) {
            if (expression instanceof IntegerLiteral)
                return "I";
            if (expression instanceof RealLiteral)
                return "D";
            if (expression instanceof BooleanLiteral)
                return "Z";
            return "Ljava/lang/Object;";
        }
        Type resolved = semantic.TypeConformance.resolveType(type, context.getSymbolTable());
        if (resolved instanceof ast.types.PrimitiveType primitive) {
            String name = primitive.typeName.toLowerCase();
            if (name.equals("boolean"))
                return "Z";
            if (name.equals("real"))
                return "D";
            if (name.equals("integer"))
                return "I";
        }
        String jasmin = context.getTypeMapper().getJasminType(type);
        return jasmin.equals("D") ? "D" : jasmin.equals("I") ? "I" : "Ljava/lang/Object;";
    }

    private void generateRoutineCall(RoutineCallStatement statement) {
        Symbol symbol = context.getSymbolTable().lookup(statement.name);

        if (symbol == null || symbol.getCategory() != SymbolCategory.ROUTINE
                || !(symbol.getDeclaration() instanceof RoutineDeclaration routine)) {
            throw new CodeGenException("Unresolved routine call: " + statement.name);
        }

        for (Expression expression : statement.arguments) {
            expressions.generate(expression);
        }

        String descriptor = context.getTypeMapper().getJasminMethodDescriptor(routine);
        context.getWriter().emit("invokestatic Main/" + statement.name + descriptor);

        for (int i = 0; i < statement.arguments.size(); i++) {
            context.getStackTracker().pop();
        }

        if (routine.returnType != null) {
            String resultType = context.getTypeMapper().getJasminType(routine.returnType);
            context.getWriter().emit(resultType.equals("D") ? "pop2" : "pop");
            context.getStackTracker().pop();
        }
    }

    private void generateScopedBody(Body body) {
        context.getVariableManager().enterScope();

        try {
            generateBody(body);
        } finally {
            context.getVariableManager().exitScope();
        }
    }

    private String label(String prefix) {
        return context.getLabelManager().getNewLabel(prefix);
    }

    private String returnOpcode(String type) {
        if ("D".equals(type))
            return "dreturn";
        if ("I".equals(type))
            return "ireturn";
        if ("V".equals(type))
            return "return";
        return "areturn";
    }

    private String arrayLoadOpcode(String type) {
        if ("I".equals(type))
            return "iaload";
        if ("D".equals(type))
            return "daload";
        return "aaload";
    }

    private String arrayStoreOpcode(String type) {
        if ("I".equals(type))
            return "iastore";
        if ("D".equals(type))
            return "dastore";
        return "aastore";
    }

    private String stackTypeForDescriptor(String type) {
        return switch (type) {
            case "I", "Z" -> "I";
            case "D" -> "D";
            default -> "A";
        };
    }

    public int getMaxLocals() {
        return context.getVariableManager().getMaxLocals();
    }

    public int getMaxStack() {
        return context.getStackTracker().getMaxDepth();
    }
}
