public class IntegerLiteral extends Primary {

    private final int value;

    public IntegerLiteral(int value) {
        this.value = value;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "IntegerLiteral: " + value);
    }
}