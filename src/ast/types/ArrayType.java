package ast.types;

import ast.expressions.Expression;

/**
 * An array type with an element type and an optional size expression.
 */
public class ArrayType extends UserType {
    /** The size expression, or {@code null} when the array has no declared size. */
    public Expression size;
    /** The type of each array element. */
    public Type elementType;
    /** The size resolved during semantic analysis, or {@code null} before resolution. */
    public Integer resolvedSize;

    /**
     * Creates an array type.
     *
     * @param size the size expression, or {@code null} for an unsized array
     * @param elementType the type of the array elements
     */
    public ArrayType(Expression size, Type elementType) {
        this.size = size;
        this.elementType = elementType;
    }

    /**
     * Returns the source-level name of this type category.
     *
     * @return {@code "array"}
     */
    @Override
    public String toString() {
        return "array";
    }

    /**
     * Prints this array type, including its optional size and element type.
     *
     * @param indent indentation to prepend to each printed line
     */
    @Override
    public void print(String indent) {
        System.out.println(indent + "ArrayType");
        if (size != null) {
            System.out.println(indent + "  Size:");
            size.print(indent + "    ");
        }
        System.out.println(indent + "  Element Type:");
        elementType.print(indent + "    ");
    }
}