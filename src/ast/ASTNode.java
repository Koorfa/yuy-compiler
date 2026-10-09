public abstract class ASTNode {
    // Метод для вывода дерева в консоль с отступами
    public abstract void printTree(int indent);
    
    // Вспомогательный метод для печати отступов
    protected void printIndent(int indent) {
        for (int i = 0; i < indent; i++) {
            System.out.print("  ");
        }
    }
}
