package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.DirtyPath;
import dev.maxenceng.lazyclaude.session.published.SessionName;

/**
 * A ticket worktree judged against its merged pull request: the paths git reports as dirty, the commits beyond the merged head that no
 * remote ref has, and the names of the live sessions whose directory is in it.
 */
public record WorktreeContents(List<DirtyPath> dirtyPaths, List<CommitId> commitsBeyondMerge, List<SessionName> sessions)
        implements WorktreeState {

    public WorktreeContents {
        Assert.notNull("dirty paths", dirtyPaths);
        Assert.notNull("commits beyond the merge", commitsBeyondMerge);
        Assert.notNull("sessions", sessions);
    }

    @Override
    public boolean clear() {
        return dirtyPaths.isEmpty() && commitsBeyondMerge.isEmpty() && sessions.isEmpty();
    }

    public static WorktreeContentsBuilder builder() {
        return new WorktreeContentsBuilder();
    }

    public static final class WorktreeContentsBuilder {
        private List<DirtyPath> dirtyPaths = List.of();
        private List<CommitId> commitsBeyondMerge = List.of();
        private List<SessionName> sessions = List.of();

        private WorktreeContentsBuilder() {}

        public WorktreeContentsBuilder dirtyPaths(List<DirtyPath> dirtyPaths) {
            this.dirtyPaths = dirtyPaths;
            return this;
        }

        public WorktreeContentsBuilder commitsBeyondMerge(List<CommitId> commitsBeyondMerge) {
            this.commitsBeyondMerge = commitsBeyondMerge;
            return this;
        }

        public WorktreeContentsBuilder sessions(List<SessionName> sessions) {
            this.sessions = sessions;
            return this;
        }

        public WorktreeContents build() {
            return new WorktreeContents(dirtyPaths, commitsBeyondMerge, sessions);
        }
    }
}
