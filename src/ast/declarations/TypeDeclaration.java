package ast.declarations;

import ast.types.Type;

/**
 * A named type declaration that associates an identifier with a type.
 */
public class TypeDeclaration extends SimpleDeclaration {
    /** The identifier introduced by this declaration. */
    public String name;
    /** The type associated with the identifier. */
    public Type type;

    /**
     * Creates a type declaration.
     *
     * @param name the identifier being declared
     * @param type the type associated with the identifier
     */
    public TypeDeclaration(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Prints this type declaration and its underlying type.
     *
     * @param indent indentation to prepend to each printed line
     */
    @Override
    public void print(String indent) {
        System.out.println(indent + "TypeDeclaration: " + name);
        System.out.println(indent + "  Type:");
        type.print(indent + "    ");
    }
}