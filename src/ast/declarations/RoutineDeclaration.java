package ast.declarations;

import java.util.List;

import ast.Body;
import ast.types.Type;

public class RoutineDeclaration extends Declaration {
    public String name;
    public List<ParameterDeclaration> parameters;
    public Type returnType;       // null для процедур
    public Body body;

    public RoutineDeclaration(String name, List<ParameterDeclaration> parameters, Type returnType, Body body) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
    }

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
