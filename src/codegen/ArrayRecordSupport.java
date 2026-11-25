package codegen;

import ast.types.*;
import semantic.SymbolTable;
import semantic.TypeConformance;

final class ArrayRecordSupport {
    private ArrayRecordSupport() {}

    static String spec(Type type, TypeMapper mapper, SymbolTable symbols) {
        Type resolved = TypeConformance.resolveType(type, symbols);
        if (resolved instanceof PrimitiveType primitive) {
            return primitive.typeName.equalsIgnoreCase("real") ? "D" : "I";
        }
        if (resolved instanceof ArrayType array) {
            if (array.resolvedSize == null || array.resolvedSize < 0) {
                throw new CodeGenException("Cannot allocate an array with unspecified/negative size");
            }
            return "A:" + array.resolvedSize + "(" + spec(array.elementType, mapper, symbols) + ")";
        }
        if (resolved instanceof RecordType) {
            return "R:" + mapper.getRecordClassName(type);
        }
        return null;
    }

    static boolean isAggregate(Type type, TypeMapper mapper, SymbolTable symbols) {
        String spec = spec(type, mapper, symbols);
        return spec != null && (spec.startsWith("A:") || spec.startsWith("R:"));
    }
}
