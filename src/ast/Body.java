// Тело процедуры/функции (Body)
public class Body extends ASTNode {
    public List<SimpleDeclaration> declarations;
    public List<Statement> statements;

    public Body(List<SimpleDeclaration> declarations, List<Statement> statements) {
        this.declarations = declarations;
        this.statements = statements;
    }

    @Override
    public void printTree(int indent) {
        for (SimpleDeclaration decl : declarations) {
            decl.printTree(indent);
        }
        for (Statement stmt : statements) {
            stmt.printTree(indent);
        }
    }
}