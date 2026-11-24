### 1. Build the compiler

**Linux/macOS**

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
```

**Windows (PowerShell)**

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
```

### 2. Point at Jasmin (absolute path required)

**Linux/macOS**

```bash
export JASMIN_JAR=$(realpath ./tools/jasmin.jar)
```

**Windows (PowerShell)**

```powershell
$env:JASMIN_JAR = (Resolve-Path .\tools\jasmin.jar).Path
```

## 3. Run the comliper

```bash
# Print AST
java -cp out Main tests/testN.yuy

# Emit Jasmin code
java -cp out Main --jasmin tests/testN.yuy output/YuyProgram.j

# Compile .yuy to JVM bytecode (needs JASMIN_JAR)
java -cp out Main --compile tests/testN.yuy output

# Compile and run a routine (needs JASMIN_JAR)
java -cp out Main --run tests/testN.yuy ROUTINE_NAME
```