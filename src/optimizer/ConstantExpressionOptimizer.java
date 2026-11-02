package optimizer;

import ast.expressions.BinaryExpression;
import ast.expressions.Expression;
import ast.expressions.IntegerLiteral;
import lexer.TokenType;

import ast.expressions.BooleanLiteral;
import ast.expressions.RealLiteral;
import ast.expressions.UnaryExpression;

import ast.Program;
import ast.declarations.Declaration;
import ast.declarations.VariableDeclaration;

public class ConstantExpressionOptimizer {

    public void optimize(Program program) {
        for (Declaration declaration : program.declarations) {

            if (declaration instanceof VariableDeclaration variable
                    && variable.initializer != null) {

                variable.initializer =
                        optimizeExpression(variable.initializer);
            }
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