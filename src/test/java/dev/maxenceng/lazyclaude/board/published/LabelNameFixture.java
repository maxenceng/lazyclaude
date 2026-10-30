package dev.maxenceng.lazyclaude.board.published;

import java.util.List;

public final class LabelNameFixture {

    private LabelNameFixture() {}

    public static LabelName labelName() {
        return new LabelName("bug");
    }

    public static LabelName secondLabelName() {
        return new LabelName("help wanted");
    }

    public static List<LabelName> labelNames() {
        return List.of(labelName(), secondLabelName());
    }
}
