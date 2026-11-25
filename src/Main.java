
import ast.Program;
import codegen.CodeGenerator;
import codegen.EntryPointGenerator;
import lexer.Lexer;
import parser.Parser;
import semantic.DeclarationsAnalyzer;
import semantic.StatementsAnalyzer;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            if (args.length == 3 && args[0].equals("--jasmin")) {
                Path result = generateJasmin(
                        Path.of(args[1]),
                        Path.of(args[2]));

                System.out.println("Generated Jasmin: " + result);
                return;
            }

            if (args.length == 3 && args[0].equals("--compile")) {
                Path result = compileToClass(
                        Path.of(args[1]),
                        Path.of(args[2]));

                System.out.println("Generated class: " + result);
                return;
            }

            if (args.length >= 3 && args[0].equals("--run")) {
                Path source = Path.of(args[1]);
                String routine = args[2];

                Path output = Files.createTempDirectory("yuy-program-");

                compileToClass(source, output);

                String[] routineArgs = Arrays.copyOfRange(
                        args, 3, args.length);

                runRoutine(output, routine, routineArgs);
                return;
            }

            if (args.length == 1) {
                Program program = parseAndAnalyze(
                        Files.readString(Path.of(args[0])));

                program.print("");
                return;
            }

            System.exit(2);

        } catch (Exception e) {
            System.err.println("Yuy error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Program parseAndAnalyze(String source) {
        return parseAndAnalyze(source, new DeclarationsAnalyzer());
    }

    private static Program parseAndAnalyze(
            String source,
            DeclarationsAnalyzer declarations) {

        Program program = new Parser(
                new Lexer(source).tokenize()).parseProgram();

        declarations.analyze(program);

        StatementsAnalyzer statements = new StatementsAnalyzer(
                declarations.getSymbolTable());

        statements.analyze(program);

        return program;
    }

    private static Path generateJasmin(
            Path sourceFile,
            Path outputFile) throws IOException {

        String source = Files.readString(sourceFile);

        DeclarationsAnalyzer declarations = new DeclarationsAnalyzer();

        Program program = parseAndAnalyze(source, declarations);

        Map<String, String> artifacts = new CodeGenerator().generate(
                program,
                declarations.getSymbolTable());

        Path absoluteOutput = outputFile.toAbsolutePath();
        Path outputDirectory = absoluteOutput.getParent();

        if (outputDirectory == null) {
            outputDirectory = Path.of(".").toAbsolutePath();
        }

        Files.createDirectories(outputDirectory);

        for (Map.Entry<String, String> artifact : artifacts.entrySet()) {
            Path target = outputDirectory.resolve(artifact.getKey());

            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }

            Files.writeString(target, artifact.getValue());
        }

        Path mainArtifact = outputDirectory.resolve(
                CodeGenerator.OUTPUT_CLASS + ".j");

        if (!Files.exists(mainArtifact)) {
            throw new IOException(
                    "CodeGenerator did not produce " + mainArtifact);
        }

        String mainJasmin = Files.readString(mainArtifact);

        String entryPointJasmin = EntryPointGenerator.generate(
                mainJasmin);

        Path entryPointFile = outputDirectory.resolve("YuyEntryPoint.j");

        Files.writeString(
                entryPointFile,
                entryPointJasmin);

        if (!absoluteOutput.equals(mainArtifact)) {
            Files.copy(
                    mainArtifact,
                    absoluteOutput,
                    StandardCopyOption.REPLACE_EXISTING);
        }

        return absoluteOutput;
    }

    private static Path compileToClass(
            Path sourceFile,
            Path outputDirectory) throws IOException, InterruptedException {

        Path output = outputDirectory.toAbsolutePath();

        Files.createDirectories(output);

        try (var files = Files.list(output)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();

                boolean generated = name.equals("YuyProgram.j")
                        || name.equals("YuyProgram.class")
                        || name.equals("YuyEntryPoint.j")
                        || name.equals("YuyEntryPoint.class");

                boolean record = name.startsWith("YuyRecord_")
                        && (name.endsWith(".j") || name.endsWith(".class"));

                if (generated || record) {
                    Files.deleteIfExists(file);
                }
            }
        }

        Path mainFile = output.resolve("YuyProgram.j");

        generateJasmin(sourceFile, mainFile);

        List<Path> jasminFiles;

        try (var files = Files.list(output)) {
            jasminFiles = files
                    .filter(file -> file.getFileName()
                            .toString()
                            .endsWith(".j"))
                    .toList();
        }

        for (Path jasminFile : jasminFiles) {
            assemble(jasminFile, output);
        }

        Path classFile = output.resolve("YuyProgram.class");
        Path entryPointClass = output.resolve("YuyEntryPoint.class");

        if (!Files.exists(classFile)) {
            throw new IOException(
                    "Assembler did not produce " + classFile);
        }

        if (!Files.exists(entryPointClass)) {
            throw new IOException(
                    "Assembler did not produce " + entryPointClass);
        }

        copyRuntimeClass(output);

        return classFile;
    }

    private static void assemble(
            Path jasminFile,
            Path outputDirectory) throws IOException, InterruptedException {

        Files.createDirectories(outputDirectory);

        String jasminJar = System.getenv("JASMIN_JAR");

        if (jasminJar == null || jasminJar.isBlank()) {
            jasminJar = System.getProperty("jasmin.jar");
        }

        if (jasminJar == null || jasminJar.isBlank()) {
            throw new IllegalStateException(
                    "Jasmin is not configured. Set JASMIN_JAR.");
        }

        String java = Path.of(
                System.getProperty("java.home"),
                "bin",
                isWindows() ? "java.exe" : "java").toString();

        Process process = new ProcessBuilder(
                java,
                "-cp",
                jasminJar,
                "jasmin.Main",
                "-d",
                outputDirectory.toAbsolutePath().toString(),
                jasminFile.toAbsolutePath().toString()).inheritIO().start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException(
                    "Jasmin Assembler failed with exit code "
                            + exitCode
                            + " while assembling "
                            + jasminFile);
        }
    }

    private static void copyRuntimeClass(Path outputDirectory)
            throws IOException {

        try (var input = Main.class.getResourceAsStream(
                "/YuyRuntime.class")) {

            if (input == null) {
                throw new IOException(
                        "YuyRuntime.class was not found. "
                                + "Make sure Main.java and YuyRuntime "
                                + "are compiled together.");
            }

            Files.copy(
                    input,
                    outputDirectory.resolve("YuyRuntime.class"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void runRoutine(
            Path outputDirectory,
            String routineName,
            String[] routineArgs) throws IOException, InterruptedException {

        String java = Path.of(
                System.getProperty("java.home"),
                "bin",
                isWindows() ? "java.exe" : "java").toString();

        List<String> command = new ArrayList<>();

        command.add(java);
        command.add("-cp");
        command.add(outputDirectory.toAbsolutePath().toString());
        command.add("YuyEntryPoint");
        command.add(routineName);
        command.addAll(Arrays.asList(routineArgs));

        Process process = new ProcessBuilder(command)
                .directory(outputDirectory.toFile())
                .inheritIO()
                .start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                    "Yuy program exited with code " + exitCode);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase()
                .contains("win");
    }
}

final class YuyRuntime {

    private YuyRuntime() {
    }

    public static Object allocate(String spec) {
        try {
            if (spec.startsWith("R:")) {
                String name = spec.substring(2);

                ClassLoader loader = Thread.currentThread().getContextClassLoader();

                return Class.forName(name, true, loader)
                        .getDeclaredConstructor()
                        .newInstance();
            }

            if (!spec.startsWith("A:")) {
                throw new IllegalArgumentException(
                        "Not an aggregate type: " + spec);
            }

            int open = spec.indexOf('(');
            int close = spec.lastIndexOf(')');

            int size = Integer.parseInt(
                    spec.substring(2, open));

            String componentSpec = spec.substring(open + 1, close);

            Class<?> component = componentClass(componentSpec);

            Object array = Array.newInstance(component, size);

            if (componentSpec.startsWith("A:")
                    || componentSpec.startsWith("R:")) {

                for (int i = 0; i < size; i++) {
                    Array.set(array, i, allocate(componentSpec));
                }
            }

            return array;

        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException(
                    "Cannot allocate Yuy aggregate '" + spec + "'",
                    e);
        }
    }

    private static Class<?> componentClass(String spec)
            throws ClassNotFoundException {

        return switch (spec) {
            case "I" -> int.class;
            case "D" -> double.class;

            default -> {
                if (spec.startsWith("A:")) {
                    int open = spec.indexOf('(');
                    int close = spec.lastIndexOf(')');

                    yield Array.newInstance(
                            componentClass(
                                    spec.substring(open + 1, close)),
                            0).getClass();
                }

                if (spec.startsWith("R:")) {
                    yield Class.forName(
                            spec.substring(2),
                            true,
                            Thread.currentThread().getContextClassLoader());
                }

                throw new IllegalArgumentException(
                        "Unknown aggregate component: " + spec);
            }
        };
    }
}
