package dev.maxenceng.lazyclaude.shell;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionView;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.Version;
import dev.maxenceng.lazyclaude.session.published.WaitingForInput;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.WaitingReason;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/** How the shell words a live session: one glyph per state, in its colour, and the facts the main view lists. */
final class SessionText {

    private static final String BUSY_GLYPH = "∼";
    private static final String YOUR_TURN_GLYPH = "•";
    private static final String PERMISSION_GLYPH = "!";
    private static final String INPUT_GLYPH = "?";
    private static final String UNKNOWN_GLYPH = "·";
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
    static List<DetailRow> facts(SessionView session) {
        List<DetailRow> rows = new ArrayList<>();
        rows.add(new DetailRow(
                HEADING_FORMAT.formatted(session.name().value(), glyph(session.state()), label(session.state())),
                Style.EMPTY.bold()));
        rows.add(fact(CWD, session.directory().toString()));
        rows.add(fact(PID, Long.toString(session.pid().value())));
        rows.add(fact(STARTED_FACT, STARTED_FORMAT.formatted(STARTED.format(session.startedAt()), session.age().label())));
        switch (session.version()) {
            case Version version -> rows.add(fact(VERSION, version.value()));
            case NoVersion none -> {}
        }
        return rows;
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
