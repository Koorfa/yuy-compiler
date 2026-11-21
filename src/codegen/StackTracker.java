package codegen;

import java.util.Stack;

public class StackTracker {
    private final Stack<String> stack = new Stack<>();

    public void push(String type) { stack.push(type); }
    
    public String pop() { 
        if (stack.isEmpty()) throw new CodeGenException("Stack underflow in StackTracker");
        return stack.pop(); 
    }
    
    public int size() { return stack.size(); }
    public void clear() { stack.clear(); }
    
    // TODO: Add methods to peek at the top of the stack if needed for 
    // complex expression generation or optimization passes.
}