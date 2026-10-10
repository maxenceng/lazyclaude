package dev.maxenceng.lazyclaude.docs.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.docs.published.FolderName;
import dev.maxenceng.lazyclaude.error.domain.Assert;

/**
 * A project's documents grouped by folder, in display order. Its rules: only listed documents are in it
 * (markdown, outside {@code docs/backlog/}), files come before folders, and each level is sorted by name.
 */
public record DocsTree(Folder root) {

    private static final FolderName DOCS = new FolderName("docs");

    public DocsTree {
        Assert.notNull("root", root);
    }

    public static DocsTree of(List<Document> documents) {
        List<Document> listed =
                documents.stream().filter(document -> document.path().listed()).toList();
        return new DocsTree(Folder.at(DOCS, List.of(), listed));
    }
}
