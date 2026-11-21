package codegen;

public class LabelManager {
    private int labelCounter = 0;
    public String getNewLabel(String prefix) {
        return prefix + "_" + (labelCounter++);
    }
}