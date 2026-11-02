package semantic;

import ast.declarations.VariableDeclaration;
import ast.expressions.*;
import ast.types.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lexer.TokenType;

public class ExpressionAnalyzer {

    private final SymbolTable symbolTable;
    private final Set<Symbol> variablesBeingInferred = new HashSet<>();

    public ExpressionAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public Type analyze(Expression expr) {
        if (expr == null) return null;

        Type resultType = switch (expr) {
            case IntegerLiteral lit -> analyzeIntegerLiteral(lit);
            case RealLiteral lit -> analyzeRealLiteral(lit);
            case BooleanLiteral lit -> analyzeBooleanLiteral(lit);
            case Identifier id -> analyzeIdentifier(id);
            case BinaryExpression bin -> analyzeBinaryExpression(bin);
            case UnaryExpression un -> analyzeUnaryExpression(un);
            case FieldAccess field -> analyzeFieldAccess(field);
            case ArrayAccess array -> analyzeArrayAccess(array);
            case RoutineCallExpression call -> analyzeRoutineCall(call);
            default -> throw new SemanticException(
                "Unknown expression type: " + expr.getClass().getSimpleName()
            );
        };

        if (resultType != null) {
            resultType = TypeConformance.resolveType(resultType, symbolTable);
        }
        expr.resolvedType = resultType;
        return resultType;
    }

    private Type analyzeIntegerLiteral(IntegerLiteral literal) { return PrimitiveType.INTEGER; }
    private Type analyzeRealLiteral(RealLiteral literal) { return PrimitiveType.REAL; }
    private Type analyzeBooleanLiteral(BooleanLiteral literal) { return PrimitiveType.BOOLEAN; }

    private Type analyzeIdentifier(Identifier identifier) {
        Symbol symbol = symbolTable.lookup(identifier.getName());
        if (symbol == null) {
            throw new SemanticException("Undeclared identifier: " + identifier.getName());
        }
        if (symbol.getCategory() != SymbolCategory.VARIABLE) {
            throw new SemanticException(identifier.getName() + " is not a variable (it's a " + symbol.getCategory() + ")");
        }
        
        identifier.resolvedSymbol = symbol;
        return inferVariableType(symbol);
    }

    public Type inferVariableType(Symbol symbol) {
        if (symbol.getType() != null) {
            return TypeConformance.resolveType(symbol.getType(), symbolTable);
        }
        if (!(symbol.getDeclaration() instanceof VariableDeclaration variable)
                || variable.initializer == null) {
            throw new SemanticException(
                    "Cannot infer type for variable '" + symbol.getName() + "': missing initializer"
            );
        }
        if (!variablesBeingInferred.add(symbol)) {
            throw new SemanticException(
                    "Cyclic type inference involving variable '" + symbol.getName() + "'"
            );
        }

        try {
            Type inferredType = analyze(variable.initializer);
            if (inferredType == null) {
                throw new SemanticException(
                        "Cannot infer type for variable '" + symbol.getName() + "' from its initializer"
                );
            }
            symbol.setType(inferredType);
            variable.type = inferredType;
            return inferredType;
        } finally {
            variablesBeingInferred.remove(symbol);
        }
    }

    public int evaluateIntegerConstant(Expression expression) {
        if (expression instanceof IntegerLiteral literal) {
            return literal.getValue();
        }

        if (expression instanceof UnaryExpression unary
                && (unary.getOperator() == TokenType.PLUS || unary.getOperator() == TokenType.MINUS)) {
            int operand = evaluateIntegerConstant(unary.getOperand());
            try {
                return unary.getOperator() == TokenType.MINUS
                        ? Math.negateExact(operand)
                        : operand;
            } catch (ArithmeticException exception) {
                throw new SemanticException("Integer constant overflow in array size");
            }
        }

        if (expression instanceof BinaryExpression binary
                && isArithmeticOperator(binary.getOperator())) {
            int left = evaluateIntegerConstant(binary.getLeft());
            int right = evaluateIntegerConstant(binary.getRight());
            try {
                return switch (binary.getOperator()) {
                    case PLUS -> Math.addExact(left, right);
                    case MINUS -> Math.subtractExact(left, right);
                    case MULTIPLY -> Math.multiplyExact(left, right);
                    case DIVIDE -> {
                        if (right == 0) {
                            throw new SemanticException("Division by zero in array size constant");
                        }
                        if (left == Integer.MIN_VALUE && right == -1) {
                            throw new SemanticException("Integer constant overflow in array size");
                        }
                        yield left / right;
                    }
                    case MODULO -> {
                        if (right == 0) {
                            throw new SemanticException("Modulo by zero in array size constant");
                        }
                        yield left % right;
                    }
                    default -> throw new SemanticException(
                            "Unsupported operator in array size constant: " + binary.getOperator()
                    );
                };
            } catch (ArithmeticException exception) {
                throw new SemanticException("Integer constant overflow in array size");
            }
        }

        throw new SemanticException("Array size must be a compile-time integer constant");
    }

