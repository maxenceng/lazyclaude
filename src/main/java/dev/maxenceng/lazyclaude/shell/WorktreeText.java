package dev.maxenceng.lazyclaude.shell;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.HeldWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoMergedPullRequestView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnreadableWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeContentsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeStateView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeView;
import dev.maxenceng.lazyclaude.session.published.DirtyPath;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionName;

/**
 * How the shell words a ticket worktree: the row of the ticket detail, the question on the status line and the reasons it is
 * refused. The detail and the refusal read the same state and count the same things.
 */
final class WorktreeText {

    private static final String PROMPT = "remove worktree %s? y/n";
    private static final String CLEAN = "clean";
    private static final String CANNOT_REMOVE = "cannot remove: %s";
    private static final String REASON_SEPARATOR = "; ";
    private static final String PART_SEPARATOR = ", ";
    private static final String PATH_DASH = " — ";
    private static final String DIRTY = "dirty path";
    private static final String COMMIT = "commit";
    private static final String BEYOND_THE_MERGE = "%s beyond the merge";
    private static final String NOT_ON_A_REMOTE = "%s that no remote has";
    private static final String LIVE_SESSION = "live session";
    private static final String NO_MERGED_PULL_REQUEST = "no merged pull request";
    private static final String CANNOT_TELL = "cannot tell what the merge covered (no merged pull request found for the branch)";
    private static final String ONE_SESSION = "live session %s is in the worktree";
    private static final String MANY_SESSIONS = "live sessions %s are in the worktree";
    private static final String QUOTED = "\"%s\"";
    private static final String UNREADABLE = "unreadable (%s)";
    private static final String NO_WORKTREE = "the ticket has no worktree";
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
            case UnreadableWorktreeView unreadable -> failure(unreadable.reason());
            case NoWorktreeView _ -> CANNOT_REMOVE.formatted(NO_WORKTREE);
        };
    }

    static String refusal(WorktreeStateView state) {
        return CANNOT_REMOVE.formatted(
                switch (state) {
                    case WorktreeContentsView contents ->
                        Stream.of(
                                        dirty(contents, true),
                                        commits(contents, true),
                                        sessionsIn(contents))
                                .flatMap(Function.identity())
                                .collect(Collectors.joining(REASON_SEPARATOR));
                    case NoMergedPullRequestView _ -> CANNOT_TELL;
                });
    }

    static String failure(Reason reason) {
        return CANNOT_REMOVE.formatted(reason.value());
    }

    private static String state(WorktreeStateView state) {
        return switch (state) {
            case WorktreeContentsView contents -> {
                String parts = Stream.of(dirty(contents, false), commits(contents, false), liveSessions(contents))
                        .flatMap(Function.identity())
                        .collect(Collectors.joining(PART_SEPARATOR));
                yield parts.isEmpty() ? CLEAN : parts;
            }
            case NoMergedPullRequestView _ -> NO_MERGED_PULL_REQUEST;
        };
    }

    private static Stream<String> dirty(WorktreeContentsView contents, boolean listed) {
        if (contents.dirtyPaths().isEmpty()) {
            return Stream.empty();
        }
        String counted = counted(contents.dirtyPaths().size(), DIRTY);
        return Stream.of(listed
                ? counted
                        + PATH_DASH
                        + contents.dirtyPaths().stream().map(DirtyPath::value).collect(Collectors.joining(PART_SEPARATOR))
                : counted);
    }

    private static Stream<String> commits(WorktreeContentsView contents, boolean noRemoteHasThem) {
        if (contents.commitsBeyondMerge() == NONE) {
            return Stream.empty();
        }
        String beyond = BEYOND_THE_MERGE.formatted(counted(contents.commitsBeyondMerge(), COMMIT));
        return Stream.of(noRemoteHasThem ? NOT_ON_A_REMOTE.formatted(beyond) : beyond);
    }

    private static Stream<String> liveSessions(WorktreeContentsView contents) {
        return contents.sessions().isEmpty() ? Stream.empty() : Stream.of(counted(contents.sessions().size(), LIVE_SESSION));
    }

    private static Stream<String> sessionsIn(WorktreeContentsView contents) {
        if (contents.sessions().isEmpty()) {
            return Stream.empty();
        }
        String named = contents.sessions().stream()
                .map(SessionName::value)
                .map(QUOTED::formatted)
                .collect(Collectors.joining(PART_SEPARATOR));
        return Stream.of((contents.sessions().size() == ONE ? ONE_SESSION : MANY_SESSIONS).formatted(named));
    }

    private static String counted(int count, String noun) {
        return COUNTED.formatted(count, count == ONE ? noun : PLURAL.formatted(noun));
    }

    private static String relative(Path root, Path worktree) {
        return root.relativize(worktree).toString();
    }
}
