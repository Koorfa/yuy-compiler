package semantic;

import ast.expressions.Expression;
import ast.expressions.IntegerLiteral;
import ast.expressions.UnaryExpression;
import ast.types.*;
import java.util.HashSet;
import java.util.Set;
import lexer.TokenType;


public class TypeConformance {

    public static Conversion checkConformance(Type targetType, Type sourceType) {
        return checkConformance(targetType, sourceType, null);
    }

    public static Conversion checkConformance(
            Type targetType,
            Type sourceType,
            SymbolTable symbolTable
    ) {
        if (targetType == null || sourceType == null) {
            throw new SemanticException("Cannot check conformance for null types");
        }

        if (symbolTable != null) {
            targetType = resolveType(targetType, symbolTable);
            sourceType = resolveType(sourceType, symbolTable);
        } else if (targetType instanceof IdentifierType targetId
                && sourceType instanceof IdentifierType sourceId
                && targetId.name.equals(sourceId.name)) {
            return Conversion.NONE;
        }

        if (targetType instanceof PrimitiveType targetPrim && 
            sourceType instanceof PrimitiveType sourcePrim) {
            return checkPrimitiveConformance(targetPrim, sourcePrim);
        }

        if (targetType == sourceType) {
            return Conversion.NONE;
        }

        throw new SemanticException(
            "Incompatible types: " + getTypeName(targetType) + 
            " and " + getTypeName(sourceType)
        );
    }

    public static Conversion checkExpressionConformance(
            Expression expression,
            Type targetType,
            Type sourceType,
            SymbolTable symbolTable
    ) {
        if (expression == null) {
            throw new SemanticException("Cannot check conformance for a null expression");
        }

        Conversion conversion = checkConformance(targetType, sourceType, symbolTable);
        if (conversion == Conversion.INT_TO_BOOL) {
            Integer literalValue = getSignedIntegerLiteralValue(expression);
            if (literalValue != null && literalValue != 0 && literalValue != 1) {
                throw new SemanticException(
                        "Integer literal must be 0 or 1 to be converted to boolean, got " + literalValue
                );
            }
        }
        return conversion;
    }

    private static Integer getSignedIntegerLiteralValue(Expression expression) {
        if (expression instanceof IntegerLiteral literal) {
            return literal.getValue();
        }
        if (expression instanceof UnaryExpression unary
                && (unary.getOperator() == TokenType.PLUS || unary.getOperator() == TokenType.MINUS)) {
            Integer operandValue = getSignedIntegerLiteralValue(unary.getOperand());
            if (operandValue == null) {
                return null;
            }
            return unary.getOperator() == TokenType.MINUS ? -operandValue : operandValue;
        }
        return null;
    }

    public static Type resolveType(Type type, SymbolTable symbolTable) {
        if (type == null) {
            throw new SemanticException("Cannot resolve a null type");
        }
        if (symbolTable == null) {
            throw new SemanticException("Cannot resolve type aliases without a symbol table");
        }
        return resolveType(type, symbolTable, new HashSet<>());
    }

    private static Type resolveType(
            Type type,
            SymbolTable symbolTable,
            Set<String> visitedAliases
    ) {
        if (!(type instanceof IdentifierType identifierType)) {
            return type;
        }

        if (!visitedAliases.add(identifierType.name)) {
            throw new SemanticException("Cyclic type alias: " + identifierType.name);
        }

        Symbol symbol = symbolTable.lookup(identifierType.name);
        if (symbol == null || symbol.getCategory() != SymbolCategory.TYPE) {
            throw new SemanticException("Unknown type: " + identifierType.name);
        }
        return resolveType(symbol.getType(), symbolTable, visitedAliases);
    }

    private static Conversion checkPrimitiveConformance(
            PrimitiveType target, 
            PrimitiveType source) {
        
        String t = target.typeName.toLowerCase();
        String s = source.typeName.toLowerCase();

        if (t.equals(s)) {
            return Conversion.NONE;
        }

        return switch (t) {
            case "integer" -> {
                if (s.equals("real")) yield Conversion.REAL_TO_INT;
                if (s.equals("boolean")) yield Conversion.BOOL_TO_INT;
                throw new SemanticException("Cannot convert " + s + " to " + t);
            }
            case "real" -> {
                if (s.equals("integer")) yield Conversion.INT_TO_REAL;
                if (s.equals("boolean")) yield Conversion.BOOL_TO_REAL;
                throw new SemanticException("Cannot convert " + s + " to " + t);
            }
            case "boolean" -> {
                if (s.equals("integer")) yield Conversion.INT_TO_BOOL;
                if (s.equals("real")) throw new SemanticException("Illegal assignment: cannot convert real to boolean");
                throw new SemanticException("Cannot convert " + s + " to " + t);
            }
            default -> throw new SemanticException("Cannot convert " + s + " to " + t);
        };
    }
    
    public static boolean conformsToBoolean(Type type) {
        if (type instanceof PrimitiveType primitiveType) {
            String name = primitiveType.typeName.toLowerCase();
            return name.equals("boolean") || name.equals("integer");
        }
        return false;
    }

    public static boolean conformsToBoolean(Type type, SymbolTable symbolTable) {
        return conformsToBoolean(resolveType(type, symbolTable));
    }

    public static boolean conformsToInteger(Type type) {
        if (type instanceof PrimitiveType primitiveType) {
            String name = primitiveType.typeName.toLowerCase();
            return name.equals("integer") || name.equals("real") || name.equals("boolean");
        }
        return false;
    }

    public static boolean conformsToInteger(Type type, SymbolTable symbolTable) {
        return conformsToInteger(resolveType(type, symbolTable));
    }

    private static String getTypeName(Type type) {
        if (type == null) {
            return "null"; 
        }
        
        if (type instanceof PrimitiveType p) return p.typeName;
        if (type instanceof IdentifierType id) return id.name;
        if (type instanceof ArrayType) return "array";
        if (type instanceof RecordType) return "record";
        
        return type.getClass().getSimpleName(); 
    }
}