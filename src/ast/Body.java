package ast;

// Тело процедуры/функции (Body)

import java.util.List;

public class Body extends ASTNode {
    public List<SimpleDeclaration> declarations;
    public List<Statement> statements;

    public Body(List<SimpleDeclaration> declarations, List<Statement> statements) {
        this.declarations = declarations;
        this.statements = statements;
    }

    @Override
    public void print(String indent) {
        // Body just prints its contents without adding its own label
        for (SimpleDeclaration decl : declarations) {
            decl.print(indent);
        }
        for (Statement stmt : statements) {
            stmt.print(indent);
        }
    }
}