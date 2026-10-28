package dev.maxenceng.lazyclaude.session.domain;

import java.util.List;
import java.util.stream.Stream;

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

    /** The session every hosted one runs now and the one it was launched with, each of which hides its past row. */
    // deferred: the transcript a hosted session left by /clear has no row until its tab closes, since resuming it would reuse the tab's id — close it when the host knows a pty by something other than its launch id.
    public List<SessionId> ids() {
        return hosted.stream()
                .flatMap(open -> Stream.of(open.id(), open.session().id()))
                .distinct()
                .toList();
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
