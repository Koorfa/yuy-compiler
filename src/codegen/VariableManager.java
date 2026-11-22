package codegen;

import semantic.Symbol;

// TODO
public interface VariableManager {
    void loadVariable(Symbol symbol);
    void storeVariable(Symbol symbol);
    String getVariableInfo(Symbol symbol);
    int registerLocal(Symbol symbol);
    int allocateTemporary(int width);
    void enterScope();
    void exitScope();
    int getMaxLocals();
}