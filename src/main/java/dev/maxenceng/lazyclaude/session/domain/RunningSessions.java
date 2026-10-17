package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * One read of the live sessions: the id of every session running on the machine, which hides its past row, and the live
 * sessions of {@code project}, and those of every other place. A change to the transcripts reuses it rather than reading the registry again.
 */
public record RunningSessions(
        Project project, List<SessionId> ids, List<LiveSession> live, List<ElsewhereGroup> elsewhere) {

    public RunningSessions {
        Assert.notNull("project", project);
        Assert.notNull("running ids", ids);
        Assert.notNull("live sessions", live);
        Assert.notNull("elsewhere", elsewhere);
    }

    public static RunningSessionsBuilder builder() {
        return new RunningSessionsBuilder();
    }

    public static final class RunningSessionsBuilder {
        private Project project;
        private List<SessionId> ids;
        private List<LiveSession> live;
        private List<ElsewhereGroup> elsewhere = List.of();

        private RunningSessionsBuilder() {}

        public RunningSessionsBuilder project(Project project) {
            this.project = project;
            return this;
        }

        public RunningSessionsBuilder ids(List<SessionId> ids) {
            this.ids = ids;
            return this;
        }

        public RunningSessionsBuilder live(List<LiveSession> live) {
            this.live = live;
            return this;
        }

        public RunningSessionsBuilder elsewhere(List<ElsewhereGroup> elsewhere) {
            this.elsewhere = elsewhere;
            return this;
        }

        public RunningSessions build() {
            return new RunningSessions(project, ids, live, elsewhere);
        }
    }
}
