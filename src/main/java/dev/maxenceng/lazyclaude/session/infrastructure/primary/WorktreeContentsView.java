package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.session.domain.WorktreeContents;
import dev.maxenceng.lazyclaude.session.published.DirtyPath;
import dev.maxenceng.lazyclaude.session.published.SessionName;

/** A ticket worktree judged against its merged pull request: its dirty paths, the commits beyond the merge that no remote ref has, and the live sessions in it. */
public record WorktreeContentsView(List<DirtyPath> dirtyPaths, int commitsBeyondMerge, List<SessionName> sessions)
        implements WorktreeStateView {

    static WorktreeContentsView from(WorktreeContents contents) {
        return builder()
                .dirtyPaths(contents.dirtyPaths())
                .commitsBeyondMerge(contents.commitsBeyondMerge().size())
                .sessions(contents.sessions())
                .build();
    }

    public static WorktreeContentsViewBuilder builder() {
        return new WorktreeContentsViewBuilder();
    }

    public static final class WorktreeContentsViewBuilder {
        private List<DirtyPath> dirtyPaths;
        private int commitsBeyondMerge;
        private List<SessionName> sessions;

        private WorktreeContentsViewBuilder() {}

        public WorktreeContentsViewBuilder dirtyPaths(List<DirtyPath> dirtyPaths) {
            this.dirtyPaths = dirtyPaths;
            return this;
        }

        public WorktreeContentsViewBuilder commitsBeyondMerge(int commitsBeyondMerge) {
            this.commitsBeyondMerge = commitsBeyondMerge;
            return this;
        }

        public WorktreeContentsViewBuilder sessions(List<SessionName> sessions) {
            this.sessions = sessions;
            return this;
        }

        public WorktreeContentsView build() {
            return new WorktreeContentsView(dirtyPaths, commitsBeyondMerge, sessions);
        }
    }
}
