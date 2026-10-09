import java.util.List;

public class RecordType extends UserType {
    public List<VariableDeclaration> members;

    public RecordType(List<VariableDeclaration> members) { this.members = members; }

    @Override
    public void printTree(int indent) {
        printIndent(indent);
        System.out.println("RecordType");
        for (VariableDeclaration member : members) {
            member.printTree(indent + 1);
        }
    }
}