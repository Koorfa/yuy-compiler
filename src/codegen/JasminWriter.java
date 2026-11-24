package codegen;

public final class JasminWriter {
    private final StringBuilder output = new StringBuilder();
    private String methodHeader;
    private StringBuilder methodBody;

    public void directive(String line) {
        if (methodHeader != null) {
            throw new CodeGenException("Class-level directive inside a method");
        }
        output.append(requireText(line, "directive")).append('\n');
    }

    public void beginMethod(String signature) {
        if (methodHeader != null) {
            throw new CodeGenException("Previous method is not finished");
        }
        methodHeader = ".method " + requireText(signature, "method signature");
        methodBody = new StringBuilder();
    }

    public void instruction(String text) {
        requireMethod();
        methodBody.append("    ")
                .append(requireText(text, "instruction"))
                .append('\n');
    }

    public void emit(String instruction) {
        instruction(instruction);
    }

    public void label(String name) {
        requireMethod();
        String validName = requireText(name, "label");

        if (!validName.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            throw new CodeGenException("Invalid Jasmin label: " + validName);
        }

        methodBody.append(validName).append(":\n");
    }

    public void emitLabel(String label) {
        label(label);
    }

    public void emitLdc(int value) {
        if (value == -1) {
            emit("iconst_m1");
        } else if (value >= 0 && value <= 5) {
            emit("iconst_" + value);
        } else if (value >= -128 && value <= 127) {
            emit("bipush " + value);
        } else if (value >= -32768 && value <= 32767) {
            emit("sipush " + value);
        } else {
            emit("ldc " + value);
        }
    }

    public void emitLdc2_w(double value) {
        emit("ldc2_w " + value);
    }

    public void endMethod(int maxStack, int maxLocals) {
        requireMethod();

        if (maxStack < 0 || maxLocals < 0) {
            throw new CodeGenException("Negative JVM stack/local limit");
        }

        output.append(methodHeader).append('\n');
        output.append("    .limit stack ").append(maxStack).append('\n');
        output.append("    .limit locals ").append(maxLocals).append('\n');
        output.append(methodBody);
        output.append(".end method\n\n");

        methodHeader = null;
        methodBody = null;
    }

    public String getCode() {
        if (methodHeader != null) {
            throw new CodeGenException(
                    "Cannot read output before ending the method"
            );
        }
        return output.toString();
    }

    public void clear() {
        output.setLength(0);
        methodHeader = null;
        methodBody = null;
    }

    private void requireMethod() {
        if (methodHeader == null) {
            throw new CodeGenException("No active Jasmin method");
        }
    }

    private static String requireText(String text, String role) {
        if (text == null || text.isBlank()) {
            throw new CodeGenException("Empty " + role);
        }
        return text.strip();
    }
}
