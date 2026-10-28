package semantic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

    public SymbolTable() {
        enterScope();
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
    }

    public void exitScope() {
        if (scopes.size() <= 1) {
            throw new SemanticException("Cannot exit global scope");
        }

        scopes.pop();
    }

    public void add(Symbol symbol) {
        Map<String, Symbol> currentScope = scopes.peek();

        if (currentScope.containsKey(symbol.getName())) {
            throw new SemanticException(
                    "Duplicate declaration: " + symbol.getName()
            );
        }

        currentScope.put(symbol.getName(), symbol);
    }

    public Symbol lookupCurrentScope(String name) {
        return scopes.peek().get(name);
    }

    public Symbol lookup(String name) {
        for (Map<String, Symbol> scope : scopes) {
            Symbol symbol = scope.get(name);

            if (symbol != null) {
                return symbol;
            }
        }

        return null;
    }
}