package codegen;

import java.util.List;
import java.util.Stack;

public class StackTracker {
    private final Stack<String> stack = new Stack<>();
    private int currentDepth;
    private int maxDepth;

    public static final class StackState {
        private final List<String> types;

        private StackState(List<String> types) {
            this.types = List.copyOf(types);
        }
    }

    public void reset() {
        stack.clear();
        currentDepth = 0;
        maxDepth = 0;
    }

    public void push(String type) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Stack type cannot be empty");
        }
        stack.push(type);
        adjust(wordSize(type));
    }

    public String pop() {
        if (stack.isEmpty()) {
            throw new CodeGenException("Stack underflow in StackTracker");
        }
        String type = stack.pop();
        adjust(-wordSize(type));
        return type;
    }

    public String peek() {
        if (stack.isEmpty()) {
            throw new CodeGenException("Cannot peek an empty operand stack");
        }
        return stack.peek();
    }

    public void adjust(int wordDelta) {
        int nextDepth = currentDepth + wordDelta;
        if (nextDepth < 0) {
            throw new CodeGenException("Operand stack underflow: depth became " + nextDepth);
        }
        currentDepth = nextDepth;
        maxDepth = Math.max(maxDepth, currentDepth);
    }

    public StackState snapshot() {
        return new StackState(stack);
    }

    public void restore(StackState state) {
        if (state == null) {
            throw new IllegalArgumentException("Stack state cannot be null");
        }

        stack.clear();
        stack.addAll(state.types);
        currentDepth = 0;
        for (String type : stack) {
            currentDepth += wordSize(type);
        }
    }

    public void assertCompatible(
            StackState expected,
            String location) {

        if (expected == null) {
            throw new IllegalArgumentException("Expected stack state cannot be null");
        }

        if (!stack.equals(expected.types)) {
            throw new CodeGenException("Incompatible operand stack at " + location + ": expected " + expected.types + ", got " + stack);
        }
    }

    public void assertDepth(int expected, String location) {
        if (currentDepth != expected) {
            throw new CodeGenException("Inconsistent stack depth at " + location + ": expected " + expected + ", got " + currentDepth);
        }
    }

    private int wordSize(String type) {
        return ("D".equals(type) || "J".equals(type)) ? 2 : 1;
    }

    public int size() {
        return stack.size();
    }

    public int getDepth() {
        return currentDepth;
    }

    public int getMaxDepth() {
        return maxDepth;
    }
}