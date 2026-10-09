public class ArrayType extends UserType {
    public Expression size; // Может быть null, если размер не указан (sizeless)
    public Type elementType;

    public ArrayType(Expression size, Type elementType) {
        this.size = size;
        this.elementType = elementType;
    }

    @Override
    public void printTree(int indent) {
        printIndent(indent);
        System.out.println("ArrayType");
        if (size != null) {
            printIndent(indent + 1);
            System.out.println("Size:");
            size.printTree(indent + 2);
        }
        printIndent(indent + 1);
        System.out.println("Element Type:");
        elementType.printTree(indent + 2);
    }
}