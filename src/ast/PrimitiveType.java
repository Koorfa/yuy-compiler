
public class PrimitiveType extends Type {
    public String typeName; // "integer", "real", "boolean"

    public PrimitiveType(String typeName) { this.typeName = typeName; }

    @Override
    public void print(String indent) {
        System.out.println(indent + "PrimitiveType: " + typeName);
    }
}