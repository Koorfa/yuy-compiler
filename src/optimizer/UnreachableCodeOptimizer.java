package optimizer;

import ast.*;
import ast.declarations.*;
import ast.statements.*;

public class UnreachableCodeOptimizer {
    public void optimize(Program program) {
        for (Declaration decl : program.declarations) {
            if (decl instanceof RoutineDeclaration routine) {
                optimizeRoutine(routine);
            }
        }
    }
    
    private void optimizeRoutine(RoutineDeclaration routine) {
        if (routine.body != null) {
            optimizeBody(routine.body);
        }
    }

    private void optimizeBody(Body body) {
        if (body == null || body.nodes == null) return;

        for (ASTNode node : body.nodes) {
            if (node instanceof IfStatement ifStmt) {
                optimizeBody(ifStmt.thenBody);
                if (ifStmt.elseBody != null) {
                    optimizeBody(ifStmt.elseBody);
                }
            } else if (node instanceof WhileLoop whileLoop) {
                optimizeBody(whileLoop.body);
            } else if (node instanceof ForLoop forLoop) {
                optimizeBody(forLoop.body);
            }
        }

        boolean foundReturn = false;
        int cutIndex = -1;
        for (int i = 0; i < body.nodes.size(); i++) {
            ASTNode node = body.nodes.get(i);
            if (foundReturn) {
                cutIndex = i;
                break;
            }

            if (node instanceof ReturnStatement) {
                foundReturn = true;
            }
        }

        if (foundReturn && cutIndex != -1) {
            while (body.nodes.size() > cutIndex) {
                body.nodes.remove(body.nodes.size() - 1);
            }
        }
    }
}
