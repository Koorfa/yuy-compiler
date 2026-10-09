public class FieldAccess extends ModifiablePrimary {

    private final ModifiablePrimary target;
    private final String fieldName;

    public FieldAccess(ModifiablePrimary target, String fieldName) {
        this.target = target;
        this.fieldName = fieldName;
    }

    @Override
    public void print(String indent) {
        System.out.println(indent + "FieldAccess: " + fieldName);
        target.print(indent + "    ");
    }
}