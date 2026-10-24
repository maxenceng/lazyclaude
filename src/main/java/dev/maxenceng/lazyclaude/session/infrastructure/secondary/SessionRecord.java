package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.Path;
import java.time.Instant;

import dev.maxenceng.lazyclaude.session.domain.Entrypoint;
import dev.maxenceng.lazyclaude.session.domain.NoEntrypoint;
import dev.maxenceng.lazyclaude.session.domain.OtherEntrypoint;
import dev.maxenceng.lazyclaude.session.domain.ReportedSession;
import dev.maxenceng.lazyclaude.session.domain.ReportedStatus;
import dev.maxenceng.lazyclaude.session.domain.SdkCliEntrypoint;
import dev.maxenceng.lazyclaude.session.domain.StartedAt;
import dev.maxenceng.lazyclaude.session.published.ClaudeVersion;
import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.Version;
import dev.maxenceng.lazyclaude.session.published.WaitingFor;
import dev.maxenceng.lazyclaude.session.published.WaitingReason;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/**
 * One session as Claude Code writes it, in a registry record or in {@code claude agents --json}: every
 * field is as the JSON has it, null where the JSON lacks it. The two sources share their fields; only the
 * registry has {@code procStart}, {@code pidDomain}, {@code version} and {@code entrypoint}.
 */
record SessionRecord(
        Long pid,
        String sessionId,
        String cwd,
        Long startedAt,
        String procStart,
        String version,
        String entrypoint,
        String pidDomain,
        String name,
        String status,
        String waitingFor) {

    private static final String BUSY = "busy";
    private static final String SHELL = "shell";
    private static final String IDLE = "idle";
    private static final String WAITING = "waiting";
    private static final String SDK_CLI = "sdk-cli";
    private static final String PID = "pid";
    private static final String SESSION_ID = "sessionId";
    private static final String CWD = "cwd";
    private static final String STARTED_AT = "startedAt";
    private static final String NAME = "name";
    private static final String VERSION = "version";

    /** The fields a session cannot be read without that the JSON lacks; a version is optional, but one written blank is not a version. */
    MissingFields missingFields() {
        MissingFields required = new MissingFields()
                .absent(PID, pid)
                .blank(SESSION_ID, sessionId)
                .blank(CWD, cwd)
                .absent(STARTED_AT, startedAt)
                .blank(NAME, name);
        return version == null ? required : required.blank(VERSION, version);
    }

    /** Only once {@link #missingFields()} names none. @throws java.nio.file.InvalidPathException when {@code cwd} is not a path */
    ReportedSession toReportedSession() {
        return ReportedSession.builder()
                .id(new SessionId(sessionId))
                .pid(new ProcessId(pid))
                .name(new SessionName(name))
                .directory(new WorkingDirectory(Path.of(cwd)))
                .startedAt(new StartedAt(Instant.ofEpochMilli(startedAt)))
                .version(claudeVersion())
                .status(reported())
                .waitingFor(waitingReason())
                .entrypoint(entrypointKind())
                .build();
    }

    private ClaudeVersion claudeVersion() {
        return version == null ? new NoVersion() : new Version(version);
    }

    private ReportedStatus reported() {
        if (status == null) {
            return ReportedStatus.UNKNOWN;
        }
        return switch (status) {
            case BUSY -> ReportedStatus.BUSY;
            case SHELL -> ReportedStatus.SHELL;
            case IDLE -> ReportedStatus.IDLE;
            case WAITING -> ReportedStatus.WAITING;
            default -> ReportedStatus.UNKNOWN;
        };
    }

    private WaitingFor waitingReason() {
        return waitingFor == null || waitingFor.isBlank() ? new NoWaitingReason() : new WaitingReason(waitingFor);
    }

    private Entrypoint entrypointKind() {
        if (entrypoint == null) {
            return new NoEntrypoint();
        }
        return entrypoint.equals(SDK_CLI) ? new SdkCliEntrypoint() : new OtherEntrypoint();
    }
}
