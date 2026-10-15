package dev.maxenceng.lazyclaude.board.domain;

import java.nio.file.Path;
import java.util.List;

public final class BranchChangesFixture {

    private BranchChangesFixture() {}

    public static ChangedFiles frontendChanges() {
        return new ChangedFiles(List.of(new ChangedFile(Path.of("src/App.java")), new ChangedFile(Path.of("frontend/src/App.tsx"))));
    }

    public static ChangedFiles backendChanges() {
        return new ChangedFiles(List.of(new ChangedFile(Path.of("src/App.java")), new ChangedFile(Path.of("docs/frontend-notes.md"))));
    }

    public static ChangedFiles noChanges() {
        return new ChangedFiles(List.of());
    }
}
