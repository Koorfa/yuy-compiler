package semantic;

import ast.declarations.Declaration;
import ast.types.Type;

import java.util.Collections;
import java.util.List;

public class Symbol {
    private final String name;
    private final Type type;
    private final SymbolCategory category;
    private final List<Type> parameterTypes;
    private final Declaration declaration;

    public Symbol(
            String name,
            Type type,
            SymbolCategory category,
            List<Type> parameterTypes,
            Declaration declaration
    ) {
        this.name = name;
        this.type = type;
        this.category = category;
        this.parameterTypes = parameterTypes == null
                ? Collections.emptyList()
                : parameterTypes;
        this.declaration = declaration;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public SymbolCategory getCategory() {
        return category;
    }

    public List<Type> getParameterTypes() {
        return parameterTypes;
    }

    public Declaration getDeclaration() {
        return declaration;
    }
}