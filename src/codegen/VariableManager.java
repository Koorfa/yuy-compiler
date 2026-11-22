
package codegen;

import ast.declarations.VariableDeclaration;
import ast.types.Type;
import semantic.Symbol;
import semantic.SymbolCategory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VariableManager {

    public static final class Variable {
        private final String sourceName;
        private final String jasminName;
        private final String descriptor;
        private final int slot;
        private final int scopeLevel;

        private Variable(String sourceName, String jasminName,
                         String descriptor, int slot, int scopeLevel) {
            this.sourceName = sourceName;
            this.jasminName = jasminName;
            this.descriptor = descriptor;
            this.slot = slot;
            this.scopeLevel = scopeLevel;
        }

        public String sourceName() { return sourceName; }
        public String jasminName() { return jasminName; }
        public String descriptor() { return descriptor; }
        public int slot() { return slot; }
        public int scopeLevel() { return scopeLevel; }
        public boolean isGlobal() { return scopeLevel == 0; }
    }

    private final Map<String, Variable> globals = new LinkedHashMap<>();
    private final Deque<Map<String, Variable>> localScopes = new ArrayDeque<>();
    private final Map<Symbol, Variable> variablesBySymbol = new IdentityHashMap<>();
    private final Deque<List<Symbol>> scopedSymbols = new ArrayDeque<>();

    private final JasminWriter writer;
    private final LocalAllocator allocator;
    private final TypeMapper typeMapper;

    public VariableManager() {
        this(null, null, null);
    }

    public VariableManager(JasminWriter writer,
                           LocalAllocator allocator,
                           TypeMapper typeMapper) {
        this.writer = writer;
        this.allocator = allocator;
        this.typeMapper = typeMapper;
    }

    public Variable declareGlobal(String name, String fieldName, String descriptor) {
        if (!localScopes.isEmpty()) {
            throw new CodeGenException("Cannot declare global inside a routine");
        }

        name = notBlank(name, "source name");

        if (globals.containsKey(name)) {
            throw new CodeGenException("Global variable already declared: " + name);
        }

        Variable variable = new Variable(
                name,
                notBlank(fieldName, "field name"),
                notBlank(descriptor, "descriptor"),
                -1,
                0
        );

        globals.put(name, variable);
        return variable;
    }

    public void beginRoutine() {
        if (!localScopes.isEmpty()) {
            throw new CodeGenException("A routine scope is already active");
        }

        if (allocator != null) {
            allocator.reset();
        }

        enterScope();
    }

    public void enterScope() {
        if (!localScopes.isEmpty() && allocator != null) {
            allocator.enterScope();
        }

        localScopes.push(new LinkedHashMap<>());
        scopedSymbols.push(new ArrayList<>());
    }

    public void exitScope() {
        if (localScopes.isEmpty()) {
            throw new CodeGenException("No variable scope to exit");
        }

        if (localScopes.size() > 1 && allocator != null) {
            allocator.exitScope();
        }

        for (Symbol symbol : scopedSymbols.pop()) {
            variablesBySymbol.remove(symbol);
        }

        localScopes.pop();
    }

    public void endRoutine() {
        if (localScopes.size() != 1) {
            throw new CodeGenException(
                    "Unbalanced scopes at end of routine: " + localScopes.size()
            );
        }

        exitScope();
    }

    public Variable declareLocal(String name, String descriptor, int slot) {
        if (localScopes.isEmpty()) {
            throw new CodeGenException("Local variable outside a routine");
        }

        if (slot < 0) {
            throw new CodeGenException("Negative slot: " + slot);
        }

        name = notBlank(name, "source name");
        Map<String, Variable> scope = localScopes.peek();

        if (scope.containsKey(name)) {
            throw new CodeGenException(
                    "Variable already declared in this scope: " + name
            );
        }

        Variable variable = new Variable(
                name,
                null,
                notBlank(descriptor, "descriptor"),
                slot,
                localScopes.size()
        );

        scope.put(name, variable);
        return variable;
    }

    public Variable resolve(String name) {
        name = notBlank(name, "source name");

        for (Map<String, Variable> scope : localScopes) {
            Variable variable = scope.get(name);

            if (variable != null) {
                return variable;
            }
        }

        Variable global = globals.get(name);

        if (global != null) {
            return global;
        }

        throw new CodeGenException("Variable is not declared: " + name);
    }

    public void bind(Symbol symbol, Variable variable) {
        if (symbol == null || variable == null) {
            throw new CodeGenException(
                    "Symbol and variable must not be null"
            );
        }

        if (symbol.getCategory() != SymbolCategory.VARIABLE
                || !symbol.getName().equals(variable.sourceName())) {
            throw new CodeGenException(
                    "Invalid variable binding: " + symbol.getName()
            );
        }

        Variable previous = variablesBySymbol.putIfAbsent(symbol, variable);

        if (previous != null && previous != variable) {
            throw new CodeGenException(
                    "Symbol already bound: " + symbol.getName()
            );
        }

        if (previous == null && !variable.isGlobal()) {
            if (scopedSymbols.isEmpty()) {
                throw new CodeGenException("Local binding outside a scope");
            }

            scopedSymbols.peek().add(symbol);
        }
    }

    public Variable resolve(Symbol symbol) {
        if (symbol == null) {
            throw new CodeGenException("Null symbol");
        }

        Variable variable = variablesBySymbol.get(symbol);

        if (variable == null) {
            throw new CodeGenException(
                    "Unbound symbol: " + symbol.getName()
            );
        }

        return variable;
    }

    public int registerLocal(Symbol symbol) {
        requireIntegration();

        if (symbol == null) {
            throw new CodeGenException("Null local symbol");
        }

        if (localScopes.isEmpty()) {
            throw new CodeGenException("No active routine");
        }

        if (localScopes.peek().containsKey(symbol.getName())) {
            throw new CodeGenException(
                    "Duplicate local: " + symbol.getName()
            );
        }

        Type type = symbol.getType();

        if (type == null
                && symbol.getDeclaration() instanceof VariableDeclaration declaration
                && declaration.initializer != null) {
            type = declaration.initializer.resolvedType;
        }

        if (type == null) {
            throw new CodeGenException(
                    "Unknown variable type: " + symbol.getName()
            );
        }

        String descriptor = typeMapper.getJasminType(type);

        if ("V".equals(descriptor)) {
            throw new CodeGenException(
                    "Invalid variable type: " + symbol.getName()
            );
        }

        int width = "D".equals(descriptor) ? 2 : 1;
        int slot = allocator.allocate(symbol, width);

        Variable variable = declareLocal(
                symbol.getName(), descriptor, slot
        );

        bind(symbol, variable);
        return slot;
    }

    public int allocateTemporary(int width) {
        requireIntegration();

        if (localScopes.isEmpty()) {
            throw new CodeGenException("Temporary outside a routine");
        }

        return allocator.allocateTemporary(width);
    }

    public int getMaxLocals() {
        requireIntegration();
        return allocator.getMaxLocals();
    }

    public void loadVariable(Symbol symbol) {
        requireIntegration();

        Variable variable = resolve(symbol);

        if (variable.isGlobal()) {
            writer.emit(
                    "getstatic " + variable.jasminName()
                            + " " + variable.descriptor()
            );
        } else {
            writer.emit(
                    loadOpcode(variable.descriptor()) + " " + variable.slot()
            );
        }
    }

    public void storeVariable(Symbol symbol) {
        requireIntegration();

        Variable variable = resolve(symbol);

        if (variable.isGlobal()) {
            writer.emit(
                    "putstatic " + variable.jasminName()
                            + " " + variable.descriptor()
            );
        } else {
            writer.emit(
                    storeOpcode(variable.descriptor()) + " " + variable.slot()
            );
        }
    }

    public String getVariableInfo(Symbol symbol) {
        Variable variable = resolve(symbol);

        return variable.isGlobal()
                ? variable.jasminName()
                : String.valueOf(variable.slot());
    }

    public int scopeLevel() {
        return localScopes.size();
    }

    private void requireIntegration() {
        if (writer == null || allocator == null || typeMapper == null) {
            throw new CodeGenException(
                    "VariableManager needs writer, allocator and type mapper"
            );
        }
    }

    private static String loadOpcode(String descriptor) {
        if ("D".equals(descriptor)) return "dload";
        if ("I".equals(descriptor) || "Z".equals(descriptor)) return "iload";
        return "aload";
    }

    private static String storeOpcode(String descriptor) {
        if ("D".equals(descriptor)) return "dstore";
        if ("I".equals(descriptor) || "Z".equals(descriptor)) return "istore";
        return "astore";
    }

    private static String notBlank(String value, String role) {
        if (value == null || value.isBlank()) {
            throw new CodeGenException("Empty " + role);
        }

        return value;
    }
}
