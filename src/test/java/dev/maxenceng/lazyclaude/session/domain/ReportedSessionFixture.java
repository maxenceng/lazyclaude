package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.laterStartedAt;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.startedAt;
import static dev.maxenceng.lazyclaude.session.published.ClaudeVersionFixture.version;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.secondSessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.sessionName;

import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.WaitingFor;

public final class ReportedSessionFixture {

    private ReportedSessionFixture() {}

    public static ReportedSession.ReportedSessionBuilder reportedSessionBuilder() {
        return ReportedSession.builder()
                .session(sessionId())
                .name(sessionName())
                .startedAt(startedAt())
                .version(version())
                .status(ReportedStatus.BUSY)
                .waitingFor(new NoWaitingReason());
    }

    public static ReportedSession reportedSession() {
        return reportedSessionBuilder().build();
    }

    public static ReportedSession newestReportedSession() {
        return reportedSessionBuilder().session(secondSessionId()).name(secondSessionName()).startedAt(laterStartedAt()).build();
    }

    public static ReportedSession reportedWith(ReportedStatus status) {
        return reportedSessionBuilder().status(status).build();
    }

    public static ReportedSession reportedWaitingFor(WaitingFor waitingFor) {
        return reportedSessionBuilder().status(ReportedStatus.WAITING).waitingFor(waitingFor).build();
    }
}
