package codegen;

public class JasminWriter {
    private final StringBuilder sb = new StringBuilder();

    public void emit(String instruction) {
        sb.append("  ").append(instruction).append("\n");
    }

    public void emitLabel(String label) {
        sb.append(label).append(":\n");
    }

    public void emitLdc(int value) {
        if (value >= -1 && value <= 5) emit("iconst_" + value);
        else if (value >= -128 && value <= 127) emit("bipush " + value);
        else if (value >= -32768 && value <= 32767) emit("sipush " + value);
        else emit("ldc " + value);
    }

    public void emitLdc2_w(double value) {
        emit("ldc2_w " + value);
    }

    public String getCode() { return sb.toString(); }
    public void clear() { sb.setLength(0); }
}