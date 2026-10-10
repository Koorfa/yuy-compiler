package ast;

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
    public void print(String indent) {
        System.out.println(indent + "VariableDeclaration: " + name);
        if (type != null) {
            System.out.println(indent + "  Type:");
            type.print(indent + "    ");
        }
        if (initializer != null) {
            System.out.println(indent + "  Initializer:");
            initializer.print(indent + "    ");
        }
    }
}