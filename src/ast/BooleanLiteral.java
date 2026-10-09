public class BooleanLiteral extends Primary {

    private final boolean value;

    public BooleanLiteral(boolean value) {
        this.value = value;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "BooleanLiteral: " + value);
    }
}