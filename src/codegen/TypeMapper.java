package codegen;

import java.util.HashMap;
import java.util.Map;

import ast.declarations.RoutineDeclaration;
import ast.types.*;
import semantic.Symbol;
import semantic.SymbolTable;
import semantic.TypeConformance;

public class TypeMapper {
    private final SymbolTable symbolTable;
    private final Map<RecordType, String> recordNames = new HashMap<>();

    public TypeMapper(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public String getJasminType(Type type) {
        if (type == null)
            return "V";
        Type resolved = TypeConformance.resolveType(type, symbolTable);
        
        if (resolved instanceof PrimitiveType) {
            String name = ((PrimitiveType) resolved).typeName.toLowerCase();
            if (name.equals("integer"))
                return "I";
            if (name.equals("real"))
                return "D";
            if (name.equals("boolean"))
                return "I";
        }
        if (resolved instanceof ArrayType) {
            ArrayType arr = (ArrayType) resolved;
            String elemType = getJasminType(arr.elementType);
            return "[" + elemType;
        }
        if (resolved instanceof RecordType) {
            RecordType record = (RecordType) resolved;
            String name = recordNames.get(record);
            if (name == null) {
                throw new CodeGenException("Record type has not been registered for code generation");
            }
            return "L" + name + ";";
        }
        throw new CodeGenException("Unsupported type for JVM mapping: " + resolved.getClass().getSimpleName());
    }

    public String getRecordClassName(Type type) {
        Type resolved = TypeConformance.resolveType(type, symbolTable);
        if (resolved instanceof RecordType) {
            RecordType record = (RecordType) resolved;
            String name = recordNames.get(record);
            if (name == null) {
                throw new CodeGenException("Record type has not been registered for code generation");
            }
            return name;
        }
        String descriptor = getJasminType(type);
        return descriptor.startsWith("L") && descriptor.endsWith(";")
                ? descriptor.substring(1, descriptor.length() - 1)
                : descriptor;
    }

    public void registerRecordType(RecordType type, String internalName) {
        recordNames.putIfAbsent(type, internalName);
    }

    public boolean hasRecordType(RecordType type) {
        return recordNames.containsKey(type);
    }

    public Map<RecordType, String> getRecordTypes() {
        return Map.copyOf(recordNames);
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
        if (type == null)
            return "V";
        return getJasminType(type);
    }
}