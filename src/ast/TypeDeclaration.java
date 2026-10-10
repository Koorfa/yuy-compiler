public class TypeDeclaration extends SimpleDeclaration {
    public String name;
    public Type type;

    public TypeDeclaration(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "TypeDeclaration: " + name);
        System.out.println(indent + "  Type:");
        type.print(indent + "    ");
    }
}