package codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EntryPointGenerator {

    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "^\\s*\\.method\\s+public\\s+static\\s+(\\S+)\\(([^)]*)\\)(\\S+)\\s*$",
            Pattern.MULTILINE);

    private EntryPointGenerator() {
    }

    private static final class Routine {
        final String name;
        final String descriptor;
        final String returnDescriptor;
        final List<Character> parameters;

        Routine(
                String name,
                String descriptor,
                String returnDescriptor,
                List<Character> parameters) {
            this.name = name;
            this.descriptor = descriptor;
            this.returnDescriptor = returnDescriptor;
            this.parameters = parameters;
        }
    }

    /**
     * Reads generated YuyProgram Jasmin and creates YuyEntryPoint Jasmin.
     */
    public static String generate(String programJasmin) {
        List<Routine> routines = findRoutines(programJasmin);

        StringBuilder jasmin = new StringBuilder();

        jasmin.append(".class public YuyEntryPoint\n");
        jasmin.append(".super java/lang/Object\n\n");

        jasmin.append(".method public static main([Ljava/lang/String;)V\n");
        int maxStack = 4;

        for (Routine routine : routines) {
            int required = 4;

            for (char type : routine.parameters) {
                required += (type == 'D') ? 2 : 1;
            }

            maxStack = Math.max(maxStack, required);
        }

        jasmin.append("    .limit stack ").append(maxStack).append("\n");
        jasmin.append("    .limit locals 1\n\n");

        if (routines.isEmpty()) {
            jasmin.append("    new java/lang/IllegalArgumentException\n");
            jasmin.append("    dup\n");
            jasmin.append("    ldc \"No supported public static Yuy procedures found\"\n");
            jasmin.append("    invokespecial java/lang/IllegalArgumentException/<init>(Ljava/lang/String;)V\n");
            jasmin.append("    athrow\n");
        } else {
            for (int i = 0; i < routines.size(); i++) {
                Routine routine = routines.get(i);
                String nextLabel = "NEXT_" + i;
                String callLabel = "CALL_" + i;

                // Compare the requested routine name.
                jasmin.append("    aload_0\n");
                jasmin.append("    iconst_0\n");
                jasmin.append("    aaload\n");
                jasmin.append("    ldc \"").append(escape(routine.name)).append("\"\n");
                jasmin.append("    invokevirtual java/lang/String/equals(Ljava/lang/Object;)Z\n");
                jasmin.append("    ifne ").append(callLabel).append("\n");
                jasmin.append("    goto ").append(nextLabel).append("\n\n");

                jasmin.append(callLabel).append(":\n");

                // Check the number of command-line arguments.
                jasmin.append("    aload_0\n");
                jasmin.append("    arraylength\n");
                appendInteger(jasmin, routine.parameters.size() + 1);
                jasmin.append("    if_icmpeq ARGS_OK_").append(i).append("\n");

                jasmin.append("    new java/lang/IllegalArgumentException\n");
                jasmin.append("    dup\n");
                jasmin.append("    ldc \"Wrong number of arguments for routine ")
                        .append(escape(routine.name)).append("\"\n");
                jasmin.append("    invokespecial java/lang/IllegalArgumentException/<init>(Ljava/lang/String;)V\n");
                jasmin.append("    athrow\n");

                jasmin.append("ARGS_OK_").append(i).append(":\n");

                // Convert command-line arguments and put them on the stack.
                for (int p = 0; p < routine.parameters.size(); p++) {
                    char type = routine.parameters.get(p);

                    jasmin.append("    aload_0\n");
                    appendInteger(jasmin, p + 1);
                    jasmin.append("    aaload\n");

                    if (type == 'I') {
                        jasmin.append("    invokestatic java/lang/Integer/parseInt(Ljava/lang/String;)I\n");
                    } else if (type == 'D') {
                        jasmin.append("    invokestatic java/lang/Double/parseDouble(Ljava/lang/String;)D\n");
                    }
                }

                jasmin.append("    invokestatic YuyProgram/")
                        .append(routine.name)
                        .append(routine.descriptor)
                        .append("\n");

                // Discard the returned value if the routine is not void.
                if (!"V".equals(routine.returnDescriptor)) {
                    char returnType = routine.returnDescriptor.charAt(0);

                    if (returnType == 'J' || returnType == 'D') {
                        jasmin.append("    pop2\n");
                    } else {
                        jasmin.append("    pop\n");
                    }
                }

                jasmin.append("    return\n\n");

                jasmin.append(nextLabel).append(":\n");
            }

            // No routine name matched.
            jasmin.append("    new java/lang/IllegalArgumentException\n");
            jasmin.append("    dup\n");
            jasmin.append("    ldc \"Unknown Yuy routine name\"\n");
            jasmin.append("    invokespecial java/lang/IllegalArgumentException/<init>(Ljava/lang/String;)V\n");
            jasmin.append("    athrow\n");
        }

        jasmin.append(".end method\n");

        return jasmin.toString();
    }

    private static List<Routine> findRoutines(String source) {
        List<Routine> routines = new ArrayList<>();

        Matcher matcher = METHOD_PATTERN.matcher(source);

        while (matcher.find()) {
            String name = matcher.group(1);
            String parametersDescriptor = matcher.group(2);
            String returnDescriptor = matcher.group(3);

            // Do not include JVM special methods.
            if (name.startsWith("<")) {
                continue;
            }

            List<Character> parameters = parseParameters(parametersDescriptor);

            if (parameters == null) {
                continue;
            }

            routines.add(new Routine(
                    name,
                    "(" + parametersDescriptor + ")" + returnDescriptor,
                    returnDescriptor,
                    parameters));
        }

        return routines;
    }

    private static List<Character> parseParameters(String descriptor) {
        List<Character> result = new ArrayList<>();

        for (int i = 0; i < descriptor.length(); i++) {
            char type = descriptor.charAt(i);

            if (type == 'I' || type == 'D') {
                result.add(type);
            } else {
                // Arrays, records, strings and other types are not supported here.
                return null;
            }
        }

        return result;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void appendInteger(StringBuilder jasmin, int value) {
        if (value >= -1 && value <= 5) {
            jasmin.append("    iconst_").append(value).append("\n");
        } else if (value >= -128 && value <= 127) {
            jasmin.append("    bipush ").append(value).append("\n");
        } else if (value >= -32768 && value <= 32767) {
            jasmin.append("    sipush ").append(value).append("\n");
        } else {
            jasmin.append("    ldc ").append(value).append("\n");
        }
    }
}
