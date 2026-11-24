package codegen;

import ast.ASTNode;
import ast.Body;
import ast.Program;
import ast.declarations.Declaration;
import ast.declarations.ParameterDeclaration;
import ast.declarations.RoutineDeclaration;
import ast.declarations.VariableDeclaration;
import ast.expressions.*;
import ast.statements.*;
import ast.types.ArrayType;
import ast.types.PrimitiveType;
import ast.types.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import semantic.Symbol;
import semantic.SymbolCategory;
import semantic.SymbolTable;
import semantic.TypeConformance;

/**
 * Collects semantic symbols for declarations before generating JVM code.
 * Keeps original Symbol identities used by Identifier.resolvedSymbol.
 * Does not allocate local slots: that belongs to VariableManager/LocalAllocator.
 */
public final class CodeGenPrepPass {
    private final SymbolTable symbolTable;

    private final Map<Declaration, Symbol> declarationSymbols =
            new IdentityHashMap<>();

    private final Map<ForLoop, Symbol> loopSymbols =
            new IdentityHashMap<>();

    public CodeGenPrepPass(SymbolTable symbolTable) {
        this.symbolTable = Objects.requireNonNull(symbolTable, "symbolTable");
    }

    public void prepare(Program program) {
        Objects.requireNonNull(program, "program");

        declarationSymbols.clear();
        loopSymbols.clear();

        // Find original symbols saved in expressions during semantic analysis.
        for (Declaration declaration : program.declarations) {
            if (declaration instanceof VariableDeclaration variable) {
                scanExpression(variable.initializer, new ArrayDeque<>());
            } else if (declaration instanceof RoutineDeclaration routine) {
                scanBody(routine.body, new ArrayDeque<>());
            }
        }

        // Also register declarations that are never referenced.
        for (Declaration declaration : program.declarations) {
            if (declaration instanceof VariableDeclaration variable) {
                Symbol global = symbolTable.lookupCurrentScope(variable.name);

                if (global == null || global.getDeclaration() != variable) {
                    throw new CodeGenException(
                            "Missing global symbol: " + variable.name
                    );
                }

                associate(variable, global);
                ensureType(variable, global);

            } else if (declaration instanceof RoutineDeclaration routine
                    && routine.body != null) {

                for (ParameterDeclaration param : routine.parameters) {
                    ensureParameter(param);
                }

                indexBody(routine.body);
            }
        }
    }

    public Symbol symbolFor(VariableDeclaration declaration) {
        return required(declaration);
    }

    public Symbol symbolFor(ParameterDeclaration declaration) {
        return required(declaration);
    }

    public Symbol symbolFor(ForLoop loop) {
        Symbol result = loopSymbols.get(loop);

        if (result == null) {
            throw new CodeGenException(
                    "For-loop not prepared: " + loop.variable
            );
        }

        return result;
    }

    private Symbol required(Declaration declaration) {
        Symbol result = declarationSymbols.get(declaration);

        if (result == null) {
            throw new CodeGenException(
                    "Declaration not prepared: " + declaration
            );
        }

        return result;
    }

    private void associate(Declaration declaration, Symbol symbol) {
        if (symbol.getCategory() != SymbolCategory.VARIABLE
                || symbol.getDeclaration() != declaration) {
            throw new CodeGenException(
                    "Incorrect symbol for declaration: " + symbol.getName()
            );
        }

        Symbol old = declarationSymbols.putIfAbsent(declaration, symbol);

        if (old != null && old != symbol) {
            throw new CodeGenException(
                    "Different semantic symbols for: " + symbol.getName()
            );
        }
    }

    private void registerReference(Identifier id, Deque<ForLoop> activeLoops) {
        Symbol symbol = id.resolvedSymbol;

        if (symbol == null || symbol.getCategory() != SymbolCategory.VARIABLE) {
            return;
        }

        Declaration declaration = symbol.getDeclaration();

        if (declaration instanceof VariableDeclaration
                || declaration instanceof ParameterDeclaration) {
            associate(declaration, symbol);

        } else if (declaration == null) {
            // Loop variables are implicitly declared.
            for (ForLoop loop : activeLoops) {
                if (loop.variable.equals(symbol.getName())) {
                    Symbol existing = loopSymbols.putIfAbsent(loop, symbol);

                    if (existing != null && existing != symbol) {
                        throw new CodeGenException(
                                "Different symbols for loop variable: "
                                        + loop.variable
                        );
                    }
                    break;
                }
            }
        }
    }

