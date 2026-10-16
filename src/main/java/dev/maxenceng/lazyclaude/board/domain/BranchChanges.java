package dev.maxenceng.lazyclaude.board.domain;

/** What a ticket branch changes from {@code main}: its files, or nothing known when git cannot say. */
public sealed interface BranchChanges permits ChangedFiles, UnreadChanges {

    boolean touchesFrontend();
}
