package codegen;

import java.util.Objects;

public final class LoopContext {
    private final String breakLabel;
    private final String continueLabel;

    public LoopContext(String breakLabel, String continueLabel) {
        this.breakLabel = Objects.requireNonNull(breakLabel, "breakLabel");
        this.continueLabel = Objects.requireNonNull(continueLabel, "continueLabel");
    }

    public String getBreakLabel() {
        return breakLabel;
    }

    public String getContinueLabel() {
        return continueLabel;
    }
}
