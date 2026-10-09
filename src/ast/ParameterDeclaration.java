public class ParameterDeclaration extends Declaration {
    public String name;
    public Type type;

    public ParameterDeclaration(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public void printTree(int indent) {
        printIndent(indent);
        System.out.println("ParameterDeclaration: " + name);
        printIndent(indent + 1);
        System.out.println("Type:");
        type.printTree(indent + 2);
    }
}