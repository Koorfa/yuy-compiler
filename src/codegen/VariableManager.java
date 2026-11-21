
package codegen;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.IdentityHashMap;
import semantic.Symbol;
import semantic.SymbolCategory;



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

        public String sourceName() {
            return sourceName;
        }

        public String jasminName() {
            return jasminName;
        }

        public String descriptor() {
            return descriptor;
        }

        public int slot() {
            return slot;
        }

        public int scopeLevel() {
            return scopeLevel;
        }

        public boolean isGlobal() {
            return scopeLevel == 0;
        }
    }

    private final Map<String, Variable> globals = new LinkedHashMap<>();
    private final Map<Symbol, Variable> variablesBySymbol = new IdentityHashMap<>();
    private final Deque<Map<String, Variable>> localScopes = new ArrayDeque<>();

    public Variable declareGlobal(String sourceName, String fieldName, String descriptor) {
        ensureNotInRoutine("global variable");

        String name = notBlank(sourceName, "source name");

        if (globals.containsKey(name)) {
            throw new CodeGenException("Global variable already declared: " + name);
        }

        Variable variable = new Variable(
                name,
                notBlank(fieldName, "Jasmin name"),
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
        enterScope();
    }

    public void enterScope() {
        localScopes.push(new LinkedHashMap<>());
    }

    public void exitScope() {
        if (localScopes.isEmpty()) {
            throw new CodeGenException("No variable scope to exit");
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

    public Variable declareLocal(String sourceName, String descriptor, int slot) {
        if (localScopes.isEmpty()) {
            throw new CodeGenException("Local variable outside a routine");
        }

        if (slot < 0) {
            throw new CodeGenException("Negative local variable slot: " + slot);
        }

        String name = notBlank(sourceName, "source name");
        Map<String, Variable> scope = localScopes.peek();

        if (scope.containsKey(name)) {
            throw new CodeGenException("Variable already declared in this scope: " + name);
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

    public Variable resolve(String sourceName) {
        String name = notBlank(sourceName, "source name");

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


    /** Links a semantic symbol to its JVM variable. */
    public void bind(Symbol symbol, Variable variable) {
        if (symbol == null || variable == null) {
            throw new CodeGenException("Symbol and variable must not be null");
        }

        if (symbol.getCategory() != SymbolCategory.VARIABLE) {
            throw new CodeGenException("Symbol is not a variable: " + symbol.getName());
        }

        if (!symbol.getName().equals(variable.sourceName())) {
            throw new CodeGenException("Symbol and variable names do not match");
        }

        Variable existing = variablesBySymbol.putIfAbsent(symbol, variable);

        if (existing != null && existing != variable) {
            throw new CodeGenException("Symbol already has a JVM location: " + symbol.getName());
        }
    }

    /** Finds the JVM variable corresponding to an exact semantic symbol. */
    public Variable resolve(Symbol symbol) {
        if (symbol == null) {
            throw new CodeGenException("Cannot resolve a null symbol");
        }

        Variable variable = variablesBySymbol.get(symbol);

        if (variable == null) {
            throw new CodeGenException("No JVM location for symbol: " + symbol.getName());
        }

        return variable;
    }


    public int scopeLevel() {
        return localScopes.size();
    }

    private void ensureNotInRoutine(String what) {
        if (!localScopes.isEmpty()) {
            throw new CodeGenException(
                    "Cannot declare " + what + " inside a routine"
            );
        }
    }

    private static String notBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new CodeGenException("Empty " + name);
        }
        return value;
    }
}
