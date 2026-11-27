package ast.declarations;

import ast.types.Type;

/**
 * A named, typed parameter in a routine declaration.
 */
public class ParameterDeclaration extends Declaration {
    /** The parameter's identifier. */
    public String name;
    /** The declared type of the parameter. */
    public Type type;

    /**
     * Creates a parameter declaration.
     *
     * @param name the parameter's identifier
     * @param type the parameter's declared type
     */
    public ParameterDeclaration(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Prints this parameter and its type using the given indentation.
     *
     * @param indent indentation to prepend to each printed line
     */
    @Override
    public void print(String indent) {
        System.out.println(indent + "ParameterDeclaration: " + name);
        System.out.println(indent + "  Type:");
        type.print(indent + "    ");
    }
}