package semantic;

import java.util.List;

import ast.*;
import ast.declarations.*;
import ast.expressions.*;
import ast.statements.*;
import ast.types.*;

public class StatementsAnalyzer {
    private final SymbolTable symbolTable;
    private final ExpressionAnalyzer expressionAnalyzer;
    private int loopDepth = 0;
    private int currentScopeLevel = 0;

    public StatementsAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.expressionAnalyzer = new ExpressionAnalyzer(symbolTable);
    }

    public void analyze(Program program) {
        for (Declaration decl : program.declarations) {
            if (decl instanceof RoutineDeclaration routine) {
                checkRoutine(routine);
            } else if (decl instanceof VariableDeclaration var) {
                if (var.initializer != null) {
                    Type initType = expressionAnalyzer.analyze(var.initializer);
                    if (var.type != null) {
                        TypeConformance.checkExpressionConformance(var.initializer, var.type, initType, symbolTable);
                    } else {
                        if (initType == null) {
                            throw new SemanticException("Cannot determine type of global variable '" + var.name + "'");
                        }
                    }
                }
            }
        }
    }

    public void checkRoutine(RoutineDeclaration routine) {
        symbolTable.enterScope();
        currentScopeLevel += 1;

        for (ParameterDeclaration param : routine.parameters) {
            Symbol paraSymbol = new Symbol(param.name, param.type, SymbolCategory.VARIABLE, null, param);
            symbolTable.add(paraSymbol);
        }

        boolean hasReturn = false;
        Type returnType = routine.returnType;

        if (routine.body != null) {
            for (ASTNode node : routine.body.nodes) {
                if (node instanceof VariableDeclaration var) {
                    var.scopeLevel = currentScopeLevel;
                    Symbol varSymbol = new Symbol(var.name, var.type, SymbolCategory.VARIABLE, null, var);
                    symbolTable.add(varSymbol);

                    if (var.initializer != null) {
                        Type initType = expressionAnalyzer.analyze(var.initializer);
                        if (var.type != null) {
                            TypeConformance.checkExpressionConformance(var.initializer, var.type, initType,
                                    symbolTable);
                        }
                    }
                } else if (node instanceof Statement stmt) {
                    if (stmt instanceof ReturnStatement retStmt) {
                        hasReturn = true;
                        checkReturnStatement(retStmt, returnType);
                    } else {
                        checkStatement(stmt);
                    }
                }
            }
        }

        if (returnType != null && !hasReturn) {
            throw new SemanticException("Function '" + routine.name + "' must return a value");
        }

        if (returnType == null && hasReturn) {
            throw new SemanticException("Routine '" + routine.name + "' cannot return a value");
        }

        currentScopeLevel -= 1;
        symbolTable.exitScope();
    }

    private void checkReturnStatement(ReturnStatement stmt, Type expecredReturnType) {
        Type actualType = expressionAnalyzer.analyze(stmt.value);
        if (expecredReturnType == null) {
            throw new SemanticException("Routine cannot return a value");
        }

        TypeConformance.checkExpressionConformance(stmt.value, expecredReturnType, actualType, symbolTable);
    }

    private void checkStatement(Statement stmt) {
        switch (stmt) {
            case Assignment assign -> checkAssignment(assign);
            case IfStatement ifStmt -> checkIf(ifStmt);
            case WhileLoop whileLoop -> checkWhile(whileLoop);
            case ForLoop forLoop -> checkFor(forLoop);
            case PrintStatement printStmt -> checkPrint(printStmt);
            case BreakStatement breakStmt -> checkBreak(breakStmt);
            case ContinueStatement continueStmt -> checkContinue(continueStmt);
            case RoutineCallStatement callStmt -> checkRoutineCallStatement(callStmt);
            default -> throw new SemanticException("Unknown statement type: " + stmt.getClass().getSimpleName());
        }
    }

    private void checkAssignment(Assignment assign) {
        Type targetType = expressionAnalyzer.analyze(assign.target);
        Type valueType = expressionAnalyzer.analyze(assign.value);

        TypeConformance.checkExpressionConformance(
                assign.value, targetType, valueType, symbolTable);
    }

    private void checkIf(IfStatement stmt) {
        Type condType = expressionAnalyzer.analyze(stmt.condition);

        if (!TypeConformance.conformsToBoolean(condType, symbolTable)) {
            throw new SemanticException("If condition must be boolean-compatible");
        }

        symbolTable.enterScope();
        currentScopeLevel += 1;
        checkBody(stmt.thenBody);
        currentScopeLevel -= 1;
        symbolTable.exitScope();

        if (stmt.elseBody != null) {
            symbolTable.enterScope();
            currentScopeLevel += 1;
            checkBody(stmt.elseBody);
            currentScopeLevel -= 1;
            symbolTable.exitScope();
        }
    }

    private void checkWhile(WhileLoop loop) {
        Type condType = expressionAnalyzer.analyze(loop.condition);

        if (!TypeConformance.conformsToBoolean(condType, symbolTable)) {
            throw new SemanticException("While condition must be boolean-compatible");
        }

        loopDepth += 1;
        symbolTable.enterScope();
        currentScopeLevel += 1;
        checkBody(loop.body);
        currentScopeLevel -= 1;
        symbolTable.exitScope();
        loopDepth -= 1;
    }

    private void checkFor(ForLoop loop) {
        Type startType = expressionAnalyzer.analyze(loop.start);

        if (loop.end != null) {
            Type endType = expressionAnalyzer.analyze(loop.end);
            if (!TypeConformance.conformsToInteger(startType, symbolTable) ||
                    !TypeConformance.conformsToInteger(endType, symbolTable)) {
                throw new SemanticException("For range requires integer");
            }
        } else {
            if (!(startType instanceof ArrayType)) {
                throw new SemanticException("For-each requires array");
            }
        }

        loopDepth += 1;
        symbolTable.enterScope();
        currentScopeLevel += 1;

        Symbol loopVar = new Symbol(
                loop.variable, PrimitiveType.INTEGER, SymbolCategory.VARIABLE, null, null);
        symbolTable.add(loopVar);

        checkBody(loop.body);

        currentScopeLevel -= 1;
        symbolTable.exitScope();
        loopDepth -= 1;
    }

    private void checkPrint(PrintStatement stmt) {
        for (Expression expr : stmt.expressions) {
            expressionAnalyzer.analyze(expr);
        }
    }

    private void checkBreak(BreakStatement stmt) {
        if (loopDepth == 0) {
            throw new SemanticException("'break' outside loop");
        }
    }

    private void checkContinue(ContinueStatement stmt) {
        if (loopDepth == 0) {
            throw new SemanticException("'continue' outside loop");
        }
    }

    private void checkRoutineCallStatement(RoutineCallStatement stmt) {
        Symbol symbol = symbolTable.lookup(stmt.name);
        if (symbol == null) {
            throw new SemanticException("Undeclared routine: " + stmt.name);
        }
        if (symbol.getCategory() != SymbolCategory.ROUTINE) {
            throw new SemanticException(stmt.name + " is not a routine (it's a " + symbol.getCategory() + ")");
        }

        List<Type> paramTypes = symbol.getParameterTypes();
        if (stmt.arguments.size() != paramTypes.size()) {
            throw new SemanticException("Routine '" + stmt.name + "' expects " + paramTypes.size()
                    + " arguments, but got " + stmt.arguments.size());
        }

        for (int i = 0; i < stmt.arguments.size(); i++) {
            Type argType = expressionAnalyzer.analyze(stmt.arguments.get(i));
            Type paramType = paramTypes.get(i);
            TypeConformance.checkExpressionConformance(stmt.arguments.get(i), paramType, argType, symbolTable);
        }
    }

    private void checkBody(Body body) {
        for (ASTNode node : body.nodes) {
            if (node instanceof VariableDeclaration var) {
                var.scopeLevel = currentScopeLevel;

                Symbol varSymbol = new Symbol(
                        var.name, var.type, SymbolCategory.VARIABLE, null, var);
                symbolTable.add(varSymbol);

                if (var.initializer != null) {
                    Type initType = expressionAnalyzer.analyze(var.initializer);
                    if (var.type != null) {
                        TypeConformance.checkExpressionConformance(
                                var.initializer, var.type, initType, symbolTable);
                    }
                }
            } else if (node instanceof Statement stmt) {
                checkStatement(stmt);
            }
        }
    }
}
