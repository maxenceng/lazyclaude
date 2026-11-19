package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.DirtyPath;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionName;

/** How a refused or failed removal of a ticket worktree is worded, once for every frontend that says it; the views hand the words out. */
final class WorktreeWording {

    private static final String CANNOT_REMOVE = "cannot remove: %s";
    private static final String REASON_SEPARATOR = "; ";
    private static final String PART_SEPARATOR = ", ";
    private static final String PATH_DASH = " — ";
    private static final String DIRTY = "dirty path";
    private static final String COMMIT = "commit";
    private static final String BEYOND_THE_MERGE = "%s beyond the merge that no remote has";
    private static final String CANNOT_TELL = "cannot tell what the merge covered (no merged pull request found for the branch)";
    private static final String ONE_SESSION = "live session %s is in the worktree";
    private static final String MANY_SESSIONS = "live sessions %s are in the worktree";
    private static final String LIVE_SESSION = "live session";
    private static final String QUOTED = "\"%s\"";
    private static final String COUNTED = "%d %s";
    private static final String PLURAL = "%ss";
    private static final String CLEAN = "clean";
    private static final String NO_MERGED_PULL_REQUEST = "no merged pull request";
    private static final String UNREADABLE = "unreadable (%s)";
    private static final String SUMMARY_BEYOND_THE_MERGE = "%s beyond the merge";
    private static final int ONE = 1;
    private static final int NONE = 0;

    private WorktreeWording() {}

    /** Why a worktree holding {@code state} is not removed. */
    static Reason refusal(WorktreeStateView state) {
        return new Reason(CANNOT_REMOVE.formatted(
                switch (state) {
                    case WorktreeContentsView contents ->
                        Stream.of(dirty(contents), commits(contents), sessionsIn(contents))
                                .flatMap(Function.identity())
                                .collect(Collectors.joining(REASON_SEPARATOR));
                    case NoMergedPullRequestView _ -> CANNOT_TELL;
                }));
    }

    /** What stands in a worktree in {@code state}, as the ticket detail counts it. */
    static Reason summary(WorktreeStateView state) {
        return new Reason(
                switch (state) {
                    case WorktreeContentsView contents -> {
                        String parts = Stream.of(dirtyCount(contents), commitCount(contents), liveSessionCount(contents))
                                .flatMap(Function.identity())
                                .collect(Collectors.joining(PART_SEPARATOR));
                        yield parts.isEmpty() ? CLEAN : parts;
                    }
                    case NoMergedPullRequestView _ -> NO_MERGED_PULL_REQUEST;
                });
    }

    /** What stands in a worktree git could not read: why, as the ticket detail says it. */
    static Reason unreadable(Reason reason) {
        return new Reason(UNREADABLE.formatted(reason.value()));
    }

    /** The line saying a worktree in {@code state} is held back, with what stands in it. */
    static Reason heldBack(WorktreeStateView state) {
        return new Reason(CANNOT_REMOVE.formatted(summary(state).value()));
    }

    /** The line saying a worktree git could not read is held back, with why. */
    static Reason heldBackUnreadable(Reason reason) {
        return new Reason(CANNOT_REMOVE.formatted(unreadable(reason).value()));
    }

    /** Why a removal, or the read before it, failed in git. */
    static Reason failure(Reason reason) {
        return new Reason(CANNOT_REMOVE.formatted(reason.value()));
    }

    private static Stream<String> dirty(WorktreeContentsView contents) {
        if (contents.dirtyPaths().isEmpty()) {
            return Stream.empty();
        }
        return Stream.of(counted(contents.dirtyPaths().size(), DIRTY)
                + PATH_DASH
                + contents.dirtyPaths().stream().map(DirtyPath::value).collect(Collectors.joining(PART_SEPARATOR)));
    }

    private static Stream<String> commits(WorktreeContentsView contents) {
        if (contents.commitsBeyondMerge() == NONE) {
            return Stream.empty();
        }
        return Stream.of(BEYOND_THE_MERGE.formatted(counted(contents.commitsBeyondMerge(), COMMIT)));
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

    private static Stream<String> dirtyCount(WorktreeContentsView contents) {
        return contents.dirtyPaths().isEmpty() ? Stream.empty() : Stream.of(counted(contents.dirtyPaths().size(), DIRTY));
    }

    private static Stream<String> commitCount(WorktreeContentsView contents) {
        if (contents.commitsBeyondMerge() == NONE) {
            return Stream.empty();
        }
        return Stream.of(SUMMARY_BEYOND_THE_MERGE.formatted(counted(contents.commitsBeyondMerge(), COMMIT)));
    }

    private static Stream<String> liveSessionCount(WorktreeContentsView contents) {
        return contents.sessions().isEmpty() ? Stream.empty() : Stream.of(counted(contents.sessions().size(), LIVE_SESSION));
    }

    private static String counted(int count, String noun) {
        return COUNTED.formatted(count, count == ONE ? noun : PLURAL.formatted(noun));
    }
}
