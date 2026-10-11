package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.AvailableSessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.LiveSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnavailableSessionsView;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.Marker;
import dev.maxenceng.lazyclaude.session.published.Reason;

/**
 * The Sessions panel's rows and what the shell remembers about them: the selection, by session id, so it survives the
 * sessions being read again and does not move when a row above it appears or a state changes, the ticket whose section is
 * shown, and whether the older unlinked past sessions are unfolded. The selected ticket's section comes first, under its id,
 * with every session linked to it; the unlinked section follows, where only the newest few past sessions show until the
 * rest are unfolded, followed by a row that counts them. Header rows are labels the cursor skips.
 */
final class SessionRows {

    private static final int DOWN = 1;
    private static final int UP = -1;
    private static final int PAST_SHOWN = 5;

    private SessionsView sessions;
    private Optional<LinkedTicketId> ticket = Optional.empty();
    private Optional<RowKey> selected;
    private boolean expanded;

    SessionRows(SessionsView sessions) {
        this.sessions = sessions;
        this.selected = firstSelectable();
    }

    List<SessionRow> visible() {
        return switch (sessions) {
            case AvailableSessionsView available -> rowsOf(available);
            case UnavailableSessionsView unavailable -> List.of();
        };
    }

    private List<SessionRow> rowsOf(AvailableSessionsView available) {
        List<SessionRow> rows = new ArrayList<>();
        ticket.ifPresent(selectedTicket -> addTicketSection(rows, available, selectedTicket));
        addUnlinkedSection(rows, available);
        return rows;
    }

    private void addTicketSection(List<SessionRow> rows, AvailableSessionsView available, LinkedTicketId selectedTicket) {
        rows.add(new SectionHeaderRow(selectedTicket.value()));
        int header = rows.size();
        available.live().stream()
                .flatMap(session -> session.linkTo(selectedTicket).map(link -> new LiveSessionRow(session, Optional.of(link))).stream())
                .forEach(rows::add);
        available.past().stream()
                .flatMap(session -> session.linkTo(selectedTicket).map(link -> new PastSessionRow(session, Optional.of(link))).stream())
                .forEach(rows::add);
        if (rows.size() == header) {
            rows.add(new NoSessionRow(selectedTicket));
        }
    }

    private void addUnlinkedSection(List<SessionRow> rows, AvailableSessionsView available) {
        List<LiveSessionView> live = available.unlinkedLive();
        List<PastSessionView> past = available.unlinkedPast();
        if (live.isEmpty() && past.isEmpty()) {
            return;
        }
        rows.add(new SectionHeaderRow(SessionText.unlinkedLabel()));
        live.forEach(session -> rows.add(new LiveSessionRow(session, Optional.empty())));
        int shown = expanded ? past.size() : Math.min(past.size(), PAST_SHOWN);
        past.stream().limit(shown).forEach(session -> rows.add(new PastSessionRow(session, Optional.empty())));
        if (shown < past.size()) {
            rows.add(new OlderSessionsRow(past.size() - shown));
        }
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

    /** The strongest marker among the live sessions linked to {@code ticketId}, for the ticket's row on the board. */
    Optional<Marker> markerOf(LinkedTicketId ticketId) {
        return sessions instanceof AvailableSessionsView available ? available.strongestMarkerOf(ticketId) : Optional.empty();
    }

    /** The strongest marker among the project's live sessions, linked or not, for the status line. */
    Optional<Marker> strongestMarker() {
        return sessions instanceof AvailableSessionsView available ? available.strongestMarker() : Optional.empty();
    }

    /** Shows {@code next}'s section, the selection going to its first row. Says whether the ticket changed. */
    boolean showTicket(Optional<LinkedTicketId> next) {
        if (next.equals(ticket)) {
            return false;
        }
        ticket = next;
        selected = firstSelectable();
        return true;
    }

    /** Selects the session of the shown ticket with the strongest marker, the first of any tied, else the first row. */
    void selectStrongestMarker() {
        Optional<RowKey> strongest = sessions instanceof AvailableSessionsView available
                ? ticket.flatMap(available::strongestSessionOf).map(session -> new LiveSessionRow(session, Optional.empty()).key())
                : Optional.empty();
        selected = strongest.or(this::firstSelectable);
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
        int index = selectableIndexOfSelected();
        sessions = next;
        expanded |= was.filter(this::isFolded).isPresent();
        selected = RowSelection.kept(was, index, selectableKeys());
        return !was.equals(selected);
    }

    private void move(int step) {
        List<RowKey> keys = selectableKeys();
        if (keys.isEmpty()) {
            return;
        }
        int target = Math.clamp(selectableIndexOfSelected() + (long) step, 0, keys.size() - 1);
        selected = Optional.of(keys.get(target));
    }

    private Optional<RowKey> firstSelectable() {
        return selectableKeys().stream().findFirst();
    }

    private List<RowKey> selectableKeys() {
        return visible().stream().filter(SessionRow::selectable).map(SessionRow::key).toList();
    }

    private int selectableIndexOfSelected() {
        List<RowKey> keys = selectableKeys();
        return selected.map(keys::indexOf).filter(index -> index >= 0).orElse(0);
    }

    /** Whether {@code key} is an unlinked past session the fold hides, as a selected live session is once it dies idle long enough. */
    private boolean isFolded(RowKey key) {
        return sessions instanceof AvailableSessionsView available
                && available.unlinkedPast().stream()
                        .skip(PAST_SHOWN)
                        .map(session -> new PastSessionRow(session, Optional.empty()))
                        .anyMatch(row -> row.key().equals(key));
    }

    private static int indexOf(List<SessionRow> rows, RowKey key) {
        return IntStream.range(0, rows.size())
                .filter(index -> rows.get(index).key().equals(key))
                .findFirst()
                .orElseThrow(() -> new SelectedSessionNotVisibleException(key));
    }
}
