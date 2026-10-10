package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.Path;
import java.time.Instant;

import dev.maxenceng.lazyclaude.error.domain.Assert;
import dev.maxenceng.lazyclaude.session.domain.Entrypoint;
import dev.maxenceng.lazyclaude.session.domain.LiveSession;
import dev.maxenceng.lazyclaude.session.domain.NoEntrypoint;
import dev.maxenceng.lazyclaude.session.domain.OtherEntrypoint;
import dev.maxenceng.lazyclaude.session.domain.SdkCliEntrypoint;
import dev.maxenceng.lazyclaude.session.domain.SessionsUnavailableException;
import dev.maxenceng.lazyclaude.session.published.ClaudeVersion;
import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.ReportedStatus;
import dev.maxenceng.lazyclaude.session.published.SessionDirectory;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.StartedAt;
import dev.maxenceng.lazyclaude.session.published.Version;
import dev.maxenceng.lazyclaude.session.published.WaitingFor;
import dev.maxenceng.lazyclaude.session.published.WaitingReason;

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

    /** @throws dev.maxenceng.lazyclaude.error.domain.DomainException when a field the domain needs is missing or refused */
    LiveSession toLiveSession() {
        Assert.notNull("pid", pid);
        Assert.notNull("startedAt", startedAt);
        Assert.notBlank("cwd", cwd);
        return LiveSession.builder()
                .id(new SessionId(sessionId))
                .pid(new ProcessId(pid))
                .name(new SessionName(name))
                .directory(new SessionDirectory(Path.of(cwd)))
                .startedAt(new StartedAt(Instant.ofEpochMilli(startedAt)))
                .version(claudeVersion())
                .state(state())
                .entrypoint(entrypointKind())
                .build();
    }

    private ClaudeVersion claudeVersion() {
        return version == null ? new NoVersion() : new Version(version);
    }

    private SessionState state() {
        Assert.notNull("status", status);
        return SessionState.of(reported(), waitingReason());
    }

    private ReportedStatus reported() {
        return switch (status) {
            case BUSY -> ReportedStatus.BUSY;
            case SHELL -> ReportedStatus.SHELL;
            case IDLE -> ReportedStatus.IDLE;
            case WAITING -> ReportedStatus.WAITING;
            default -> throw new SessionsUnavailableException("a session reports status '%s', which lazyclaude does not know".formatted(status));
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
