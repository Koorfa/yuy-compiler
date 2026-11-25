package codegen;

import ast.declarations.RoutineDeclaration;
import ast.expressions.*;
import ast.types.*;
import lexer.TokenType;
import semantic.Symbol;
import semantic.SymbolCategory;

/**
 * Generates Jasmin instructions for Yuy expressions.
 * It dispatches each expression node to the appropriate generator, tracks JVM
 * operand-stack changes, and applies semantic conversions after evaluation.
 */
public class ExpressionGenerator {
    private final CodegenContext context;
    private final TypeConversionGenerator conversionGenerator;

    /**
     * Creates an expression generator for the given code generation context.
     *
     * @param context shared code generation state
     */
    public ExpressionGenerator(CodegenContext context) {
        this.context = context;
        this.conversionGenerator = new TypeConversionGenerator(context);
    }

    /**
     * Generates Jasmin instructions that evaluate an expression.
     *
     * @param expr expression to generate; {@code null} is ignored
     * @throws CodeGenException if the expression or a referenced symbol is unsupported
     *         or has not been resolved
     */
    public void generate(Expression expr) {
        if (expr == null) return;

        switch (expr) {
            case IntegerLiteral lit -> generateIntegerLiteral(lit);
            case RealLiteral lit -> generateRealLiteral(lit);
            case BooleanLiteral lit -> generateBooleanLiteral(lit);
            case Identifier id -> generateIdentifier(id);
            case BinaryExpression bin -> generateBinaryExpression(bin);
            case UnaryExpression un -> generateUnaryExpression(un);
            case FieldAccess field -> generateFieldAccess(field);
            case ArrayAccess array -> generateArrayAccess(array);
            case RoutineCallExpression call -> generateRoutineCall(call);
            default -> throw new CodeGenException("Unknown expression type: " + expr.getClass().getSimpleName());
        }

        // Apply type conversion if the semantic analyzer determined one is needed 
        // (this handles conversions for assignments and argument passing seamlessly).
        conversionGenerator.applyConversion(expr);
    }

    private void generateIntegerLiteral(IntegerLiteral lit) {
        context.getWriter().emitLdc(lit.getValue());
        context.getStackTracker().push("I");
    }

    private void generateRealLiteral(RealLiteral lit) {
        context.getWriter().emitLdc2_w(lit.getValue());
        context.getStackTracker().push("D");
    }

    private void generateBooleanLiteral(BooleanLiteral lit) {
        if (lit.getValue()) {
            context.getWriter().emit("iconst_1");
        } else {
            context.getWriter().emit("iconst_0");
        }
        context.getStackTracker().push("I");
    }

    private void generateIdentifier(Identifier id) {
        Symbol symbol = id.resolvedSymbol;
        if (symbol == null) {
            throw new CodeGenException("Unresolved identifier: " + id.getName());
        }
        if (symbol.getCategory() == SymbolCategory.VARIABLE) {
            context.getVariableManager().loadVariable(symbol);
            context.getStackTracker().push(context.getTypeMapper().getJasminType(symbol.getType()));
        } else {
            throw new CodeGenException("Identifier is not a variable: " + id.getName());
        }
    }

    private void generateBinaryExpression(BinaryExpression bin) {
        generate(bin.getLeft());
        generate(bin.getRight());
        
        TokenType op = bin.getOperator();
        Type resolvedType = bin.resolvedType;
        String jasminType = context.getTypeMapper().getJasminType(resolvedType);
        
        switch (op) {
            case PLUS -> emitArithmetic("iadd", "dadd", jasminType);
            case MINUS -> emitArithmetic("isub", "dsub", jasminType);
            case MULTIPLY -> emitArithmetic("imul", "dmul", jasminType);
            case DIVIDE -> emitArithmetic("idiv", "ddiv", jasminType);
            case MODULO -> context.getWriter().emit("irem");
            case EQUAL -> { generateComparison(bin, "if_icmpeq", "if_acmpeq", "dcmpg"); return; }
            case NOT_EQUAL -> { generateComparison(bin, "if_icmpne", "if_acmpne", "dcmpl"); return; }
            case LESS -> { generateComparison(bin, "if_icmplt", "ignored", "dcmpg"); return; }
            case LESS_EQUAL -> { generateComparison(bin, "if_icmple", "ignored", "dcmpg"); return; }
            case GREATER -> { generateComparison(bin, "if_icmpgt", "ignored", "dcmpl"); return; }
            case GREATER_EQUAL -> { generateComparison(bin, "if_icmpge", "ignored", "dcmpl"); return; }
            case AND -> context.getWriter().emit("iand");
            case OR -> context.getWriter().emit("ior");
            case XOR -> context.getWriter().emit("ixor");
            default -> throw new CodeGenException("Unknown binary operator: " + op);
        }
        
        popOperandsAndPushResult(jasminType);
    }

    private void emitArithmetic(String intOp, String doubleOp, String jasminType) {
        context.getWriter().emit(jasminType.equals("D") ? doubleOp : intOp);
    }

