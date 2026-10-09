
public class IdentifierType extends Type {
    public String name;

    public IdentifierType(String name) { this.name = name; }

    @Override
    public void printTree(int indent) {
        printIndent(indent);
        System.out.println("IdentifierType: " + name);
    }
}