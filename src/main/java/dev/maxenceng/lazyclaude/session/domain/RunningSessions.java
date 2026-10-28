package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * One read of the hosted sessions of {@code project}: where each stands by its registry record. A change to the transcripts
 * reuses it rather than reading the records again.
 */
public record RunningSessions(Project project, List<OpenHostedSession> hosted) {

    public RunningSessions {
        Assert.notNull("project", project);
        Assert.notNull("hosted sessions", hosted);
    }

    /** The id of every hosted session, which hides its past row. */
    public List<SessionId> ids() {
        return hosted.stream().map(open -> open.session().id()).toList();
    }

    public static RunningSessionsBuilder builder() {
        return new RunningSessionsBuilder();
    }

    public static final class RunningSessionsBuilder {
        private Project project;
        private List<OpenHostedSession> hosted;

        private RunningSessionsBuilder() {}

        public RunningSessionsBuilder project(Project project) {
            this.project = project;
            return this;
        }

        public RunningSessionsBuilder hosted(List<OpenHostedSession> hosted) {
            this.hosted = hosted;
            return this;
        }

        public RunningSessions build() {
            return new RunningSessions(project, hosted);
        }
    }
}
