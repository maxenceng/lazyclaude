package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;

/** The files a ticket branch changes from {@code main}. */
public record ChangedFiles(List<ChangedFile> files) implements BranchChanges {

    public ChangedFiles {
        Assert.notNull("changed files", files);
    }

    @Override
    public boolean touchesFrontend() {
        return files.stream().anyMatch(ChangedFile::underFrontend);
    }
}