    private Type analyzeBinaryExpression(BinaryExpression binExpr) {
        Type leftType = analyze(binExpr.getLeft());
        Type rightType = analyze(binExpr.getRight());
        TokenType op = binExpr.getOperator();

        if (isComparisonOperator(op)) {
            if (!typesCompatible(leftType, rightType)) {
                throw new SemanticException("Cannot compare " + getTypeName(leftType) + " and " + getTypeName(rightType));
            }
            if (isNumericType(leftType) && isNumericType(rightType)) {
                Type comparisonType = isRealType(leftType) || isRealType(rightType)
                        ? PrimitiveType.REAL
                        : PrimitiveType.INTEGER;
                annotateConversion(binExpr.getLeft(), comparisonType, leftType);
                annotateConversion(binExpr.getRight(), comparisonType, rightType);
            }
            return PrimitiveType.BOOLEAN;
        }

        if (isLogicalOperator(op)) {
            if (!TypeConformance.conformsToBoolean(leftType, symbolTable)) {
                throw new SemanticException("Left operand of logical operator must be boolean-compatible, got " + getTypeName(leftType));
            }
            if (!TypeConformance.conformsToBoolean(rightType, symbolTable)) {
                throw new SemanticException("Right operand of logical operator must be boolean-compatible, got " + getTypeName(rightType));
            }
            annotateConversion(binExpr.getLeft(), PrimitiveType.BOOLEAN, leftType);
            annotateConversion(binExpr.getRight(), PrimitiveType.BOOLEAN, rightType);
            return PrimitiveType.BOOLEAN;
        }

        if (isArithmeticOperator(op)) {
            if (op == TokenType.MODULO) {
                if (!isIntegerType(leftType) || !isIntegerType(rightType)) {
                    throw new SemanticException("Modulo operator (%) requires integer operands");
                }
                return PrimitiveType.INTEGER;
            }
            if (!isNumericType(leftType) || !isNumericType(rightType)) {
                throw new SemanticException(
                        "Arithmetic operator " + op + " requires numeric operands, got "
                                + getTypeName(leftType) + " and " + getTypeName(rightType)
                );
            }
            Type resultType = isRealType(leftType) || isRealType(rightType)
                    ? PrimitiveType.REAL
                    : PrimitiveType.INTEGER;
            annotateConversion(binExpr.getLeft(), resultType, leftType);
            annotateConversion(binExpr.getRight(), resultType, rightType);
            return resultType;
        }

        throw new SemanticException("Unknown binary operator: " + op);
    }

    private Type analyzeUnaryExpression(UnaryExpression unExpr) {
        Type operandType = analyze(unExpr.getOperand());
        TokenType op = unExpr.getOperator();

        if (op == TokenType.NOT) {
            if (!TypeConformance.conformsToBoolean(operandType, symbolTable)) {
                throw new SemanticException("Operand of 'not' must be boolean-compatible, got " + getTypeName(operandType));
            }
            annotateConversion(unExpr.getOperand(), PrimitiveType.BOOLEAN, operandType);
            return PrimitiveType.BOOLEAN;
        }

        if (op == TokenType.PLUS || op == TokenType.MINUS) {
            if (!isNumericType(operandType)) {
                throw new SemanticException("Unary " + op + " requires numeric operand, got " + getTypeName(operandType));
            }
            return operandType;
        }

        throw new SemanticException("Unknown unary operator: " + op);
    }

    private Type analyzeFieldAccess(FieldAccess fieldAccess) {
        Type baseType = analyze(fieldAccess.getTarget());

        Type resolvedBaseType = TypeConformance.resolveType(baseType, symbolTable);
        if (!(resolvedBaseType instanceof RecordType recordType)) {
            throw new SemanticException("Attempt to access field '" + fieldAccess.getFieldName() + "' of non-record type: " + getTypeName(baseType));
        }

        VariableDeclaration field = findFieldInRecord(recordType, fieldAccess.getFieldName());
        
        if (field == null) {
            throw new SemanticException(
                    "Record '" + getTypeName(baseType) + "' has no field '" + fieldAccess.getFieldName() + "'"
            );
        }

        fieldAccess.resolvedFieldDeclaration = field;
        
        return field.type;
    }

