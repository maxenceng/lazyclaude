package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.board.domain.FrontmatterEntry.FrontmatterEntryBuilder;
import dev.maxenceng.lazyclaude.board.published.FrontmatterKey;
import dev.maxenceng.lazyclaude.board.published.FrontmatterValue;

public final class FrontmatterEntryFixture {

    private FrontmatterEntryFixture() {}

    public static FrontmatterEntryBuilder frontmatterEntryBuilder() {
        return entryBuilder("id", "PROJ-1");
    }

    public static FrontmatterEntry idEntry() {
        return entry("id", "PROJ-1");
    }

    public static FrontmatterEntry statusEntry() {
        return entry("status", "todo");
    }

    public static FrontmatterEntry contextEntry() {
        return entry("context", "board");
    }

    public static FrontmatterEntry typeEntry() {
        return entry("type", "feature");
    }

    public static FrontmatterEntry createdEntry() {
        return entry("created", "2026-10-10");
    }

    public static FrontmatterEntry blockedByEntry() {
        return entry("blocked_by", "[PROJ-8, PROJ-9]");
    }

    public static FrontmatterEntry entry(String key, String value) {
        return entryBuilder(key, value).build();
    }

    private static FrontmatterEntryBuilder entryBuilder(String key, String value) {
        return FrontmatterEntry.builder().key(new FrontmatterKey(key)).value(new FrontmatterValue(value));
    }
}
