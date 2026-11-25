package codegen;

import ast.types.Type;
import semantic.SymbolTable;

/**
 * Holds the shared state used by JVM code generation components.
 * A context provides access to symbols, output, labels, operand-stack tracking,
 * local variables, and Yuy-to-JVM type mapping.
 */
public class CodegenContext {
    private String currentRoutineName;
    private Type currentReturnType;
    
    private final SymbolTable symbolTable;
    private final JasminWriter writer;
    private final LabelManager labelManager;
    private final StackTracker stackTracker;
    private final VariableManager variableManager;
    private final TypeMapper typeMapper;

    /**
     * Creates a context and constructs a default type mapper from the symbol table.
     *
     * @param symbolTable semantic symbol table
     * @param writer Jasmin output writer
     * @param labelManager generator for unique labels
     * @param stackTracker tracker for the simulated JVM operand stack
     * @param variableManager manager for local and global variables
     */
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

    /**
     * Creates a context using the supplied type mapper.
     *
     * @param symbolTable semantic symbol table
     * @param writer Jasmin output writer
     * @param labelManager generator for unique labels
     * @param stackTracker tracker for the simulated JVM operand stack
     * @param variableManager manager for local and global variables
     * @param typeMapper mapper used to convert Yuy types to JVM descriptors
     */
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

    /**
     * Gets the name of the routine currently being generated.
     *
     * @return current routine name, or {@code null} if none is active
     */
    public String getCurrentRoutineName() { return currentRoutineName; }

    /**
     * Sets the name of the routine currently being generated.
     *
     * @param currentRoutineName routine name
     */
    public void setCurrentRoutineName(String currentRoutineName) { this.currentRoutineName = currentRoutineName; }

    /**
     * Gets the declared return type of the current routine.
     *
     * @return current return type, or {@code null} for a procedure
     */
    public Type getCurrentReturnType() { return currentReturnType; }

    /**
     * Sets the declared return type of the current routine.
     *
     * @param currentReturnType return type, or {@code null} for a procedure
     */
    public void setCurrentReturnType(Type currentReturnType) { this.currentReturnType = currentReturnType; }

    /**
     * Gets the semantic symbol table.
     *
     * @return symbol table associated with this context
     */
    public SymbolTable getSymbolTable() { return symbolTable; }

    /**
     * Gets the Jasmin output writer.
     *
     * @return shared output writer
     */
    public JasminWriter getWriter() { return writer; }

    /**
     * Gets the label manager.
     *
     * @return shared label manager
     */
    public LabelManager getLabelManager() { return labelManager; }

    /**
     * Gets the operand-stack tracker.
     *
     * @return shared stack tracker
     */
    public StackTracker getStackTracker() { return stackTracker; }

    /**
     * Gets the variable manager.
     *
     * @return shared variable manager
     */
    public VariableManager getVariableManager() { return variableManager; }

    /**
     * Gets the Yuy-to-JVM type mapper.
     *
     * @return shared type mapper
     */
    public TypeMapper getTypeMapper() { return typeMapper; }
}