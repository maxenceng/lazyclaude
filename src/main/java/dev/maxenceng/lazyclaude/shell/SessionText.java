package dev.maxenceng.lazyclaude.shell;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.LiveSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastSessionView;
import dev.maxenceng.lazyclaude.session.published.AiTitle;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.FirstPrompt;
import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.maxenceng.lazyclaude.session.published.NoTitle;
import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.StrongLink;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.Version;
import dev.maxenceng.lazyclaude.session.published.WaitingForInput;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.WaitingReason;
import dev.maxenceng.lazyclaude.session.published.WeakLink;
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
    private static final String BRANCH_SUFFIX = "(branch)";
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
    private static final String PAST_HEADING_FORMAT = "%s  %s past";
    private static final String PAST_FACT_FORMAT = "%-15s%s";
    private static final String LAST_ACTIVITY_FACT = "last activity";
    private static final String SESSION_FACT = "session";
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
    private static final DateTimeFormatter STARTED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private SessionText() {}

    static String glyph(SessionState state) {
        return switch (state) {
            case Busy busy -> BUSY_GLYPH;
            case YourTurn yourTurn -> YOUR_TURN_GLYPH;
            case WaitingForPermission permission -> PERMISSION_GLYPH;
            case WaitingForInput input -> INPUT_GLYPH;
            case Unknown unknown -> UNKNOWN_GLYPH;
        };
    }

    static Color colour(SessionState state) {
        return switch (state) {
            case Busy busy -> Color.GREEN;
            case YourTurn yourTurn -> Color.CYAN;
            case WaitingForPermission permission -> Color.RED;
            case WaitingForInput input -> Color.YELLOW;
            case Unknown unknown -> Color.GRAY;
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

    /** The verb a linked row reads before its title; a weak link, and a session with no link, have none. */
    static Optional<String> verb(Optional<TicketLink> link) {
        return link.filter(StrongLink.class::isInstance).map(strong -> ((StrongLink) strong).verb().value());
    }

    static boolean isWeak(Optional<TicketLink> link) {
        return link.filter(WeakLink.class::isInstance).isPresent();
    }

    static String branchSuffix() {
        return BRANCH_SUFFIX;
    }

    /** What a hosted session's row says where a session started elsewhere says its age. */
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

    static String pastGlyph() {
        return PAST_GLYPH;
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
            case NoTitle none -> session.id().shortValue();
        };
    }

    /** A title that stands in for a missing one is dimmed, so it does not read as something the session said. */
    static boolean titleIsStandIn(PastSessionView session) {
        return session.title() instanceof NoTitle;
    }

    static String label(SessionState state) {
        return switch (state) {
            case Busy busy -> BUSY_LABEL;
            case YourTurn yourTurn -> YOUR_TURN_LABEL;
            case WaitingForPermission permission -> PERMISSION_LABEL;
            case WaitingForInput input -> waitingForInput(input);
            case Unknown unknown -> UNKNOWN_LABEL;
        };
    }

    /** The rows of the main view for a session: its name and state, then cwd, pid, start time and, when its source says, version. */
    static List<DetailRow> facts(LiveSessionView session) {
        Stream<DetailRow> version = switch (session.version()) {
            case Version known -> Stream.of(fact(VERSION, known.value()));
            case NoVersion none -> Stream.empty();
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

    /** The rows of the main view for a past session: its title and that it is past, then cwd, last activity, start time and id. */
    static List<DetailRow> pastFacts(PastSessionView session) {
        return List.of(
                new DetailRow(PAST_HEADING_FORMAT.formatted(title(session), PAST_GLYPH), Style.EMPTY.bold()),
                pastFact(CWD, session.directory().toString()),
                pastFact(
                        LAST_ACTIVITY_FACT,
                        STARTED_FORMAT.formatted(STARTED.format(session.lastActivity()), session.age().label())),
                pastFact(STARTED_FACT, STARTED.format(session.startedAt())),
                pastFact(SESSION_FACT, session.id().value()));
    }

    private static DetailRow pastFact(String name, String value) {
        return new DetailRow(PAST_FACT_FORMAT.formatted(name, value), Style.EMPTY);
    }

    private static DetailRow fact(String name, String value) {
        return new DetailRow(FACT_FORMAT.formatted(name, value), Style.EMPTY);
    }

    private static String waitingForInput(WaitingForInput input) {
        return switch (input.reason()) {
            case WaitingReason reason -> INPUT_REASON_FORMAT.formatted(reason.value());
            case NoWaitingReason none -> INPUT_LABEL;
        };
    }
}
