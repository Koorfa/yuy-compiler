package ast;


public class IdentifierType extends Type {
    public String name;

    public IdentifierType(String name) { this.name = name; }

    @Override
    public void print(String indent) {
        System.out.println(indent + "IdentifierType: " + name);
    }
}