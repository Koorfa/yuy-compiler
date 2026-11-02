package semantic;

import ast.Program;
import ast.declarations.Declaration;
import ast.declarations.TypeDeclaration;
import ast.declarations.VariableDeclaration;
import ast.types.ArrayType;
import ast.types.IdentifierType;
import ast.types.PrimitiveType;
import ast.types.RecordType;
import ast.types.Type;

import ast.declarations.ParameterDeclaration;
import ast.declarations.RoutineDeclaration;

import java.util.ArrayList;
import java.util.List;

public class FirstPassAnalyzer {

    private final SymbolTable symbolTable;
    private ExpressionAnalyzer expressionAnalyzer;

    public FirstPassAnalyzer() {
        this.symbolTable = new SymbolTable();
    }

    public void analyze(Program program) {
        for (Declaration declaration : program.declarations) {
            collectDeclaration(declaration);
        }

        expressionAnalyzer = new ExpressionAnalyzer(symbolTable);

        for (Declaration declaration : program.declarations) {
            validateDeclarationTypes(declaration);
        }

        for (Declaration declaration : program.declarations) {
            if (declaration instanceof VariableDeclaration variable && variable.type == null) {
                Symbol symbol = symbolTable.lookupCurrentScope(variable.name);
                if (symbol == null) {
                    throw new SemanticException(
                            "Missing symbol for variable '" + variable.name + "' during type inference"
                    );
                }
                expressionAnalyzer.inferVariableType(symbol);
            }
        }

        validateForwardDeclarations(program);
    }

    private void collectDeclaration(Declaration declaration) {

        if (declaration instanceof TypeDeclaration typeDeclaration) {

            Symbol symbol = new Symbol(
                    typeDeclaration.name,
                    typeDeclaration.type,
                    SymbolCategory.TYPE,
                    null,
                    typeDeclaration
            );

            symbolTable.add(symbol);
            return;
        }

        if (declaration instanceof RoutineDeclaration routine) {
            List<Type> parameterTypes = new ArrayList<>();

            for (ParameterDeclaration parameter : routine.parameters) {
                parameterTypes.add(parameter.type);
            }

            Symbol symbol = new Symbol(
                    routine.name,
                    routine.returnType,
                    SymbolCategory.ROUTINE,
                    parameterTypes,
                    routine
            );

            addRoutineSymbol(routine, symbol);
            return;
        }

        if (declaration instanceof VariableDeclaration variable) {
            Symbol symbol = new Symbol(
                    variable.name,
                    variable.type,
                    SymbolCategory.VARIABLE,
                    null,
                    variable
            );

            symbolTable.add(symbol);
        }
    }

    private void validateDeclarationTypes(Declaration declaration) {
        if (declaration instanceof TypeDeclaration typeDeclaration) {
            validateType(typeDeclaration.type, false);
        } else if (declaration instanceof RoutineDeclaration routine) {
            if (routine.returnType != null) {
                validateType(routine.returnType, false);
            }
            for (ParameterDeclaration parameter : routine.parameters) {
                validateType(parameter.type, true);
            }
        } else if (declaration instanceof VariableDeclaration variable && variable.type != null) {
            validateType(variable.type, false);
        }
    }

    private void validateType(Type type, boolean allowSizelessArray) {

        if (type instanceof PrimitiveType) {
            return;
        }

        if (type instanceof IdentifierType identifierType) {
            TypeConformance.resolveType(identifierType, symbolTable);
            return;
        }

        if (type instanceof ArrayType arrayType) {
            if (arrayType.size == null) {
                if (!allowSizelessArray) {
                    throw new SemanticException(
                            "Array without a size is only allowed as a routine parameter"
                    );
                }
            } else {
                Type sizeType = expressionAnalyzer.analyze(arrayType.size);
                Type resolvedSizeType = TypeConformance.resolveType(sizeType, symbolTable);
                if (!(resolvedSizeType instanceof PrimitiveType primitiveType)
                        || !primitiveType.typeName.equalsIgnoreCase("integer")) {
                    throw new SemanticException(
                            "Array size must have integer type, got " + typeName(resolvedSizeType)
                    );
                }
                arrayType.resolvedSize = expressionAnalyzer.evaluateIntegerConstant(arrayType.size);
            }
            validateType(arrayType.elementType, allowSizelessArray);
            return;
        }

        if (type instanceof RecordType recordType) {
            for (VariableDeclaration member : recordType.members) {
                if (member.type != null) {
                    validateType(member.type, false);
                }
            }
        }
    }

