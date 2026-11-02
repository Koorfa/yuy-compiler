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
        String symInfo = (resolvedSymbol != null) ? " [symbol=" + resolvedSymbol.getName() + "]" : "";
        System.out.println(indent + "Identifier: " + name + annotations() + symInfo);
    }
}