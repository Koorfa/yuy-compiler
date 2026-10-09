public class ArrayAccess extends ModifiablePrimary {

    private final ModifiablePrimary array;
    private final Expression index;

    public ArrayAccess(ModifiablePrimary array, Expression index) {
        this.array = array;
        this.index = index;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "ArrayAccess");
        array.print(indent + "    ");
        index.print(indent + "    ");
    }
}