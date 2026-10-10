import java.util.List;

public class RecordType extends UserType {
    public List<VariableDeclaration> members;

    public RecordType(List<VariableDeclaration> members) { this.members = members; }

    @Override
    public void print(String indent) {
        System.out.println(indent + "RecordType");
        for (VariableDeclaration member : members) {
            member.print(indent + "  ");
        }
    }
}