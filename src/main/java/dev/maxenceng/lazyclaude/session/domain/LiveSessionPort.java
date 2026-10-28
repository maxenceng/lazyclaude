package dev.maxenceng.lazyclaude.session.domain;

import java.util.Optional;

import dev.maxenceng.lazyclaude.session.published.ProcessId;

/** Where the state of a session lazyclaude hosts is read from: Claude Code's registry record for its process. */
public interface LiveSessionPort {

    /**
     * What Claude Code reports of the process {@code pid}, none while it has written no record for it, or a record with no status yet:
     * just started, or still on its trust dialog.
     *
     * @throws SessionRecordUnreadableException when the record is there and cannot be read or is of a shape not known, with why
     */
    Optional<ReportedSession> recordOf(ProcessId pid);
}