    private VariableDeclaration findFieldInRecord(RecordType recordType, String fieldName) {
        for (VariableDeclaration member : recordType.members) {
            if (member.name.equals(fieldName)) return member;
        }
        return null;
    }

    private Type analyzeArrayAccess(ArrayAccess arrayAccess) {
        Type baseType = analyze(arrayAccess.getArray());
        Type indexType = analyze(arrayAccess.getIndex());
        
        if (!TypeConformance.conformsToInteger(indexType, symbolTable)) {
            throw new SemanticException("Array index must be integer-compatible, got " + getTypeName(indexType));
        }
        
        if (!isIntegerType(indexType)) {
            arrayAccess.getIndex().conversion = TypeConformance.checkExpressionConformance(
                    arrayAccess.getIndex(), PrimitiveType.INTEGER, indexType, symbolTable
            );
        }

        if (!(baseType instanceof ArrayType arrayType)) {
            throw new SemanticException("Attempt to index non-array type: " + getTypeName(baseType));
        }

        return arrayType.elementType;
    }

    private Type analyzeRoutineCall(RoutineCallExpression call) {
        Symbol routineSymbol = symbolTable.lookup(call.getName());
        if (routineSymbol == null) {
            throw new SemanticException("Undeclared routine: " + call.getName());
        }
        if (routineSymbol.getCategory() != SymbolCategory.ROUTINE) {
            throw new SemanticException(call.getName() + " is not a routine (it's a " + routineSymbol.getCategory() + ")");
        }

        call.resolvedSymbol = routineSymbol;

        List<Type> paramTypes = routineSymbol.getParameterTypes();
        List<Expression> arguments = call.getArguments();

        if (arguments.size() != paramTypes.size()) {
            throw new SemanticException("Routine '" + call.getName() + "' expects " + paramTypes.size() + " arguments, but got " + arguments.size());
        }

        for (int i = 0; i < arguments.size(); i++) {
            Expression arg = arguments.get(i);
            Type argType = analyze(arg);
            Type paramType = paramTypes.get(i);

            annotateConversion(arg, paramType, argType);
        }

        return routineSymbol.getType();
    }

    private boolean isComparisonOperator(TokenType op) {
        return op == TokenType.EQUAL || op == TokenType.NOT_EQUAL ||
               op == TokenType.LESS || op == TokenType.LESS_EQUAL ||
               op == TokenType.GREATER || op == TokenType.GREATER_EQUAL;
    }

    private boolean isLogicalOperator(TokenType op) {
        return op == TokenType.AND || op == TokenType.OR || op == TokenType.XOR;
    }

    private boolean isArithmeticOperator(TokenType op) {
        return op == TokenType.PLUS || op == TokenType.MINUS ||
               op == TokenType.MULTIPLY || op == TokenType.DIVIDE || op == TokenType.MODULO;
    }

    private boolean typesCompatible(Type t1, Type t2) {
        if (t1 instanceof PrimitiveType p1 && t2 instanceof PrimitiveType p2) {
            if (p1.typeName.equalsIgnoreCase(p2.typeName)) return true;
            if (isNumericType(t1) && isNumericType(t2)) return true;
        }
        if (t1 instanceof IdentifierType id1 && t2 instanceof IdentifierType id2) {
            return id1.name.equals(id2.name);
        }
        return t1.equals(t2);
    }

    private void annotateConversion(Expression expression, Type targetType, Type sourceType) {
        if (expression == null) {
            throw new SemanticException("Cannot annotate a null expression");
        }
        Conversion conversion = TypeConformance.checkExpressionConformance(
                expression, targetType, sourceType, symbolTable
        );
        expression.conversion = conversion;
    }

    private boolean isIntegerType(Type type) {
        return type instanceof PrimitiveType p && "integer".equals(p.typeName.toLowerCase());
    }

    private boolean isRealType(Type type) {
        return type instanceof PrimitiveType p && "real".equals(p.typeName.toLowerCase());
    }

    private boolean isNumericType(Type type) {
        return isIntegerType(type) || isRealType(type);
    }

    private String getTypeName(Type type) {
        if (type == null) return "null";
        if (type instanceof PrimitiveType p) return p.typeName;
        if (type instanceof IdentifierType id) return id.name;
        if (type instanceof ArrayType) return "array";
        if (type instanceof RecordType) return "record";
        return type.getClass().getSimpleName();
    }
}