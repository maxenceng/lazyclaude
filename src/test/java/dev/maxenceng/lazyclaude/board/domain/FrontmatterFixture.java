package dev.maxenceng.lazyclaude.board.domain;

import static dev.maxenceng.lazyclaude.board.domain.FrontmatterEntryFixture.blockedByEntry;
import static dev.maxenceng.lazyclaude.board.domain.FrontmatterEntryFixture.contextEntry;
import static dev.maxenceng.lazyclaude.board.domain.FrontmatterEntryFixture.createdEntry;
import static dev.maxenceng.lazyclaude.board.domain.FrontmatterEntryFixture.idEntry;
import static dev.maxenceng.lazyclaude.board.domain.FrontmatterEntryFixture.statusEntry;
import static dev.maxenceng.lazyclaude.board.domain.FrontmatterEntryFixture.typeEntry;

import java.util.List;

public final class FrontmatterFixture {

    private FrontmatterFixture() {}

    public static Frontmatter frontmatter() {
        return new Frontmatter(List.of(idEntry(), statusEntry(), contextEntry(), typeEntry()));
    }

    public static Frontmatter datedFrontmatter() {
        return new Frontmatter(
                List.of(idEntry(), statusEntry(), contextEntry(), typeEntry(), createdEntry(), blockedByEntry()));
    }

    public static Frontmatter idAndStatusFrontmatter() {
        return new Frontmatter(List.of(idEntry(), statusEntry()));
    }
}
