package codegen;

public class LabelManager {
    private int nextId;

    public void reset() {
        nextId = 0;
    }

    public String getNewLabel(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            prefix = "label";
        }
        return prefix + "_" + nextId++;
    }
}