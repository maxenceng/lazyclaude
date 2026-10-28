package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Optional;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.domain.EndedAt;
import dev.maxenceng.lazyclaude.session.domain.LaunchCommand;
import dev.maxenceng.lazyclaude.session.domain.LaunchEntry;
import dev.maxenceng.lazyclaude.session.domain.NewClaude;
import dev.maxenceng.lazyclaude.session.domain.NotEnded;
import dev.maxenceng.lazyclaude.session.domain.ResumeClaude;
import dev.maxenceng.lazyclaude.session.domain.StartedAt;
import dev.maxenceng.lazyclaude.session.domain.TicketClaude;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * One line of the launch log, one launch: the session, where it ran, when it started, the kind of {@code claude} it was and,
 * for a {@code /ticket} one, its ticket and verb; and when its {@code claude} exited by itself, none otherwise. Every field is as
 * the line has it, null where the line lacks it.
 */
record LaunchLogLine(
        String session, String directory, String startedAt, String kind, String ticket, String verb, String endedAt) {

    private static final String NEW = "new";
    private static final String RESUME = "resume";
    private static final String TICKET = "ticket";
    private static final String NO_SESSION = "no session";
    private static final String NO_DIRECTORY = "no absolute directory";
    private static final String NO_START = "no start time";
    private static final String BAD_END = "an end time that is no time";
    private static final String UNKNOWN_KIND = "a kind of launch it does not know";
    private static final String NO_TICKET = "a ticket launch with no ticket";
    private static final String NO_VERB = "a ticket launch with no verb it knows";

    static LaunchLogLine of(LaunchEntry entry) {
        String ended = switch (entry.end()) {
            case EndedAt at -> at.value().toString();
            case NotEnded _ -> null;
        };
        return switch (entry.command()) {
            case NewClaude _ -> line(entry, NEW, null, null, ended);
            case ResumeClaude _ -> line(entry, RESUME, null, null, ended);
            case TicketClaude run -> line(entry, TICKET, run.ticket().value(), run.verb().label(), ended);
        };
    }

    private static LaunchLogLine line(LaunchEntry entry, String kind, String ticket, String verb, String endedAt) {
        return new LaunchLogLine(
                entry.id().value(),
                entry.directory().path().toString(),
                entry.startedAt().value().toString(),
                kind,
                ticket,
                verb,
                endedAt);
    }

    /** Why this line cannot be an entry, none when it can. */
    Optional<String> flaw() {
        if (isBlank(session)) {
            return Optional.of(NO_SESSION);
        }
        if (!isAbsolutePath(directory)) {
            return Optional.of(NO_DIRECTORY);
        }
        if (!isInstant(startedAt)) {
            return Optional.of(NO_START);
        }
        if (endedAt != null && !isInstant(endedAt)) {
            return Optional.of(BAD_END);
        }
        return commandFlaw();
    }

    private Optional<String> commandFlaw() {
        if (NEW.equals(kind) || RESUME.equals(kind)) {
            return Optional.empty();
        }
        if (!TICKET.equals(kind)) {
            return Optional.of(UNKNOWN_KIND);
        }
        if (isBlank(ticket)) {
            return Optional.of(NO_TICKET);
        }
        return verbOf(verb).isPresent() ? Optional.empty() : Optional.of(NO_VERB);
    }

    /** The entry this line holds, none for a line whose {@link #flaw()} is not none. */
    Optional<LaunchEntry> toEntry() {
        return flaw().isPresent()
                ? Optional.empty()
                : commandOf().map(command -> LaunchEntry.builder()
                        .command(command)
                        .directory(new WorkingDirectory(Path.of(directory)))
                        .startedAt(new StartedAt(Instant.parse(startedAt)))
                        .end(endedAt == null ? new NotEnded() : new EndedAt(Instant.parse(endedAt)))
                        .build());
    }

    private Optional<LaunchCommand> commandOf() {
        SessionId id = new SessionId(session);
        return switch (kind) {
            case NEW -> Optional.of(new NewClaude(id));
            case RESUME -> Optional.of(new ResumeClaude(id));
            default -> verbOf(verb).map(known -> TicketClaude.builder()
                    .session(id)
                    .ticket(new LinkedTicketId(ticket))
                    .verb(known)
                    .build());
        };
    }

    private static Optional<Verb> verbOf(String label) {
        return Arrays.stream(Verb.values()).filter(known -> known.label().equals(label)).findFirst();
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private static boolean isAbsolutePath(String text) {
        if (isBlank(text)) {
            return false;
        }
        try {
            return Path.of(text).isAbsolute();
        } catch (InvalidPathException notAPath) {
            return false;
        }
    }

    private static boolean isInstant(String text) {
        if (isBlank(text)) {
            return false;
        }
        try {
            Instant.parse(text);
            return true;
        } catch (DateTimeParseException notATime) {
            return false;
        }
    }
}
