package dev.maxenceng.lazyclaude.shell;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.FailedRemovalView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HeldWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoMergedPullRequestView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnreadableWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeContentsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeStateView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeView;

/**
 * How the shell words a ticket worktree: the row of the ticket detail, the question on the status line and the reasons it is
 * refused. The detail and the refusal read the same state and count the same things.
 */
final class WorktreeText {

    private static final String PROMPT = "remove worktree %s? y/n";
    private static final String CLEAN = "clean";
    private static final String PART_SEPARATOR = ", ";
    private static final String DIRTY = "dirty path";
    private static final String COMMIT = "commit";
    private static final String BEYOND_THE_MERGE = "%s beyond the merge";
    private static final String LIVE_SESSION = "live session";
    private static final String NO_MERGED_PULL_REQUEST = "no merged pull request";
    private static final String UNREADABLE = "unreadable (%s)";
    private static final String COUNTED = "%d %s";
    private static final String PLURAL = "%ss";
    private static final String NO_STATE = "";
    private static final int ONE = 1;
    private static final int NONE = 0;

    private WorktreeText() {}

    /** What follows {@code worktree} in the ticket detail: the path from the project root and the state; none when there is no worktree. */
    static Optional<WorktreeFacts> detail(Path root, WorktreeView view) {
        return switch (view) {
            case HeldWorktreeView held ->
                Optional.of(new WorktreeFacts(relative(root, held.path()), state(held.state())));
            case UnreadableWorktreeView unreadable ->
                Optional.of(new WorktreeFacts(UNREADABLE.formatted(unreadable.reason().value()), NO_STATE));
            case NoWorktreeView _ -> Optional.empty();
        };
    }

    static String prompt(Path root, Path worktree) {
        return PROMPT.formatted(relative(root, worktree));
    }

    /** Why the worktree is not removed, for the key bar. */
    static String refusal(WorktreeView view) {
        return switch (view) {
            case HeldWorktreeView held -> refusal(held.state());
            case UnreadableWorktreeView unreadable -> unreadable.refusal().value();
            case NoWorktreeView none -> none.refusal().value();
        };
    }

    static String refusal(WorktreeStateView state) {
        return state.refusal().value();
    }

    static String failure(FailedRemovalView failed) {
        return failed.refusal().value();
    }

    private static String state(WorktreeStateView state) {
        return switch (state) {
            case WorktreeContentsView contents -> {
                String parts = Stream.of(dirty(contents), commits(contents), liveSessions(contents))
                        .flatMap(Function.identity())
                        .collect(Collectors.joining(PART_SEPARATOR));
                yield parts.isEmpty() ? CLEAN : parts;
            }
            case NoMergedPullRequestView _ -> NO_MERGED_PULL_REQUEST;
        };
    }

    private static Stream<String> dirty(WorktreeContentsView contents) {
        return contents.dirtyPaths().isEmpty() ? Stream.empty() : Stream.of(counted(contents.dirtyPaths().size(), DIRTY));
    }

    private static Stream<String> commits(WorktreeContentsView contents) {
        if (contents.commitsBeyondMerge() == NONE) {
            return Stream.empty();
        }
        return Stream.of(BEYOND_THE_MERGE.formatted(counted(contents.commitsBeyondMerge(), COMMIT)));
    }

    private static Stream<String> liveSessions(WorktreeContentsView contents) {
        return contents.sessions().isEmpty() ? Stream.empty() : Stream.of(counted(contents.sessions().size(), LIVE_SESSION));
    }

    private static String counted(int count, String noun) {
        return COUNTED.formatted(count, count == ONE ? noun : PLURAL.formatted(noun));
    }

    private static String relative(Path root, Path worktree) {
        return root.relativize(worktree).toString();
    }
}
