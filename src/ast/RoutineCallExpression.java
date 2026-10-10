package ast;

import java.util.List;

public class RoutineCallExpression extends Primary {

    private final String name;
    private final List<Expression> arguments;

    public RoutineCallExpression(String name, List<Expression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "RoutineCallExpression: " + name);

        for (Expression argument : arguments) {
            argument.print(indent + "    ");
        }
    }
}