    private void addRoutineSymbol(RoutineDeclaration routine, Symbol symbol) {
        Symbol existing = symbolTable.lookupCurrentScope(routine.name);
        if (existing == null) {
            symbolTable.add(symbol);
            return;
        }
        if (existing.getCategory() != SymbolCategory.ROUTINE
                || !(existing.getDeclaration() instanceof RoutineDeclaration existingRoutine)) {
            symbolTable.add(symbol);
            return;
        }

        boolean existingIsForward = existingRoutine.body == null;
        boolean newIsForward = routine.body == null;
        if (existingIsForward == newIsForward) {
            symbolTable.add(symbol);
            return;
        }
        if (!sameRoutineSignature(existingRoutine, routine)) {
            throw new SemanticException(
                    "Forward declaration signature does not match routine '" + routine.name + "'"
            );
        }

        if (existingIsForward) {
            existing.setDeclaration(routine);
            existing.setParameterTypes(symbol.getParameterTypes());
        }
    }

    private boolean sameRoutineSignature(RoutineDeclaration first, RoutineDeclaration second) {
        if ((first.returnType == null) != (second.returnType == null)
                || first.parameters.size() != second.parameters.size()) {
            return false;
        }
        if (first.returnType != null && !sameTypeSignature(first.returnType, second.returnType)) {
            return false;
        }
        for (int i = 0; i < first.parameters.size(); i++) {
            if (!sameTypeSignature(first.parameters.get(i).type, second.parameters.get(i).type)) {
                return false;
            }
        }
        return true;
    }

    private boolean sameTypeSignature(Type first, Type second) {
        Type resolvedFirst = TypeConformance.resolveType(first, symbolTable);
        Type resolvedSecond = TypeConformance.resolveType(second, symbolTable);
        if (resolvedFirst instanceof PrimitiveType firstPrimitive
                && resolvedSecond instanceof PrimitiveType secondPrimitive) {
            return firstPrimitive.typeName.equalsIgnoreCase(secondPrimitive.typeName);
        }
        if (resolvedFirst instanceof ArrayType firstArray
                && resolvedSecond instanceof ArrayType secondArray) {
            if ((firstArray.size == null) != (secondArray.size == null)) {
                return false;
            }
            if (firstArray.size != null
                    && evaluateArraySize(firstArray) != evaluateArraySize(secondArray)) {
                return false;
            }
            return sameTypeSignature(firstArray.elementType, secondArray.elementType);
        }
        return resolvedFirst == resolvedSecond;
    }

    private int evaluateArraySize(ArrayType arrayType) {
        Type sizeType = expressionAnalyzer.analyze(arrayType.size);
        Type resolvedSizeType = TypeConformance.resolveType(sizeType, symbolTable);
        if (!(resolvedSizeType instanceof PrimitiveType primitiveType)
                || !primitiveType.typeName.equalsIgnoreCase("integer")) {
            throw new SemanticException("Array size in routine signature must have integer type");
        }
        return expressionAnalyzer.evaluateIntegerConstant(arrayType.size);
    }

    private void validateForwardDeclarations(Program program) {
        for (Declaration declaration : program.declarations) {
            if (declaration instanceof RoutineDeclaration routine && routine.body == null) {
                Symbol symbol = symbolTable.lookupCurrentScope(routine.name);
                if (symbol == null
                        || !(symbol.getDeclaration() instanceof RoutineDeclaration implementation)
                        || implementation.body == null) {
                    throw new SemanticException(
                            "Forward declaration for routine '" + routine.name + "' has no implementation"
                    );
                }
            }
        }
    }

    private String typeName(Type type) {
        if (type == null) {
            return "unknown";
        }
        if (type instanceof PrimitiveType primitiveType) {
            return primitiveType.typeName;
        }
        return type.getClass().getSimpleName();
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
}