package codegen;

import ast.types.Type;
import semantic.SymbolTable;

public class CodegenContext {
    private String currentRoutineName;
    private Type currentReturnType;
    
    private final SymbolTable symbolTable;
    private final JasminWriter writer;
    private final LabelManager labelManager;
    private final StackTracker stackTracker;
    private final VariableManager variableManager;
    private final TypeMapper typeMapper;

    public CodegenContext(
            SymbolTable symbolTable,
            JasminWriter writer,
            LabelManager labelManager,
            StackTracker stackTracker,
            VariableManager variableManager
    ) {
        this.symbolTable = symbolTable;
        this.writer = writer;
        this.labelManager = labelManager;
        this.stackTracker = stackTracker;
        this.variableManager = variableManager;
        this.typeMapper = new TypeMapper(symbolTable);
    }

    public CodegenContext(
        SymbolTable symbolTable,
        JasminWriter writer,
        LabelManager labelManager,
        StackTracker stackTracker,
        VariableManager variableManager,
        TypeMapper typeMapper
    ) {
        this.symbolTable = symbolTable;
        this.writer = writer;
        this.labelManager = labelManager;
        this.stackTracker = stackTracker;
        this.variableManager = variableManager;
        this.typeMapper = typeMapper;
    }

    public String getCurrentRoutineName() { return currentRoutineName; }
    public void setCurrentRoutineName(String currentRoutineName) { this.currentRoutineName = currentRoutineName; }

    public Type getCurrentReturnType() { return currentReturnType; }
    public void setCurrentReturnType(Type currentReturnType) { this.currentReturnType = currentReturnType; }

    public SymbolTable getSymbolTable() { return symbolTable; }
    public JasminWriter getWriter() { return writer; }
    public LabelManager getLabelManager() { return labelManager; }
    public StackTracker getStackTracker() { return stackTracker; }
    public VariableManager getVariableManager() { return variableManager; }
    public TypeMapper getTypeMapper() { return typeMapper; }
}