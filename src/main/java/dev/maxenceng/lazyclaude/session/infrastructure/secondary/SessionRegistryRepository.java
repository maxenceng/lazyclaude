package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.ReportedSession;
import dev.maxenceng.lazyclaude.session.domain.SessionRecordUnreadableException;
import dev.maxenceng.lazyclaude.session.published.ProcessId;

/**
 * Reads Claude Code's registry record of one process, {@code ~/.claude/sessions/<pid>.json}. The registry is internal and
 * undocumented, so a record that does not parse or lacks what a session needs is refused with the reason, for the session it
 * belongs to alone; no record at all, or one with no status yet, is not a failure, since a {@code claude} is not up until it reports one.
 */
public final class SessionRegistryRepository implements LiveSessionPort {

    private static final String RECORD_FILE = "%d.json";

    private final Path sessions;

    public SessionRegistryRepository(Path sessions) {
        this.sessions = sessions;
    }

    @Override
    public Optional<ReportedSession> recordOf(ProcessId pid) {
        String name = RECORD_FILE.formatted(pid.value());
        Path file = sessions.resolve(name);
        try {
            return SessionRecords.session(SessionRecords.record(Files.readAllBytes(file), name), name);
        } catch (NoSuchFileException notWrittenYet) {
            return Optional.empty();
        } catch (IOException e) {
            throw new SessionRecordUnreadableException("cannot read %s: %s".formatted(file, e.getMessage()), e);
        }
    }
}
