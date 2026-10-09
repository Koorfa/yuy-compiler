
public class PrimitiveType extends Type {
    public String typeName; // "integer", "real", "boolean"

    public PrimitiveType(String typeName) { this.typeName = typeName; }

    @Override
    public void printTree(int indent) {
        printIndent(indent);
        System.out.println("PrimitiveType: " + typeName);
    }
}