    private void popOperandsAndPushResult(String jasminType) {
        context.getStackTracker().pop();
        context.getStackTracker().pop();
        context.getStackTracker().push(jasminType.equals("D") ? "D" : "I");
    }


    private void generateComparison(
            BinaryExpression bin,
            String intBranch,
            String refBranch,
            String dcmp) {

        String trueLabel = context.getLabelManager()
                .getNewLabel("cmp_true");
        String endLabel = context.getLabelManager()
                .getNewLabel("cmp_end");

        Type leftType = bin.getLeft().resolvedType;

        String leftDescriptor = context.getTypeMapper()
                .getJasminType(leftType);
        String rightDescriptor = context.getTypeMapper()
                .getJasminType(bin.getRight().resolvedType);

        boolean realComparison = "D".equals(leftDescriptor)
                || "D".equals(rightDescriptor);

        if (realComparison) {
            context.getWriter().emit(dcmp);

            String branch = switch (bin.getOperator()) {
                case EQUAL -> "ifeq";
                case NOT_EQUAL -> "ifne";
                case LESS -> "iflt";
                case LESS_EQUAL -> "ifle";
                case GREATER -> "ifgt";
                case GREATER_EQUAL -> "ifge";
                default -> throw new CodeGenException(
                        "Unsupported comparison operator: "
                                + bin.getOperator()
                );
            };

            context.getWriter().emit(branch + " " + trueLabel);

        } else if (leftType instanceof UserType) {
            context.getWriter().emit(refBranch + " " + trueLabel);

        } else {
            context.getWriter().emit(intBranch + " " + trueLabel);
        }

        context.getWriter().emit("iconst_0");
        context.getWriter().emit("goto " + endLabel);

        context.getWriter().emitLabel(trueLabel);
        context.getWriter().emit("iconst_1");

        context.getWriter().emitLabel(endLabel);

        popOperandsAndPushResult("I");
    }


    private void generateUnaryExpression(UnaryExpression un) {
        generate(un.getOperand());
        TokenType op = un.getOperator();
        String jasminType = context.getTypeMapper().getJasminType(un.resolvedType);
        
        if (op == TokenType.MINUS) {
            if (jasminType.equals("D")) {
                // 0 - x
                context.getWriter().emit("dconst_0");
                context.getWriter().emit("dsub");
            } else {
                context.getWriter().emit("ineg");
            }
        } else if (op == TokenType.PLUS) {
            // No operation needed
        } else if (op == TokenType.NOT) {
            // x XOR 1
            context.getWriter().emit("iconst_1");
            context.getWriter().emit("ixor");
        }
    }

    private void generateArrayAccess(ArrayAccess array) {
        generate(array.getArray());
        generate(array.getIndex());

        context.getWriter().emit("iconst_1");
        context.getStackTracker().push("I");

        context.getWriter().emit("isub");

        context.getStackTracker().pop();
        context.getStackTracker().pop();
        context.getStackTracker().push("I");
        
        String jasminElemType = context.getTypeMapper().getJasminType(array.resolvedType);
        if (jasminElemType.equals("I")) {
            context.getWriter().emit("iaload");
        } else if (jasminElemType.equals("D")) {
            context.getWriter().emit("daload");
        } else {
            context.getWriter().emit("aaload");
        }
        
        context.getStackTracker().pop(); // index
        context.getStackTracker().pop(); // array ref
        context.getStackTracker().push(jasminElemType);
    }

    private void generateFieldAccess(FieldAccess field) {
        generate(field.getTarget());
        
        String ownerClass = context.getTypeMapper().getRecordClassName(field.getTarget().resolvedType);
        String fieldName = field.getFieldName();
        String fieldType = context.getTypeMapper().getJasminType(field.resolvedType);
        
        // Clean 'L' and ';' for Jasmin getfield syntax
        String cleanOwner = ownerClass.startsWith("L") ? ownerClass.substring(1, ownerClass.length() - 1) : ownerClass;
        
        context.getWriter().emit("getfield " + cleanOwner + "/" + fieldName + " " + fieldType);
        context.getStackTracker().pop(); // target ref
        context.getStackTracker().push(fieldType);
    }

    private void generateRoutineCall(RoutineCallExpression call) {
        // Arguments are generated and automatically converted if needed by the semantic analyzer's annotations
        for (Expression arg : call.getArguments()) {
            generate(arg);
        }
        
        Symbol symbol = call.resolvedSymbol;
        if (symbol == null || !(symbol.getDeclaration() instanceof RoutineDeclaration routine)) {
            throw new CodeGenException("Unresolved or invalid routine call: " + call.getName());
        }
        
        String descriptor = context.getTypeMapper().getJasminMethodDescriptor(routine);

        String className = CodeGenerator.OUTPUT_CLASS;
        
        context.getWriter().emit("invokestatic " + className + "/" + call.getName() + descriptor);
        
        for (int i = 0; i < call.getArguments().size(); i++) {
            context.getStackTracker().pop();
        }
        
        if (routine.returnType != null) {
            context.getStackTracker().push(context.getTypeMapper().getJasminType(routine.returnType));
        }
    }
}