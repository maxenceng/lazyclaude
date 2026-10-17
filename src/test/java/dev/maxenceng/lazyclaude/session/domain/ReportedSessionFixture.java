package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.earlierStartedAt;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.laterStartedAt;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.startedAt;
import static dev.maxenceng.lazyclaude.session.published.ClaudeVersionFixture.version;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.thirdProcessId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.thirdSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.secondSessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.sessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.thirdSessionName;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;

import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.WaitingFor;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

public final class ReportedSessionFixture {

    private ReportedSessionFixture() {}

    public static ReportedSession.ReportedSessionBuilder reportedSessionBuilder() {
        return ReportedSession.builder()
                .id(sessionId())
                .pid(processId())
                .name(sessionName())
                .directory(projectRootDirectory())
                .startedAt(startedAt())
                .version(version())
                .status(ReportedStatus.BUSY)
                .waitingFor(new NoWaitingReason())
                .entrypoint(new OtherEntrypoint());
    }

    public static ReportedSession reportedSession() {
        return reportedSessionBuilder().build();
    }

    public static ReportedSession.ReportedSessionBuilder newestReportedSessionBuilder() {
        return reportedSessionBuilder()
                .id(secondSessionId())
                .pid(secondProcessId())
                .name(secondSessionName())
                .startedAt(laterStartedAt());
    }

    public static ReportedSession.ReportedSessionBuilder oldestReportedSessionBuilder() {
        return reportedSessionBuilder()
                .id(thirdSessionId())
                .pid(thirdProcessId())
                .name(thirdSessionName())
                .startedAt(earlierStartedAt())
                .status(ReportedStatus.IDLE);
    }

    public static ReportedSession newestReportedSession() {
        return newestReportedSessionBuilder().build();
    }

    public static ReportedSession oldestReportedSession() {
        return oldestReportedSessionBuilder().build();
    }

    public static ReportedSession reportedIn(WorkingDirectory directory) {
        return reportedSessionBuilder().directory(directory).build();
    }

    public static ReportedSession reportedWith(ReportedStatus status) {
        return reportedSessionBuilder().status(status).build();
    }

    public static ReportedSession reportedWaitingFor(WaitingFor waitingFor) {
        return reportedSessionBuilder().status(ReportedStatus.WAITING).waitingFor(waitingFor).build();
    }

    public static ReportedSession sdkCliSession() {
        return reportedSessionBuilder().entrypoint(new SdkCliEntrypoint()).build();
    }

    public static ReportedSession sessionWithoutEntrypoint() {
        return reportedSessionBuilder().entrypoint(new NoEntrypoint()).build();
    }
}
