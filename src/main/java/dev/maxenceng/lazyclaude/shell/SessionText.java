package dev.maxenceng.lazyclaude.shell;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.LiveSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastSessionView;
import dev.maxenceng.lazyclaude.session.published.AiTitle;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.FirstPrompt;
import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.maxenceng.lazyclaude.session.published.NoFirstPrompt;
import dev.maxenceng.lazyclaude.session.published.NoTitle;
import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.ReadableTranscript;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
import dev.maxenceng.lazyclaude.session.published.TranscriptReading;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.UnreadableTranscript;
import dev.maxenceng.lazyclaude.session.published.Version;
import dev.maxenceng.lazyclaude.session.published.WaitingForInput;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.WaitingReason;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/** How the shell words a session: for a live one a glyph per state, in its colour; for a past one the title; and the facts the main view lists. */
final class SessionText {

    private static final String BUSY_GLYPH = "∼";
    private static final String YOUR_TURN_GLYPH = "•";
    private static final String PERMISSION_GLYPH = "!";
    private static final String INPUT_GLYPH = "?";
    private static final String UNKNOWN_GLYPH = "·";
    private static final String PAST_GLYPH = "○";
    private static final String INTERRUPTED_GLYPH = "↺";
    private static final String PANE_LABEL = "pane";
    private static final String QUIT_PROMPT_ONE = "1 hosted session is busy or waiting — quit? y/n";
    private static final String QUIT_PROMPT_MANY = "%d hosted sessions are busy or waiting — quit? y/n";
    private static final String NO_SESSION_LABEL = "no session";
    private static final String NEW_SESSION_FORMAT = "n new: /ticket %s";
    private static final String STARTING_GLYPH = UNKNOWN_GLYPH;
    private static final String STARTING_LABEL = "starting";
    private static final String UNLINKED_LABEL = "unlinked";
    private static final String OLDER_GLYPH = "…";
    private static final String OLDER_FORMAT = "%d older";
    private static final String BUSY_LABEL = "busy";
    private static final String YOUR_TURN_LABEL = "your turn";
    private static final String PERMISSION_LABEL = "waiting for permission";
    private static final String INPUT_LABEL = "waiting for input";
    private static final String INPUT_REASON_FORMAT = "waiting for input: %s";
    private static final String UNKNOWN_LABEL = "status unknown to lazyclaude";
    private static final String HEADING_FORMAT = "%s  %s %s";
    private static final String STARTED_FORMAT = "%s (%s ago)";
    private static final String FACT_FORMAT = "%-9s%s";
    private static final String CWD = "cwd";
    private static final String PID = "pid";
    private static final String STARTED_FACT = "started";
    private static final String VERSION = "version";
    private static final String PROMPT_FORMAT = "› %s";
    private static final String UNREADABLE_FORMAT = "transcript unreadable: %s";
    private static final String TICKET_FORMAT = "%s  %s";
    private static final String ACTIVITY_FORMAT = "started %s · last active %s ago";
    private static final DetailRow BLANK_ROW = new DetailRow("", Style.EMPTY);
    private static final DateTimeFormatter STARTED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private SessionText() {}

    static String glyph(SessionState state) {
        return switch (state) {
            case Busy _ -> BUSY_GLYPH;
            case YourTurn _ -> YOUR_TURN_GLYPH;
            case WaitingForPermission _ -> PERMISSION_GLYPH;
            case WaitingForInput _ -> INPUT_GLYPH;
            case Unknown _ -> UNKNOWN_GLYPH;
        };
    }

    static Color colour(SessionState state) {
        return switch (state) {
            case Busy _ -> Color.GREEN;
            case YourTurn _ -> Color.CYAN;
            case WaitingForPermission _ -> Color.RED;
            case WaitingForInput _ -> Color.YELLOW;
            case Unknown _ -> Color.GRAY;
        };
    }

    static String glyph(Marker marker) {
        return switch (marker) {
            case WAITING_FOR_PERMISSION -> PERMISSION_GLYPH;
            case WAITING_FOR_INPUT -> INPUT_GLYPH;
            case YOUR_TURN -> YOUR_TURN_GLYPH;
        };
    }

    static Color colour(Marker marker) {
        return switch (marker) {
            case WAITING_FOR_PERMISSION -> Color.RED;
            case WAITING_FOR_INPUT -> Color.YELLOW;
            case YOUR_TURN -> Color.CYAN;
        };
    }

    /** The verb a linked row reads before its title; a session with no link has none. */
    static Optional<String> verb(Optional<TicketLink> link) {
        return link.map(found -> found.verb().value());
    }

    /** What a hosted session's row says at its right edge. */
    static String paneLabel() {
        return PANE_LABEL;
    }

    static String quitPrompt(long busyOrWaiting) {
        return busyOrWaiting == 1 ? QUIT_PROMPT_ONE : QUIT_PROMPT_MANY.formatted(busyOrWaiting);
    }

    static String newSessionLabel(NewSessionRow row) {
        return NEW_SESSION_FORMAT.formatted(row.verb().label());
    }

    static String startingGlyph() {
        return STARTING_GLYPH;
    }

