
package codegen;

public final class JasminWriter {

    private final StringBuilder output = new StringBuilder();
    private String currentMethodHeader;
    private StringBuilder currentMethodBody;

    public void directive(String line) {
        if (currentMethodHeader != null) {
            throw new CodeGenException("Class-level directive inside a method: " + line);
        }
        output.append(requireNonBlank(line, "directive")).append('\n');
    }

    public void beginMethod(String signature) {
        if (currentMethodHeader != null) {
            throw new CodeGenException("Cannot begin a method before ending the previous one");
        }
        currentMethodHeader = ".method " + requireNonBlank(signature, "method signature");
        currentMethodBody = new StringBuilder();
    }

    public void instruction(String text) {
        requireMethod();
        currentMethodBody.append("    ")
                .append(requireNonBlank(text, "instruction"))
                .append('\n');
    }

    public void label(String name) {
        requireMethod();
        String value = requireNonBlank(name, "label");

        if (!value.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            throw new CodeGenException("Invalid Jasmin label: " + value);
        }

        currentMethodBody.append(value).append(":\n");
    }


    public void endMethod(int maxStack, int maxLocals) {
        requireMethod();

        if (maxStack < 0 || maxLocals < 0) {
            throw new CodeGenException("Negative JVM stack/local limit");
        }

        output.append(currentMethodHeader).append('\n');
        output.append("    .limit stack ").append(maxStack).append('\n');
        output.append("    .limit locals ").append(maxLocals).append('\n');
        output.append(currentMethodBody);
        output.append(".end method\n\n");

        currentMethodHeader = null;
        currentMethodBody = null;
    }

    public String getCode() {
        if (currentMethodHeader != null) {
            throw new CodeGenException("Cannot read Jasmin output: a method is still open");
        }
        return output.toString();
    }

    private void requireMethod() {
        if (currentMethodHeader == null) {
            throw new CodeGenException("Jasmin instruction/label without an active method");
        }
    }

    private static String requireNonBlank(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new CodeGenException("Empty " + what);
        }
        return value.strip();
    }
}

