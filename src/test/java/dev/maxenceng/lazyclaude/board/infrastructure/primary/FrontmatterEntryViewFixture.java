package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.board.published.FrontmatterKey;
import dev.maxenceng.lazyclaude.board.published.FrontmatterValue;

public final class FrontmatterEntryViewFixture {

    private FrontmatterEntryViewFixture() {}

    public static FrontmatterEntryView entryView(String key, String value) {
        return FrontmatterEntryView.builder()
                .key(new FrontmatterKey(key))
                .value(new FrontmatterValue(value))
                .build();
    }

    public static FrontmatterEntryView createdEntryView() {
        return entryView("created", "2026-10-10");
    }

    public static List<FrontmatterEntryView> frontmatterViews() {
        return List.of();
    }

    public static List<FrontmatterEntryView> datedFrontmatterViews() {
        return List.of(createdEntryView(), entryView("blocked_by", "[PROJ-8, PROJ-9]"));
    }
}
