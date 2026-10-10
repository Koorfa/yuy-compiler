package ast;

import java.util.List;

public class RoutineDeclaration extends Declaration {
    public String name;
    public List<ParameterDeclaration> parameters;
    public Type returnType;       // null для процедур
    public Body body;             // для тела "is ... end"
    public Expression expressionBody; // для тела "=> Expression"

    public RoutineDeclaration(String name, List<ParameterDeclaration> parameters, Type returnType, Body body, Expression expressionBody) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
        this.expressionBody = expressionBody;
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
        if (expressionBody != null) {
            System.out.println(indent + "  Expression Body:");
            expressionBody.print(indent + "    ");
        }
    }
}
