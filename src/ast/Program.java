// Корень дерева

import java.util.List;

public class Program extends ASTNode {
    public List<Declaration> declarations;

    public Program(List<Declaration> declarations) {
        this.declarations = declarations;
    }

    @Override
    public void printTree(int indent) {
        printIndent(indent);
        System.out.println("Program");
        for (Declaration decl : declarations) {
            decl.printTree(indent + 1);
        }
    }
}