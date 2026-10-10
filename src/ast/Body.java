package ast;

// Тело процедуры/функции (Body)

import java.util.List;

public class Body extends ASTNode {
    public List<ASTNode> nodes;

    public Body(List<ASTNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void print(String indent) {
        // Body just prints its contents without adding its own label
        for (ASTNode node : nodes) {
            node.print(indent);
        }
    }
}