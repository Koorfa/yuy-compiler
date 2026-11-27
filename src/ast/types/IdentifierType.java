package ast.types;

/**
 * A type reference expressed as an identifier, such as a named type alias.
 */
public class IdentifierType extends Type {
    /** The referenced type identifier. */
    public String name;

    /**
     * Creates a reference to a named type.
     *
     * @param name the referenced type identifier
     */
    public IdentifierType(String name) {
        this.name = name;
    }

    /**
     * Returns the referenced type identifier.
     *
     * @return the identifier
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Prints this type reference.
     *
     * @param indent indentation to prepend to the printed line
     */
    @Override
    public void print(String indent) {
        System.out.println(indent + "IdentifierType: " + name);
    }
}