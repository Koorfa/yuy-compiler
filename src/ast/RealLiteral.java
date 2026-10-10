package ast;

public class RealLiteral extends Primary {

    private final double value;

    public RealLiteral(double value) {
        this.value = value;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "RealLiteral: " + value);
    }
}