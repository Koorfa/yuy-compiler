package codegen;

import ast.declarations.RoutineDeclaration;
import ast.types.*;
import semantic.SymbolTable;
import semantic.TypeConformance;

public class TypeMapper {
    private final SymbolTable symbolTable;

    public TypeMapper(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public String getJasminType(Type type) {
        if (type == null) return "V";
        Type resolved = TypeConformance.resolveType(type, symbolTable);
        
        if (resolved instanceof PrimitiveType) {
            String name = ((PrimitiveType) resolved).typeName.toLowerCase();
            if (name.equals("integer")) return "I";
            if (name.equals("real")) return "D";
            if (name.equals("boolean")) return "I";
        }
        if (resolved instanceof ArrayType) {
            ArrayType arr = (ArrayType) resolved;
            String elemType = getJasminType(arr.elementType);
            return "[" + elemType;
        }
        if (resolved instanceof RecordType) {
            // TODO: Replace this fallback with the actual generated class name 
            // for the record type once DeclarationGenerator implements record class generation.
            return "Ljava/lang/Object;"; 
        }
        return "Ljava/lang/Object;";
    }

    public String getRecordClassName(Type type) {
        Type resolved = TypeConformance.resolveType(type, symbolTable);
        if (resolved instanceof RecordType) {
            // TODO: Return the actual generated class name
            // instead of the fallback once record declarations are compiled to classes.
            return "Ljava/lang/Object;"; 
        }
        return getJasminType(type);
    }

    public String getJasminMethodDescriptor(RoutineDeclaration routine) {
        StringBuilder sb = new StringBuilder("(");
        for (ast.declarations.ParameterDeclaration param : routine.parameters) {
            sb.append(getJasminType(param.type));
        }
        sb.append(")");
        sb.append(getJasminReturnType(routine.returnType));
        return sb.toString();
    }

    public String getJasminReturnType(Type type) {
        if (type == null) return "V";
        return getJasminType(type);
    }
}