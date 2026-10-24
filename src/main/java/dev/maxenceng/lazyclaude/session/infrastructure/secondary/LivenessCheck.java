package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import dev.maxenceng.lazyclaude.session.domain.SessionsUnavailableException;

/**
 * Says whether a registry record's process still runs. A record is live only when its {@code pidDomain}
 * is this machine's (Claude Code writes {@code linux:<machine id>:<pid namespace>}), {@code /proc/<pid>}
 * exists, and field 22 of {@code /proc/<pid>/stat} (the start time) equals the record's {@code procStart};
 * the last check is what tells a dead pid reused by another process.
 */
final class LivenessCheck {

    private static final String FIELD_GAP = "\\s+";
    private static final String DOMAIN_PREFIX = "linux:";
    private static final String SELF_NAMESPACE = "self/ns/pid";
    private static final String STAT = "stat";
    private static final int STAT_FIELDS_BEFORE_STATE = 2;
    private static final int START_TIME_FIELD = 22;

    private final Path proc;
    private final Path machineId;

    LivenessCheck(Path proc, Path machineId) {
        this.proc = proc;
        this.machineId = machineId;
    }

    /** This machine's pid domain, read afresh so a record is never judged against a stale one. */
    String thisMachinesDomain() {
        try {
            String id = Files.readString(machineId, StandardCharsets.UTF_8).strip();
            String namespace = Files.readSymbolicLink(proc.resolve(SELF_NAMESPACE)).toString();
            return "%s%s:%s".formatted(DOMAIN_PREFIX, id, namespace);
        } catch (IOException | UnsupportedOperationException e) {
            throw new SessionsUnavailableException(
                    "cannot tell which machine this is from %s and %s: %s".formatted(machineId, proc.resolve(SELF_NAMESPACE), e.getMessage()), e);
        }
    }

    boolean isLive(SessionRecord record, String domain) {
        if (!domain.equals(record.pidDomain()) || record.pid() == null || record.procStart() == null) {
            return false;
        }
        return startTimeOf(record.pid()).filter(record.procStart()::equals).isPresent();
    }

    /** Whether {@code /proc} holds a process named {@code pid}, read from a record's file name when its JSON cannot be. */
    boolean hasProcess(String pid) {
        return !pid.isBlank() && pid.chars().allMatch(Character::isDigit) && Files.isDirectory(proc.resolve(pid));
    }

    private Optional<String> startTimeOf(long pid) {
        try {
            String stat = Files.readString(proc.resolve(Long.toString(pid)).resolve(STAT), StandardCharsets.UTF_8);
            int endOfCommand = stat.lastIndexOf(')');
            String[] fields = stat.substring(endOfCommand + 1).strip().split(FIELD_GAP);
            int index = START_TIME_FIELD - STAT_FIELDS_BEFORE_STATE - 1;
            return index < fields.length ? Optional.of(fields[index]) : Optional.empty();
        } catch (IOException goneOrUnreadable) {
            return Optional.empty();
        }
    }
}
