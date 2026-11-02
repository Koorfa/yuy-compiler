package optimizer;

import ast.expressions.BinaryExpression;
import ast.expressions.Expression;
import ast.expressions.IntegerLiteral;
import lexer.TokenType;

import ast.expressions.BooleanLiteral;
import ast.expressions.RealLiteral;
import ast.expressions.UnaryExpression;
import ast.statements.Statement;
import ast.statements.Assignment;
import ast.statements.ForLoop;
import ast.statements.IfStatement;
import ast.statements.PrintStatement;
import ast.statements.ReturnStatement;
import ast.statements.WhileLoop;
import ast.ASTNode;
import ast.Body;
import ast.Program;
import ast.declarations.Declaration;
import ast.declarations.VariableDeclaration;
import ast.declarations.RoutineDeclaration;

public class ConstantExpressionOptimizer {

    public void optimize(Program program) {
        for (Declaration declaration : program.declarations) {

            if (declaration instanceof VariableDeclaration variable
                    && variable.initializer != null) {

                variable.initializer =
                        optimizeExpression(variable.initializer);
            } else if (declaration instanceof RoutineDeclaration routine) {
                if (routine.body != null) {
                    optimizeBody(routine.body);
                }
            }
        }
    }

    public void optimizeBody(Body body) {
        if (body == null || body.nodes == null) return;

        for (int i = 0; i < body.nodes.size(); i++) {
            ASTNode node = body.nodes.get(i);
            if (node instanceof VariableDeclaration var && var.initializer != null) {
                var.initializer = optimizeExpression(var.initializer);
            } else if (node instanceof Statement stmt) {
                optimizeStatement(stmt);
            }
        }
    }

    private void optimizeStatement(Statement stmt) {
        switch (stmt) {
            case Assignment assign -> assign.value = optimizeExpression(assign.value);
            case IfStatement ifStmt -> {
                ifStmt.condition = optimizeExpression(ifStmt.condition);
                optimizeBody(ifStmt.thenBody);
                if (ifStmt.elseBody != null) {
                    optimizeBody(ifStmt.elseBody);
                }
            }
            case WhileLoop whileLoop -> {
                whileLoop.condition = optimizeExpression(whileLoop.condition);
                optimizeBody(whileLoop.body);
            }
            case ForLoop forLoop -> {
                forLoop.start = optimizeExpression(forLoop.start);
                if (forLoop.end != null) {
                    forLoop.end = optimizeExpression(forLoop.end);
                }
                optimizeBody(forLoop.body);
            }
            case PrintStatement printStmt -> {
                for (int i = 0; i < printStmt.expressions.size(); i++) {
                    printStmt.expressions.set(i, optimizeExpression(printStmt.expressions.get(i)));
                }
            }
            case ReturnStatement returnStmt -> {
                returnStmt.value = optimizeExpression(returnStmt.value);
            }
            default -> {}
        }
    }

