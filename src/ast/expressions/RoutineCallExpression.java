package ast.expressions;

import java.util.List;
import semantic.Symbol;

public class RoutineCallExpression extends Primary {

    private final String name;
    private final List<Expression> arguments;

    public Symbol resolvedSymbol;

    public String getName() {return name;}
    public List<Expression> getArguments() {return arguments;} 
    
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