    private void scanBody(Body body, Deque<ForLoop> activeLoops) {
        if (body == null) {
            return;
        }

        for (ASTNode node : body.nodes) {
            if (node instanceof VariableDeclaration variable) {
                scanExpression(variable.initializer, activeLoops);

            } else if (node instanceof Assignment assignment) {
                scanExpression(assignment.target, activeLoops);
                scanExpression(assignment.value, activeLoops);

            } else if (node instanceof IfStatement conditional) {
                scanExpression(conditional.condition, activeLoops);
                scanBody(conditional.thenBody, activeLoops);
                scanBody(conditional.elseBody, activeLoops);

            } else if (node instanceof WhileLoop loop) {
                scanExpression(loop.condition, activeLoops);
                scanBody(loop.body, activeLoops);

            } else if (node instanceof ForLoop loop) {
                scanExpression(loop.start, activeLoops);
                scanExpression(loop.end, activeLoops);

                activeLoops.push(loop);
                try {
                    scanBody(loop.body, activeLoops);
                } finally {
                    activeLoops.pop();
                }

            } else if (node instanceof PrintStatement print) {
                for (Expression expression : print.expressions) {
                    scanExpression(expression, activeLoops);
                }

            } else if (node instanceof ReturnStatement returned) {
                scanExpression(returned.value, activeLoops);

            } else if (node instanceof RoutineCallStatement call) {
                for (Expression argument : call.arguments) {
                    scanExpression(argument, activeLoops);
                }
            }
        }
    }

    private void scanExpression(Expression expression,
                                Deque<ForLoop> activeLoops) {
        if (expression == null) {
            return;
        }

        if (expression instanceof Identifier id) {
            registerReference(id, activeLoops);

        } else if (expression instanceof BinaryExpression binary) {
            scanExpression(binary.getLeft(), activeLoops);
            scanExpression(binary.getRight(), activeLoops);

        } else if (expression instanceof UnaryExpression unary) {
            scanExpression(unary.getOperand(), activeLoops);

        } else if (expression instanceof ArrayAccess array) {
            scanExpression(array.getArray(), activeLoops);
            scanExpression(array.getIndex(), activeLoops);

        } else if (expression instanceof FieldAccess field) {
            scanExpression(field.getTarget(), activeLoops);

        } else if (expression instanceof RoutineCallExpression call) {
            for (Expression argument : call.getArguments()) {
                scanExpression(argument, activeLoops);
            }
        }
    }

    private void ensureParameter(ParameterDeclaration param) {
        declarationSymbols.computeIfAbsent(param, ignored ->
                new Symbol(
                        param.name,
                        param.type,
                        SymbolCategory.VARIABLE,
                        null,
                        param
                )
        );
    }

    private void ensureType(VariableDeclaration variable, Symbol symbol) {
        if (symbol.getType() != null) {
            return;
        }

        Type inferred = variable.type != null
                ? variable.type
                : variable.initializer == null
                  ? null
                  : variable.initializer.resolvedType;

        if (inferred == null) {
            throw new CodeGenException(
                    "Unknown type of variable: " + variable.name
            );
        }

        symbol.setType(inferred);
    }

    private void indexBody(Body body) {
        if (body == null) {
            return;
        }

        for (ASTNode node : body.nodes) {
            if (node instanceof VariableDeclaration variable) {
                Symbol symbol = declarationSymbols.computeIfAbsent(
                        variable,
                        ignored -> new Symbol(
                                variable.name,
                                variable.type,
                                SymbolCategory.VARIABLE,
                                null,
                                variable
                        )
                );

                ensureType(variable, symbol);

            } else if (node instanceof IfStatement conditional) {
                indexBody(conditional.thenBody);
                indexBody(conditional.elseBody);

            } else if (node instanceof WhileLoop loop) {
                indexBody(loop.body);

            } else if (node instanceof ForLoop loop) {
                Type loopType = PrimitiveType.INTEGER;

                if (loop.end == null
                        && loop.start != null
                        && loop.start.resolvedType != null) {

                    Type source = TypeConformance.resolveType(
                            loop.start.resolvedType,
                            symbolTable
                    );

                    if (source instanceof ArrayType array) {
                        loopType = TypeConformance.resolveType(
                                array.elementType,
                                symbolTable
                        );
                    }
                }

                Symbol loopSymbol = loopSymbols.get(loop);

                if (loopSymbol == null) {
                    loopSymbol = new Symbol(
                            loop.variable,
                            loopType,
                            SymbolCategory.VARIABLE,
                            null,
                            null
                    );
                    loopSymbols.put(loop, loopSymbol);
                }

                loopSymbol.setType(loopType);
                indexBody(loop.body);
            }
        }
    }
}
