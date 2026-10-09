public class Identifier extends ModifiablePrimary {

    private final String name;

    public Identifier(String name) {
        this.name = name;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "Identifier: " + name);
    }
}