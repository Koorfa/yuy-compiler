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

    public FirstPassAnalyzer() {
        this.symbolTable = new SymbolTable();
    }

    public void analyze(Program program) {
        for (Declaration declaration : program.declarations) {
            collectDeclaration(declaration);
        }
    }

    private void collectDeclaration(Declaration declaration) {

        if (declaration instanceof TypeDeclaration typeDeclaration) {

            validateType(typeDeclaration.type);

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

            if (routine.returnType != null) {
                validateType(routine.returnType);
            }

            List<Type> parameterTypes = new ArrayList<>();

            for (ParameterDeclaration parameter : routine.parameters) {
                validateType(parameter.type);
                parameterTypes.add(parameter.type);
            }

            Symbol symbol = new Symbol(
                    routine.name,
                    routine.returnType,
                    SymbolCategory.ROUTINE,
                    parameterTypes,
                    routine
            );

            symbolTable.add(symbol);
            return;
        }

        if (declaration instanceof VariableDeclaration variable) {

            if (variable.type != null) {
                validateType(variable.type);
            }

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

    private void validateType(Type type) {

        if (type instanceof PrimitiveType) {
            return;
        }

        if (type instanceof IdentifierType identifierType) {

            Symbol symbol = symbolTable.lookup(identifierType.name);

            if (symbol == null || symbol.getCategory() != SymbolCategory.TYPE) {
                throw new SemanticException(
                        "Unknown type: " + identifierType.name
                );
            }

            return;
        }

        if (type instanceof ArrayType arrayType) {
            validateType(arrayType.elementType);
            return;
        }

        if (type instanceof RecordType recordType) {
            for (VariableDeclaration member : recordType.members) {
                if (member.type != null) {
                    validateType(member.type);
                }
            }
        }
    }

    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
}