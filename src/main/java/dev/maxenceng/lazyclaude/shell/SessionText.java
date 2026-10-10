package dev.maxenceng.lazyclaude.shell;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionView;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.SessionState;
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
    private static final String NAME_GAP = "  ";
    private static final String FACT_FORMAT = "%-9s%s";
    private static final DateTimeFormatter STARTED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private SessionText() {}

    static String glyph(SessionState state) {
        return switch (state) {
            case Busy busy -> BUSY_GLYPH;
            case YourTurn yourTurn -> YOUR_TURN_GLYPH;
            case WaitingForPermission permission -> PERMISSION_GLYPH;
            case WaitingForInput input -> INPUT_GLYPH;
        };
    }

    static Color colour(SessionState state) {
        return switch (state) {
            case Busy busy -> Color.GREEN;
            case YourTurn yourTurn -> Color.CYAN;
            case WaitingForPermission permission -> Color.RED;
            case WaitingForInput input -> Color.YELLOW;
        };
    }

    static String label(SessionState state) {
        return switch (state) {
            case Busy busy -> "busy";
            case YourTurn yourTurn -> "your turn";
            case WaitingForPermission permission -> "waiting for permission";
            case WaitingForInput input -> waitingForInput(input);
        };
    }

    /** The rows of the main view for a session: its name and state, then cwd, pid, start time and, when its source says, version. */
    static List<DetailRow> facts(SessionView session) {
        List<DetailRow> rows = new ArrayList<>();
        rows.add(new DetailRow(
                session.name().value() + NAME_GAP + glyph(session.state()) + " " + label(session.state()),
                Style.EMPTY.bold()));
        rows.add(fact("cwd", session.directory().value().toString()));
        rows.add(fact("pid", Long.toString(session.pid().value())));
        rows.add(fact(
                "started",
                "%s (%s ago)".formatted(STARTED.format(session.startedAt()), session.age().label())));
        switch (session.version()) {
            case Version version -> rows.add(fact("version", version.value()));
            case NoVersion none -> {}
        }
        return rows;
    }

    private static DetailRow fact(String name, String value) {
        return new DetailRow(FACT_FORMAT.formatted(name, value), Style.EMPTY);
    }

    private static String waitingForInput(WaitingForInput input) {
        return switch (input.reason()) {
            case WaitingReason reason -> "waiting for input: " + reason.value();
            case NoWaitingReason none -> "waiting for input";
        };
    }
}