    static String startingLabel() {
        return STARTING_LABEL;
    }

    static String noSessionLabel() {
        return NO_SESSION_LABEL;
    }

    static String unlinkedLabel() {
        return UNLINKED_LABEL;
    }

    /** A past session's glyph: a dim circle, or, for one lazyclaude's quit, a crash or a kill cut off, the resume arrow. */
    static String pastGlyph(PastSessionView session) {
        return session.interrupted() ? INTERRUPTED_GLYPH : PAST_GLYPH;
    }

    static String olderGlyph() {
        return OLDER_GLYPH;
    }

    static String olderLabel(OlderSessionsRow older) {
        return OLDER_FORMAT.formatted(older.count());
    }

    /** What a past session's row and heading say: its title, else what it was first asked, else its short id. */
    static String title(PastSessionView session) {
        return switch (session.title()) {
            case AiTitle title -> title.value();
            case FirstPrompt prompt -> prompt.value();
            case NoTitle _ -> session.id().shortValue();
        };
    }

    /** A title that stands in for a missing one is dimmed, so it does not read as something the session said. */
    static boolean titleIsStandIn(PastSessionView session) {
        return session.title() instanceof NoTitle;
    }

    static String label(SessionState state) {
        return switch (state) {
            case Busy _ -> BUSY_LABEL;
            case YourTurn _ -> YOUR_TURN_LABEL;
            case WaitingForPermission _ -> PERMISSION_LABEL;
            case WaitingForInput input -> waitingForInput(input);
            case Unknown _ -> UNKNOWN_LABEL;
        };
    }

    /** The rows of the main view for a session: its name and state, then cwd, pid, start time and, when its source says, version. */
    static List<DetailRow> facts(LiveSessionView session) {
        Stream<DetailRow> version = switch (session.version()) {
            case Version known -> Stream.of(fact(VERSION, known.value()));
            case NoVersion _ -> Stream.empty();
        };
        return Stream.concat(
                        Stream.of(
                                new DetailRow(
                                        HEADING_FORMAT.formatted(
                                                session.name().value(), glyph(session.state()), label(session.state())),
                                        Style.EMPTY.bold()),
                                fact(CWD, session.directory().toString()),
                                fact(PID, Long.toString(session.pid().value())),
                                fact(
                                        STARTED_FACT,
                                        STARTED_FORMAT.formatted(
                                                STARTED.format(session.startedAt()), session.age().label()))),
                        version)
                .toList();
    }

    /**
     * The rows of the main view for a past session: its title, what it was first asked behind {@code ›} when the title is not that
     * prompt itself, and why the transcript cannot be read now when it cannot; then each ticket it is linked to, its directory,
     * and when it ran.
     */
    static List<DetailRow> card(PastSessionView session) {
        return Stream.of(
                        cardHead(session),
                        Stream.of(BLANK_ROW),
                        session.links().stream().map(SessionText::ticketLine),
                        Stream.of(
                                new DetailRow(session.directory().toString(), Style.EMPTY),
                                new DetailRow(
                                        ACTIVITY_FORMAT.formatted(
                                                STARTED.format(session.startedAt()), session.age().label()),
                                        Style.EMPTY)))
                .flatMap(Function.identity())
                .toList();
    }

    private static Stream<DetailRow> cardHead(PastSessionView session) {
        Style heading = titleIsStandIn(session) ? Style.EMPTY.bold().dim() : Style.EMPTY.bold();
        DetailRow title = new DetailRow(title(session), heading);
        return Stream.of(Stream.of(title), openingLine(session), readingLine(session.reading()))
                .flatMap(Function.identity());
    }

    /** The opening prompt is shown only under a title Claude Code wrote; a title that is the prompt already says it. */
    private static Stream<DetailRow> openingLine(PastSessionView session) {
        return switch (session.opening()) {
            case FirstPrompt prompt when session.title() instanceof AiTitle ->
                Stream.of(new DetailRow(PROMPT_FORMAT.formatted(prompt.value()), Style.EMPTY));
            case FirstPrompt _, NoFirstPrompt _ -> Stream.empty();
        };
    }

    private static Stream<DetailRow> readingLine(TranscriptReading reading) {
        return switch (reading) {
            case ReadableTranscript _ -> Stream.empty();
            case UnreadableTranscript unreadable ->
                Stream.of(new DetailRow(UNREADABLE_FORMAT.formatted(unreadable.reason().value()), Style.EMPTY.dim()));
        };
    }

    private static DetailRow ticketLine(TicketLink link) {
        return new DetailRow(TICKET_FORMAT.formatted(link.ticket().value(), link.verb().value()), Style.EMPTY);
    }

    private static DetailRow fact(String name, String value) {
        return new DetailRow(FACT_FORMAT.formatted(name, value), Style.EMPTY);
    }

    private static String waitingForInput(WaitingForInput input) {
        return switch (input.reason()) {
            case WaitingReason reason -> INPUT_REASON_FORMAT.formatted(reason.value());
            case NoWaitingReason _ -> INPUT_LABEL;
        };
    }
}
