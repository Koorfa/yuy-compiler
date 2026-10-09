public class VariableDeclaration extends SimpleDeclaration {
    public String name;
    public Type type;         // Может быть null, если тип выводится из инициализатора
    public Expression initializer; // Может быть null

    public VariableDeclaration(String name, Type type, Expression initializer) {
        this.name = name;
        this.type = type;
        this.initializer = initializer;
    }

    @Override
    public void printTree(int indent) {
        printIndent(indent);
        System.out.println("VariableDeclaration: " + name);
        if (type != null) {
            printIndent(indent + 1);
            System.out.println("Type:");
            type.printTree(indent + 2);
        }
        if (initializer != null) {
            printIndent(indent + 1);
            System.out.println("Initializer:");
            initializer.printTree(indent + 2);
        }
    }
}