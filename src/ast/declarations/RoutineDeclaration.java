package ast.declarations;

import java.util.List;

import ast.Body;
import ast.types.Type;

/**
 * A routine declaration, including its signature and optional body.
 */
public class RoutineDeclaration extends Declaration {
    /** The routine's identifier. */
    public String name;
    /** The routine's parameters, in declaration order. */
    public List<ParameterDeclaration> parameters;
    /** The return type, or {@code null} when the routine is a procedure. */
    public Type returnType;
    /** The routine body, or {@code null} for a declaration without a body. */
    public Body body;

    /**
     * Creates a routine declaration.
     *
     * @param name the routine's identifier
     * @param parameters the routine's parameters
     * @param returnType the return type, or {@code null} for a procedure
     * @param body the routine body, or {@code null} when not defined here
     */
    public RoutineDeclaration(String name, List<ParameterDeclaration> parameters, Type returnType, Body body) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
    }

    /**
     * Prints this routine's signature and, when present, its body.
     *
     * @param indent indentation to prepend to each printed line
     */
    @Override
    public void print(String indent) {
        System.out.println(indent + "RoutineDeclaration: " + name);
        if (returnType != null) {
            System.out.println(indent + "  Return Type:");
            returnType.print(indent + "    ");
        }
        for (ParameterDeclaration p : parameters) {
            p.print(indent + "  ");
        }
        if (body != null) {
            System.out.println(indent + "  Body:");
            body.print(indent + "    ");
        }
    }
}
