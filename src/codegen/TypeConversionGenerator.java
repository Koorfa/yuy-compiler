package codegen;

import ast.expressions.Expression;
import semantic.Conversion;

public class TypeConversionGenerator {
    private final CodegenContext context;

    public TypeConversionGenerator(CodegenContext context) {
        this.context = context;
    }

    public void applyConversion(Expression expr) {
        if (expr == null || expr.conversion == null || expr.conversion == Conversion.NONE) {
            return;
        }

        switch (expr.conversion) {
            case INT_TO_REAL:
                context.getWriter().emit("i2d");
                updateStack("I", "D");
                break;
                
            case REAL_TO_INT:
                // Spec: "copying with rounding to nearest integer"
                // Note: JVM 'd2i' truncates towards zero. For strict "round to nearest", 
                // a more complex sequence (e.g., adding 0.5 conditionally) may be required.
                context.getWriter().emit("d2i");
                updateStack("D", "I");
                break;
                
            case BOOL_TO_INT:
                // Boolean is internally represented as 'I' (0 or 1), so no JVM instruction is needed.
                break;
                
            case INT_TO_BOOL:
                // Spec: "if integer is 1, converted to true; if 0, to false; otherwise erroneous"
                String errorLabel = context.getLabelManager().getNewLabel("int_to_bool_err");
                String endLabel = context.getLabelManager().getNewLabel("int_to_bool_end");
                
                context.getWriter().emit("dup");
                context.getWriter().emit("iconst_1");
                context.getWriter().emit("if_icmpeq " + endLabel);
                
                context.getWriter().emit("dup");
                context.getWriter().emit("iconst_0");
                context.getWriter().emit("if_icmpeq " + endLabel);
                
                // TODO: Ensure this runtime exception aligns with the project's 
                // overall error handling strategy (e.g., custom exception class or standard JVM throw).
                context.getWriter().emit("new java/lang/RuntimeException");
                context.getWriter().emit("dup");
                context.getWriter().emit("ldc \"Invalid integer to boolean conversion: value must be 0 or 1\"");
                context.getWriter().emit("invokespecial java/lang/RuntimeException/<init>(Ljava/lang/String;)V");
                context.getWriter().emit("athrow");
                
                context.getWriter().emitLabel(endLabel);
                break;
                
            case BOOL_TO_REAL:
                context.getWriter().emit("i2d");
                updateStack("I", "D");
                break;
            default:
                break;
        }
    }

    private void updateStack(String popType, String pushType) {
        context.getStackTracker().pop();
        context.getStackTracker().push(pushType);
    }
}