package ast.declarations;

import ast.expressions.Expression;
import ast.types.Type;

/**
 * A variable declaration with an optional explicit type and initializer.
 */
public class VariableDeclaration extends SimpleDeclaration {
    /** The variable's identifier. */
    public String name;
    /** The explicit type, or {@code null} when it is inferred from the initializer. */
    public Type type;
    /** The initializer expression, or {@code null} when none is provided. */
    public Expression initializer;
    /** The lexical scope level assigned to this declaration. */
    public int scopeLevel;

    /**
     * Creates a variable declaration.
     *
     * @param name the variable's identifier
     * @param type the explicit type, or {@code null} when it is inferred
     * @param initializer the initializer, or {@code null} when absent
     */
    public VariableDeclaration(String name, Type type, Expression initializer) {
        this.name = name;
        this.type = type;
        this.initializer = initializer;
    }

    /**
     * Prints this variable declaration and its optional type and initializer.
     *
     * @param indent indentation to prepend to each printed line
     */
    @Override
    public void print(String indent) {
        System.out.println(indent + "VariableDeclaration: " + name + " [scope=" + scopeLevel + "]");
        if (type != null) {
            System.out.println(indent + "  Type:");
            type.print(indent + "    ");
        }
        if (initializer != null) {
            System.out.println(indent + "  Initializer:");
            initializer.print(indent + "    ");
        }
    }
}