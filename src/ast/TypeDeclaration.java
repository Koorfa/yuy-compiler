public class TypeDeclaration extends SimpleDeclaration {
    public String name;
    public Type type;

    public TypeDeclaration(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public void printTree(int indent) {
        printIndent(indent);
        System.out.println("TypeDeclaration: " + name);
        printIndent(indent + 1);
        System.out.println("Type:");
        type.printTree(indent + 2);
    }
}