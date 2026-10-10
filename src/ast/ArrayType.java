package ast;

public class ArrayType extends UserType {
    public Expression size; // Может быть null, если размер не указан (sizeless)
    public Type elementType;

    public ArrayType(Expression size, Type elementType) {
        this.size = size;
        this.elementType = elementType;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "ArrayType");
        if (size != null) {
            System.out.println(indent + "  Size:");
            size.print(indent + "    ");
        }
        System.out.println(indent + "  Element Type:");
        elementType.print(indent + "    ");
    }
}