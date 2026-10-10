package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.LiveSessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnavailableSessionsView;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;

/**
 * The Sessions panel's rows and what the shell remembers about them: the selection, by session id, so it
 * survives the sessions being read again and does not move when a row above it appears or a state changes.
 */
final class SessionRows {

    private static final int DOWN = 1;
    private static final int UP = -1;

    private SessionsView sessions;
    private Optional<SessionId> selected;

    SessionRows(SessionsView sessions) {
        this.sessions = sessions;
        this.selected = visible().stream().findFirst().map(SessionView::id);
    }

    List<SessionView> visible() {
        return switch (sessions) {
            case LiveSessionsView live -> live.sessions();
            case UnavailableSessionsView unavailable -> List.of();
        };
    }

    /** Why there are no sessions to show, when no source answered. */
    Optional<Reason> unavailable() {
        return sessions instanceof UnavailableSessionsView unavailable ? Optional.of(unavailable.reason()) : Optional.empty();
    }

    boolean isEmpty() {
        return selected.isEmpty();
    }

    Optional<SessionView> selected() {
        List<SessionView> rows = visible();
        return selected.map(id -> rows.get(indexOf(rows, id)));
    }

    int selectedIndex() {
        List<SessionView> rows = visible();
        return selected.map(id -> indexOf(rows, id)).orElse(0);
    }

    Optional<SessionId> selectedId() {
        return selected;
    }

    void down() {
        move(DOWN);
    }

    void up() {
        move(UP);
    }

    /** Swaps in freshly read sessions; the selection stays on its id, or on the row that took its place. Says whether the selection moved. */
    boolean replace(SessionsView next) {
        Optional<SessionId> was = selected;
        int index = selectedIndex();
        sessions = next;
        List<SessionView> after = visible();
        if (after.isEmpty()) {
            selected = Optional.empty();
        } else if (!was.map(id -> contains(after, id)).orElse(false)) {
            selected = Optional.of(after.get(Math.min(index, after.size() - 1)).id());
        }
        return !was.equals(selected);
    }

    private void move(int step) {
        List<SessionView> rows = visible();
        if (rows.isEmpty()) {
            return;
        }
        int target = Math.clamp(selectedIndex() + (long) step, 0, rows.size() - 1);
        selected = Optional.of(rows.get(target).id());
    }

    private static boolean contains(List<SessionView> rows, SessionId id) {
        return rows.stream().anyMatch(row -> row.id().equals(id));
    }

    private static int indexOf(List<SessionView> rows, SessionId id) {
        return IntStream.range(0, rows.size())
                .filter(index -> rows.get(index).id().equals(id))
                .findFirst()
                .orElseThrow(() -> new SelectedSessionNotVisibleException(id));
    }
}
