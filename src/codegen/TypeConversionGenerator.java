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
                // Round to the nearest integer instead of truncating.
                context.getWriter().emit(
                        "invokestatic java/lang/Math/round(D)J"
                );
                context.getWriter().emit("l2i");

                updateStack("D", "I");
                break;
                
            case BOOL_TO_INT:
                // Boolean is internally represented as 'I' (0 or 1), so no JVM instruction is needed.
                break;
                
            case INT_TO_BOOL:
                // Spec: "if integer is 1, converted to true; if 0, to false; otherwise erroneous"
                String endLabel = context.getLabelManager().getNewLabel("int_to_bool_end");
                StackTracker tracker = context.getStackTracker();
                StackTracker.StackState valueState = tracker.snapshot();
                
                context.getWriter().emit("dup");
                tracker.push("I");
                context.getWriter().emit("iconst_1");
                tracker.push("I");
                context.getWriter().emit("if_icmpeq " + endLabel);
                tracker.pop();
                tracker.pop();
                
                context.getWriter().emit("dup");
                tracker.push("I");
                context.getWriter().emit("iconst_0");
                tracker.push("I");
                context.getWriter().emit("if_icmpeq " + endLabel);
                tracker.pop();
                tracker.pop();
                
                context.getWriter().emit("new java/lang/IllegalArgumentException");
                tracker.push("A");
                context.getWriter().emit("dup");
                tracker.push("A");
                context.getWriter().emit("ldc \"Invalid integer to boolean conversion: value must be 0 or 1\"");
                tracker.push("A");
                context.getWriter().emit("invokespecial java/lang/IllegalArgumentException/<init>(Ljava/lang/String;)V");
                tracker.pop();
                tracker.pop();
                context.getWriter().emit("athrow");
                tracker.pop();
                
                tracker.restore(valueState);
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