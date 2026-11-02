package ast.types;

import java.util.List;

import ast.declarations.VariableDeclaration;

public class RecordType extends UserType {
    public List<VariableDeclaration> members;

    public RecordType(List<VariableDeclaration> members) {
        this.members = members;
    }

    @Override
    public String toString() {
        return "record";
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "RecordType");
        for (VariableDeclaration member : members) {
            member.print(indent + "  ");
        }
    }
}