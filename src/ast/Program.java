// Корень дерева

import java.util.List;

public class Program extends ASTNode {
    public List<Declaration> declarations;

    public Program(List<Declaration> declarations) {
        this.declarations = declarations;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "Program");
        for (Declaration decl : declarations) {
            decl.print(indent + "  ");
        }
    }
}