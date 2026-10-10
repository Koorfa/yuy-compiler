public class ParameterDeclaration extends Declaration {
    public String name;
    public Type type;

    public ParameterDeclaration(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "ParameterDeclaration: " + name);
        System.out.println(indent + "  Type:");
        type.print(indent + "    ");
    }
}