package ast.types;

import java.util.List;

import ast.declarations.VariableDeclaration;

/**
 * A record type composed of named variable members.
 */
public class RecordType extends UserType {
    /** The record's members, in declaration order. */
    public List<VariableDeclaration> members;

    /**
     * Creates a record type.
     *
     * @param members the record's member declarations
     */
    public RecordType(List<VariableDeclaration> members) {
        this.members = members;
    }

    /**
     * Returns the source-level name of this type category.
     *
     * @return {@code "record"}
     */
    @Override
    public String toString() {
        return "record";
    }

    /**
     * Prints this record and its members.
     *
     * @param indent indentation to prepend to each printed line
     */
    @Override
    public void print(String indent) {
        System.out.println(indent + "RecordType");
        for (VariableDeclaration member : members) {
            member.print(indent + "  ");
        }
    }
}