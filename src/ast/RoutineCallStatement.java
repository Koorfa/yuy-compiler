package ast;

import java.util.List;

public class RoutineCallStatement extends Statement {
    public String name;
    public List<Expression> arguments;
    
    public RoutineCallStatement(String name, List<Expression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override 
    public void print(String indent) {
        System.out.println(indent + "RoutineCallStatement: " + name);
        for (Expression arg : arguments) {
            arg.print(indent + "  ");
        }
    }
}
