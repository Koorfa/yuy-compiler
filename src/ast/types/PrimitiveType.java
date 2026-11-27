package ast.types;

/**
 * A built-in scalar type: {@code integer}, {@code real}, or {@code boolean}.
 */
public class PrimitiveType extends Type {
    /** The source-level name of this primitive type. */
    public String typeName;

    /** The built-in integer type. */
    public static final PrimitiveType INTEGER = new PrimitiveType("integer");
    /** The built-in real-number type. */
    public static final PrimitiveType REAL = new PrimitiveType("real");
    /** The built-in boolean type. */
    public static final PrimitiveType BOOLEAN = new PrimitiveType("boolean");

    /**
     * Creates a primitive type with the specified source-level name.
     *
     * @param typeName the primitive type name
     */
    public PrimitiveType(String typeName) { this.typeName = typeName; }

    /**
     * Returns this type's source-level name.
     *
     * @return the primitive type name
     */
    @Override
    public String toString() {
        return typeName;
    }

    /**
     * Prints this primitive type.
     *
     * @param indent indentation to prepend to the printed line
     */
    @Override
    public void print(String indent) {
        System.out.println(indent + "PrimitiveType: " + typeName);
    }
}