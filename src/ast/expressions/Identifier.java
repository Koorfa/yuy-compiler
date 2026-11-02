package ast.expressions;

import semantic.Symbol;

public class Identifier extends ModifiablePrimary {

    private final String name;

    public Symbol resolvedSymbol;

    public String getName() { return name; }

    public Identifier(String name) {
        this.name = name;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "Identifier: " + name);
    }
}