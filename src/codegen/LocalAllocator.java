package codegen;

import semantic.Symbol;
import semantic.SymbolCategory;
import ast.types.PrimitiveType;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class LocalAllocator {
    private final Stack<Map<Symbol, Integer>> scopes = new Stack<>();
    private final Stack<Integer> scopeSlotMarkers = new Stack<>();
    private int nextSlot;
    private int maxLocals;

    public LocalAllocator() {
        enterScope();
    }

    public void reset() {
        scopes.clear();
        scopeSlotMarkers.clear();
        nextSlot = 0;
        maxLocals = 0;
        enterScope();
    }

    public void enterScope() {
        scopes.push(new HashMap<>());
        scopeSlotMarkers.push(nextSlot);
    }

    public void exitScope() {
        if (scopes.size() <= 1) {
            throw new IllegalStateException("Cannot exit the root local scope");
        }
        scopes.pop();
        nextSlot = scopeSlotMarkers.pop();
    }

    public int allocate(Symbol symbol) {
        return allocate(symbol, slotWidth(symbol));
    }

    public int allocate(Symbol symbol, int width) {
        validateSymbol(symbol);
        if (symbol.getCategory() != SymbolCategory.VARIABLE) {
            throw new CodeGenException("Only variable symbols can receive local slots: " + symbol.getName());
        }

        Map<Symbol, Integer> currentScope = scopes.peek();
        if (currentScope.containsKey(symbol)) {
            throw new CodeGenException("Local symbol already allocated in current scope: " + symbol.getName());
        }

        int slot = allocateSlots(width);
        currentScope.put(symbol, slot);
        return slot;
    }

    public int allocateTemporary() {
        return allocateTemporary(1);
    }

    public int allocateTemporary(int width) {
        return allocateSlots(width);
    }

    public int lookup(Symbol symbol) {
        validateSymbol(symbol);
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Integer slot = scopes.get(i).get(symbol);
            if (slot != null) {
                return slot;
            }
        }
        throw new CodeGenException("No local slot allocated for symbol '" + symbol.getName() + "'");
    }

    public boolean containsInCurrentScope(Symbol symbol) {
        validateSymbol(symbol);
        return scopes.peek().containsKey(symbol);
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public int getNextSlot() {
        return nextSlot;
    }

    public int getScopeDepth() {
        return scopes.size();
    }

    private int allocateSlots(int width) {
        if (width != 1 && width != 2) {
            throw new IllegalArgumentException("JVM slot width must be 1 or 2");
        }
        int slot = nextSlot;
        nextSlot += width;
        maxLocals = Math.max(maxLocals, nextSlot);
        return slot;
    }

    private int slotWidth(Symbol symbol) {
        if (symbol.getType() instanceof PrimitiveType primitive
                && "real".equalsIgnoreCase(primitive.typeName)) {
            return 2;
        }
        return 1;
    }

    private void validateSymbol(Symbol symbol) {
        if (symbol == null) {
            throw new IllegalArgumentException("Symbol cannot be null");
        }
    }
}
