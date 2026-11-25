package codegen;

import ast.declarations.RoutineDeclaration;
import ast.types.*;
import java.util.HashMap;
import java.util.Map;
import semantic.SymbolTable;
import semantic.TypeConformance;

/**
 * Maps Yuy types to the Jasmin/JVM representations used during code generation.
 * Primitive values use JVM descriptors, array types use array descriptors,
 * and record types use registered JVM internal class names.
 */
public class TypeMapper {
    private final SymbolTable symbolTable;
    private final Map<RecordType, String> recordNames = new HashMap<>();

    /**
     * Creates a type mapper that uses the given symbol table to resolve types.
     *
     * @param symbolTable symbol table containing type declarations
     */
    public TypeMapper(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * Converts a Yuy type to its Jasmin field or value descriptor.
     *
     * @param type Yuy type to convert; {@code null} represents void
     * @return {@code V} for void, {@code I} for integer or boolean,
     *         {@code D} for real, or the JVM descriptor for an array or record
     * @throws CodeGenException if the resolved type is unsupported or a record
     *         class has not been registered
     */
    public String getJasminType(Type type) {
        if (type == null)
            return "V"; // void
        Type resolved = TypeConformance.resolveType(type, symbolTable);
        
        if (resolved instanceof PrimitiveType) {
            String name = ((PrimitiveType) resolved).typeName.toLowerCase();
            if (name.equals("integer"))
                return "I"; // int
            if (name.equals("real"))
                return "D"; // double
            if (name.equals("boolean"))
                return "I"; // int (in jvm boolean uses int)
        }
        if (resolved instanceof ArrayType) {
            ArrayType arr = (ArrayType) resolved;
            String elemType = getJasminType(arr.elementType);
            return "[" + elemType; // [ stands for array, symbol after is type of elements in array
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

    /**
     * Gets the JVM internal class name for a record type.
     *
     * @param type Yuy record type or type that resolves to a record
     * @return the registered JVM internal class name
     * @throws CodeGenException if a record type has not been registered or the
     *         resolved type is unsupported
     */
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

    /**
     * Registers the JVM internal class name for a record type.
     *
     * @param type Yuy record type
     * @param internalName JVM internal class name, such as {@code generated/Record}
     */
    public void registerRecordType(RecordType type, String internalName) {
        recordNames.putIfAbsent(type, internalName);
    }

    /**
     * Checks whether a JVM internal class name is registered for a record type.
     *
     * @param type Yuy record type
     * @return {@code true} if the record type has been registered
     */
    public boolean hasRecordType(RecordType type) {
        return recordNames.containsKey(type);
    }

    /**
     * Gets an immutable snapshot of the registered record type mappings.
     *
     * @return mapping from Yuy record types to JVM internal class names
     */
    public Map<RecordType, String> getRecordTypes() {
        return Map.copyOf(recordNames);
    }

    /**
     * Builds the Jasmin method descriptor for a routine declaration.
     *
     * @param routine routine whose parameter and return types are to be mapped
     * @return JVM method descriptor in the form {@code (parameters)returnType}
     * @throws CodeGenException if a parameter or return type cannot be mapped
     */
    public String getJasminMethodDescriptor(RoutineDeclaration routine) {
        StringBuilder sb = new StringBuilder("(");
        for (ast.declarations.ParameterDeclaration param : routine.parameters) {
            sb.append(getJasminType(param.type));
        }
        sb.append(")");
        sb.append(getJasminReturnType(routine.returnType));
        return sb.toString();
    }

    /**
     * Converts a Yuy routine return type to its Jasmin return descriptor.
     *
     * @param type Yuy return type; {@code null} represents a procedure with no return value
     * @return {@code V} for void or the descriptor returned by {@link #getJasminType(Type)}
     * @throws CodeGenException if the resolved type is unsupported
     */
    public String getJasminReturnType(Type type) {
        if (type == null)
            return "V"; // void
        return getJasminType(type);
    }
}