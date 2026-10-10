package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.util.ArrayList;
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
        return List.of(
                entryView("id", "PROJ-1"),
                entryView("status", "todo"),
                entryView("context", "board"),
                entryView("type", "feature"));
    }

    public static List<FrontmatterEntryView> datedFrontmatterViews() {
        List<FrontmatterEntryView> entries = new ArrayList<>(frontmatterViews());
        entries.add(createdEntryView());
        entries.add(entryView("blocked_by", "[PROJ-8, PROJ-9]"));
        return entries;
    }
}
