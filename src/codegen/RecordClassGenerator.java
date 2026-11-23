package codegen;

import ast.declarations.TypeDeclaration;
import ast.declarations.VariableDeclaration;
import ast.types.*;
import semantic.SymbolTable;
import semantic.TypeConformance;
import java.util.LinkedHashMap;
import java.util.Map;

final class RecordClassGenerator {
    private final TypeMapper mapper;
    private final SymbolTable symbols;
    private final Map<RecordType, String> names = new LinkedHashMap<>();

    RecordClassGenerator(TypeMapper mapper, SymbolTable symbols) {
        this.mapper = mapper;
        this.symbols = symbols;
    }

    Map<String, String> generate(ast.Program program) {
        for (var declaration : program.declarations) {
            if (declaration instanceof TypeDeclaration td) {
                register(td.type, "YuyRecord_" + safe(td.name));
            } else if (declaration instanceof ast.declarations.RoutineDeclaration routine) {
                registerLocalTypes(routine.body, "YuyRecord_" + safe(routine.name));
            }
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (var entry : names.entrySet()) {
            result.put(entry.getValue() + ".j", emit(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private int localTypeId;

    private void registerLocalTypes(ast.Body body, String prefix) {
        if (body == null) return;
        for (var node : body.nodes) {
            if (node instanceof TypeDeclaration td) {
                register(td.type, prefix + "_" + safe(td.name) + "_" + (localTypeId++));
            } else if (node instanceof ast.statements.IfStatement conditional) {
                registerLocalTypes(conditional.thenBody, prefix);
                registerLocalTypes(conditional.elseBody, prefix);
            } else if (node instanceof ast.statements.WhileLoop loop) {
                registerLocalTypes(loop.body, prefix);
            } else if (node instanceof ast.statements.ForLoop loop) {
                registerLocalTypes(loop.body, prefix);
            }
        }
    }

    private void register(Type type, String suggestedName) {
        if (type == null) return;
        Type resolved = TypeConformance.resolveType(type, symbols);
        if (resolved instanceof ArrayType array) {
            register(array.elementType, suggestedName + "_Element");
        } else if (resolved instanceof RecordType record) {
            if (mapper.hasRecordType(record)) return;
            String internal = suggestedName;
            mapper.registerRecordType(record, internal);
            names.put(record, internal);
            for (VariableDeclaration member : record.members) {
                register(member.type, internal + "_" + safe(member.name));
            }
        }
    }

    private String emit(RecordType record, String internalName) {
        StringBuilder out = new StringBuilder();
        out.append(".class public ").append(internalName).append('\n');
        out.append(".super java/lang/Object\n\n");
        for (VariableDeclaration member : record.members) {
            if (member.type == null) {
                throw new CodeGenException("Record member requires a type: " + member.name);
            }
            out.append(".field public ").append(member.name).append(' ')
                    .append(mapper.getJasminType(member.type)).append('\n');
        }
        out.append("\n.method public <init>()V\n")
                .append("    .limit stack 2\n")
                .append("    .limit locals 1\n")
                .append("    aload_0\n")
                .append("    invokespecial java/lang/Object/<init>()V\n");
        for (VariableDeclaration member : record.members) {
            if (ArrayRecordSupport.isAggregate(member.type, mapper, symbols)) {
                String spec = ArrayRecordSupport.spec(member.type, mapper, symbols);
                String descriptor = mapper.getJasminType(member.type);
                out.append("    aload_0\n")
                        .append("    ldc \"").append(spec).append("\"\n")
                        .append("    invokestatic YuyRuntime/allocate(Ljava/lang/String;)Ljava/lang/Object;\n")
                        .append("    checkcast ").append(checkcastName(descriptor)).append('\n')
                        .append("    putfield ").append(internalName).append('/')
                        .append(member.name).append(' ').append(descriptor).append('\n');
            }
        }
        out.append("    return\n")
                .append(".end method\n");
        return out.toString();
    }

    private static String checkcastName(String descriptor) {
        if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
            return descriptor.substring(1, descriptor.length() - 1);
        }
        return descriptor;
    }

    private static String safe(String name) {
        return name.replaceAll("[^A-Za-z0-9_$]", "_");
    }
}
