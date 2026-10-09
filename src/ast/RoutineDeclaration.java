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
    public void printTree(int indent) {
        printIndent(indent);
        System.out.println("RoutineDeclaration: " + name);
        if (returnType != null) {
            printIndent(indent + 1);
            System.out.println("Return Type:");
            returnType.printTree(indent + 2);
        }
        for (ParameterDeclaration p : parameters) {
            p.printTree(indent + 1);
        }
        if (body != null) {
            printIndent(indent + 1);
            System.out.println("Body:");
            body.printTree(indent + 2);
        }
        if (expressionBody != null) {
            printIndent(indent + 1);
            System.out.println("Expression Body:");
            expressionBody.printTree(indent + 2);
        }
    }
}