    public Expression optimizeExpression(Expression expression) {

        if (expression instanceof UnaryExpression unary) {

            Expression operand = optimizeExpression(unary.getOperand());

            if (operand instanceof IntegerLiteral intLiteral) {
                if (unary.getOperator() == TokenType.MINUS) {
                    return new IntegerLiteral(-intLiteral.getValue());
                }

                if (unary.getOperator() == TokenType.PLUS) {
                    return new IntegerLiteral(intLiteral.getValue());
                }
            }

            if (operand instanceof RealLiteral realLiteral) {
                if (unary.getOperator() == TokenType.MINUS) {
                    return new RealLiteral(-realLiteral.getValue());
                }

                if (unary.getOperator() == TokenType.PLUS) {
                    return new RealLiteral(realLiteral.getValue());
                }
            }

            if (operand instanceof BooleanLiteral bool
                    && unary.getOperator() == TokenType.NOT) {

                return new BooleanLiteral(!bool.getValue());
            }

            return new UnaryExpression(
                    unary.getOperator(),
                    operand
            );
        }

        if (expression instanceof BinaryExpression binary) {

            Expression left = optimizeExpression(binary.getLeft());
            Expression right = optimizeExpression(binary.getRight());

            if (left instanceof IntegerLiteral leftInt
                    && right instanceof IntegerLiteral rightInt) {

                int a = leftInt.getValue();
                int b = rightInt.getValue();

                if (binary.getOperator() == TokenType.PLUS) {
                    return new IntegerLiteral(a + b);
                }

                if (binary.getOperator() == TokenType.MINUS) {
                    return new IntegerLiteral(a - b);
                }

                if (binary.getOperator() == TokenType.DIVIDE) {
                    if (b == 0) {
                        return new BinaryExpression(
                                left,
                                binary.getOperator(),
                                right
                        );
                    }

                    return new IntegerLiteral(a / b);
                }

                if (binary.getOperator() == TokenType.MODULO) {
                    if (b == 0) {
                        return new BinaryExpression(
                                left,
                                binary.getOperator(),
                                right
                        );
                    }

                    return new IntegerLiteral(a % b);
                }

                if (binary.getOperator() == TokenType.MULTIPLY) {
                    return new IntegerLiteral(a * b);
                }


                if (binary.getOperator() == TokenType.LESS) {
                    return new BooleanLiteral(a < b);
                }

                if (binary.getOperator() == TokenType.LESS_EQUAL) {
                    return new BooleanLiteral(a <= b);
                }

                if (binary.getOperator() == TokenType.EQUAL) {
                    return new BooleanLiteral(a == b);
                }

                if (binary.getOperator() == TokenType.GREATER) {
                    return new BooleanLiteral(a > b);
                }

                if (binary.getOperator() == TokenType.GREATER_EQUAL) {
                    return new BooleanLiteral(a >= b);
                }

                if (binary.getOperator() == TokenType.NOT_EQUAL) {
                    return new BooleanLiteral(a != b);
                }
            }

            if (left instanceof RealLiteral leftReal
                    && right instanceof RealLiteral rightReal) {

                double a = leftReal.getValue();
                double b = rightReal.getValue();

                if (binary.getOperator() == TokenType.PLUS) {
                    return new RealLiteral(a + b);
                }

                if (binary.getOperator() == TokenType.MINUS) {
                    return new RealLiteral(a - b);
                }

                if (binary.getOperator() == TokenType.MULTIPLY) {
                    return new RealLiteral(a * b);
                }

                if (binary.getOperator() == TokenType.DIVIDE && b != 0) {
                    return new RealLiteral(a / b);
                }

                if (binary.getOperator() == TokenType.LESS) {
                    return new BooleanLiteral(a < b);
                }

                if (binary.getOperator() == TokenType.LESS_EQUAL) {
                    return new BooleanLiteral(a <= b);
                }

                if (binary.getOperator() == TokenType.GREATER) {
                    return new BooleanLiteral(a > b);
                }

                if (binary.getOperator() == TokenType.GREATER_EQUAL) {
                    return new BooleanLiteral(a >= b);
                }

                if (binary.getOperator() == TokenType.EQUAL) {
                    return new BooleanLiteral(a == b);
                }

                if (binary.getOperator() == TokenType.NOT_EQUAL) {
                    return new BooleanLiteral(a != b);
                }

            }

            if (left instanceof BooleanLiteral leftBool
                    && right instanceof BooleanLiteral rightBool) {

                boolean a = leftBool.getValue();
                boolean b = rightBool.getValue();

                if (binary.getOperator() == TokenType.AND) {
                    return new BooleanLiteral(a && b);
                }

                if (binary.getOperator() == TokenType.OR) {
                    return new BooleanLiteral(a || b);
                }

                if (binary.getOperator() == TokenType.XOR) {
                    return new BooleanLiteral(a ^ b);
                }

                if (binary.getOperator() == TokenType.EQUAL) {
                    return new BooleanLiteral(a == b);
                }

                if (binary.getOperator() == TokenType.NOT_EQUAL) {
                    return new BooleanLiteral(a != b);
                }
            }

            return new BinaryExpression(
                    left,
                    binary.getOperator(),
                    right
            );
        }

        return expression;
    }
}