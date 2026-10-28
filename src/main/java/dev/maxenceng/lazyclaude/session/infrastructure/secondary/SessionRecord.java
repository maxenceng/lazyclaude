package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.time.Instant;

import dev.maxenceng.lazyclaude.session.domain.ReportedSession;
import dev.maxenceng.lazyclaude.session.domain.ReportedStatus;
import dev.maxenceng.lazyclaude.session.domain.StartedAt;
import dev.maxenceng.lazyclaude.session.published.ClaudeVersion;
import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.Version;
import dev.maxenceng.lazyclaude.session.published.WaitingFor;
import dev.maxenceng.lazyclaude.session.published.WaitingReason;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.MissingFields;

/** What a hosted session's registry record holds that the domain reads: every field is as the JSON has it, null where the JSON lacks it. */
record SessionRecord(Long startedAt, String version, String name, String status, String waitingFor) {

    private static final String BUSY = "busy";
    private static final String SHELL = "shell";
    private static final String IDLE = "idle";
    private static final String WAITING = "waiting";
    private static final String STARTED_AT = "startedAt";
    private static final String NAME = "name";
    private static final String VERSION = "version";

    /** The fields a session cannot be read without that the JSON lacks; a version is optional, but one written blank is not a version. */
    MissingFields missingFields() {
        MissingFields required = new MissingFields().absent(STARTED_AT, startedAt).blank(NAME, name);
        return version == null ? required : required.blank(VERSION, version);
    }

    /** Claude Code writes the record a moment before the session's first status: until then the session is not up yet. */
    boolean hasStatus() {
        return status != null;
    }

    /** Only once {@link #missingFields()} names none. */
    ReportedSession toReportedSession() {
        return ReportedSession.builder()
                .name(new SessionName(name))
                .startedAt(new StartedAt(Instant.ofEpochMilli(startedAt)))
                .version(claudeVersion())
                .status(reported())
                .waitingFor(waitingReason())
                .build();
    }

    private ClaudeVersion claudeVersion() {
        return version == null ? new NoVersion() : new Version(version);
    }

    private ReportedStatus reported() {
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
}
