package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.LiveSession;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.SessionsUnavailableException;

/**
 * Reads Claude Code's registry, {@code ~/.claude/sessions/<pid>.json}, and keeps the records whose process
 * still runs. The registry is internal and undocumented, so a record that no longer parses makes the whole
 * registry unusable rather than being skipped: that is the signal to use {@code claude agents --json}. A
 * record that does not parse and whose file names a pid with no process is skipped instead, because a
 * dead process never rewrites it and it would pin the fallback for good.
 */
public final class SessionRegistryRepository implements LiveSessionPort {

    private static final String RECORD_GLOB = "*.json";
    private static final String RECORD_SUFFIX = ".json";

    private final Path sessions;
    private final LivenessCheck liveness;

    public SessionRegistryRepository(Path sessions, Path proc, Path machineId) {
        this.sessions = sessions;
        this.liveness = new LivenessCheck(proc, machineId);
    }

    @Override
    public List<LiveSession> liveSessions() {
        List<Path> files = recordFiles();
        String domain = liveness.thisMachinesDomain();
        List<LiveSession> live = new ArrayList<>();
        for (Path file : files) {
            Optional<SessionRecord> record = read(file);
            if (record.isPresent() && liveness.isLive(record.get(), domain)) {
                live.add(SessionRecords.session(record.get(), file.getFileName().toString()));
            }
        }
        return live;
    }

    private List<Path> recordFiles() {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(sessions, RECORD_GLOB)) {
            List<Path> found = new ArrayList<>();
            files.forEach(found::add);
            return found;
        } catch (IOException e) {
            throw unreadableRegistry(e);
        } catch (DirectoryIteratorException e) {
            throw unreadableRegistry(e.getCause());
        }
    }

    private SessionsUnavailableException unreadableRegistry(IOException cause) {
        return new SessionsUnavailableException(
                "cannot read the session registry %s: %s".formatted(sessions, cause.getMessage()), cause);
    }

    private Optional<SessionRecord> read(Path file) {
        String name = file.getFileName().toString();
        try {
            return Optional.of(SessionRecords.record(Files.readAllBytes(file), name));
        } catch (NoSuchFileException diedMeanwhile) {
            return Optional.empty();
        } catch (SessionsUnavailableException unparsed) {
            if (liveness.hasProcess(name.substring(0, name.length() - RECORD_SUFFIX.length()))) {
                throw unparsed;
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new SessionsUnavailableException("cannot read %s: %s".formatted(file, e.getMessage()), e);
        }
    }
}
