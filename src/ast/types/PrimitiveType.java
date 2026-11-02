package ast.types;

public class PrimitiveType extends Type {
    public String typeName; // "integer", "real", "boolean"

    public static final PrimitiveType INTEGER = new PrimitiveType("integer");
    public static final PrimitiveType REAL = new PrimitiveType("real");
    public static final PrimitiveType BOOLEAN = new PrimitiveType("boolean");

    public PrimitiveType(String typeName) { this.typeName = typeName; }

    @Override
    public void print(String indent) {
        System.out.println(indent + "PrimitiveType: " + typeName);
    }
}