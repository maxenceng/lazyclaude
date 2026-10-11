package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.AvailableSessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnavailableSessionsView;
import dev.maxenceng.lazyclaude.session.published.Reason;

/**
 * The Sessions panel's rows and what the shell remembers about them: the selection, by session id, so it survives the
 * sessions being read again and does not move when a row above it appears or a state changes, and whether the older past
 * sessions are unfolded. Only the newest few past sessions show until then, followed by a row that counts the rest.
 */
final class SessionRows {

    private static final int DOWN = 1;
    private static final int UP = -1;
    private static final int PAST_SHOWN = 5;

    private SessionsView sessions;
    private Optional<RowKey> selected;
    private boolean expanded;

    SessionRows(SessionsView sessions) {
        this.sessions = sessions;
        this.selected = visible().stream().findFirst().map(SessionRow::key);
    }

    List<SessionRow> visible() {
        return switch (sessions) {
            case AvailableSessionsView available -> rowsOf(available);
            case UnavailableSessionsView unavailable -> List.of();
        };
    }

    private List<SessionRow> rowsOf(AvailableSessionsView available) {
        List<SessionRow> rows = new ArrayList<>();
        available.live().forEach(session -> rows.add(new LiveSessionRow(session)));
        List<PastSessionView> past = available.past();
        int shown = expanded ? past.size() : Math.min(past.size(), PAST_SHOWN);
        past.stream().limit(shown).forEach(session -> rows.add(new PastSessionRow(session)));
        if (shown < past.size()) {
            rows.add(new OlderSessionsRow(past.size() - shown));
        }
        return rows;
    }

    /** Why there are no sessions to show, when no source answered. */
    Optional<Reason> unavailable() {
        return sessions instanceof UnavailableSessionsView unavailable ? Optional.of(unavailable.reason()) : Optional.empty();
    }

    boolean isEmpty() {
        return selected.isEmpty();
    }

    Optional<SessionRow> selected() {
        List<SessionRow> rows = visible();
        return selected.map(key -> rows.get(indexOf(rows, key)));
    }

    int selectedIndex() {
        List<SessionRow> rows = visible();
        return selected.map(key -> indexOf(rows, key)).orElse(0);
    }

    Optional<RowKey> selectedKey() {
        return selected;
    }

    boolean olderSelected() {
        return selected().filter(OlderSessionsRow.class::isInstance).isPresent();
    }

    /** Unfolds every older past session in place; the selection lands on the first of them. */
    void expand() {
        int index = selectedIndex();
        expanded = true;
        selected = Optional.of(visible().get(index).key());
    }

    void down() {
        move(DOWN);
    }

    void up() {
        move(UP);
    }

    /** Swaps in freshly read sessions; the selection stays on its id, or on the row that took its place. Says whether the selection moved. */
    boolean replace(SessionsView next) {
        Optional<RowKey> was = selected;
        int index = selectedIndex();
        sessions = next;
        expanded |= was.filter(this::isFolded).isPresent();
        selected = RowSelection.kept(was, index, visible().stream().map(SessionRow::key).toList());
        return !was.equals(selected);
    }

    private void move(int step) {
        List<SessionRow> rows = visible();
        if (rows.isEmpty()) {
            return;
        }
        int target = Math.clamp(selectedIndex() + (long) step, 0, rows.size() - 1);
        selected = Optional.of(rows.get(target).key());
    }

    /** Whether {@code key} is a past session the fold hides, as a selected live session is once it dies idle long enough. */
    private boolean isFolded(RowKey key) {
        return sessions instanceof AvailableSessionsView available
                && available.past().stream().skip(PAST_SHOWN).map(PastSessionRow::new).anyMatch(row -> row.key().equals(key));
    }

    private static int indexOf(List<SessionRow> rows, RowKey key) {
        return IntStream.range(0, rows.size())
                .filter(index -> rows.get(index).key().equals(key))
                .findFirst()
                .orElseThrow(() -> new SelectedSessionNotVisibleException(key));
    }
}
