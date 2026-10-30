package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.LiveSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PastSessionView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsView;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.Marker;

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
    private Optional<Verb> offered = Optional.empty();
    private Optional<RowKey> selected;
    private boolean expanded;

    SessionRows(SessionsView sessions) {
        this.sessions = sessions;
        this.selected = firstSelectable();
    }

    List<SessionRow> visible() {
        return Stream.concat(
                        ticket.stream().flatMap(selectedTicket -> ticketSection(sessions, selectedTicket)),
                        unlinkedSection(sessions))
                .toList();
    }

    private Stream<SessionRow> ticketSection(SessionsView available, LinkedTicketId selectedTicket) {
        List<SessionRow> linked = Stream.of(
                        available.live().stream()
                                .flatMap(session -> session.linkTo(selectedTicket)
                                        .<SessionRow>map(link -> new LiveSessionRow(session, Optional.of(link)))
                                        .stream()),
                        available.past().stream()
                                .flatMap(session -> session.linkTo(selectedTicket)
                                        .<SessionRow>map(link -> new PastSessionRow(session, Optional.of(link)))
                                        .stream()),
                        available.hosted().stream()
                                .filter(tab -> tab.live().isEmpty() && tab.ticket().filter(selectedTicket::equals).isPresent())
                                .<SessionRow>map(StartingSessionRow::new))
                .flatMap(Function.identity())
                .toList();
        return Stream.of(
                        Stream.<SessionRow>of(new SectionHeaderRow(TicketText.number(selectedTicket.value()))),
                        linked.isEmpty() ? Stream.<SessionRow>of(new NoSessionRow(selectedTicket)) : linked.stream(),
                        offered.stream().<SessionRow>map(verb -> new NewSessionRow(selectedTicket, verb)))
                .flatMap(Function.identity());
    }

    private Stream<SessionRow> unlinkedSection(SessionsView available) {
        List<LiveSessionView> live = available.unlinkedLive();
        List<PastSessionView> past = available.unlinkedPast();
        if (live.isEmpty() && past.isEmpty()) {
            return Stream.empty();
        }
        int shown = expanded ? past.size() : Math.min(past.size(), PAST_SHOWN);
        return Stream.of(
                        Stream.<SessionRow>of(new SectionHeaderRow(SessionText.unlinkedLabel())),
                        live.stream().<SessionRow>map(session -> new LiveSessionRow(session, Optional.empty())),
                        past.stream().limit(shown).<SessionRow>map(session -> new PastSessionRow(session, Optional.empty())),
                        Included.<SessionRow>when(shown < past.size(), new OlderSessionsRow(past.size() - shown)))
                .flatMap(Function.identity());
    }

    /** The hosted sessions, in tab order, whether or not the registry could be read. */
    List<HostedSessionView> hosted() {
        return sessions.hosted();
    }

    /** How many hosted sessions are busy or waiting on you, which quitting would cut off. */
    long busyOrWaitingHosted() {
        return sessions.hosted().stream().filter(HostedSessionView::busyOrWaiting).count();
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

    boolean pastSelected() {
        return selected().filter(PastSessionRow.class::isInstance).isPresent();
    }

    boolean olderSelected() {
        return selected().filter(OlderSessionsRow.class::isInstance).isPresent();
    }

    /** The strongest marker among the live sessions linked to {@code ticketId}, for the ticket's row on the board. */
    Optional<Marker> markerOf(LinkedTicketId ticketId) {
        return sessions.strongestMarkerOf(ticketId);
    }

    /** How many of the project's live sessions, linked or not, carry each marker, for the status line. */
    Map<Marker, Long> markerCounts() {
        return sessions.markerCounts();
    }

    /** The verb the shown ticket's {@code n new} row offers to start, none while it offers no session, as a {@code done} ticket does not. */
    void offering(Optional<Verb> verb) {
        offered = verb;
    }

    /** Shows {@code next}'s section, the selection going to its first row when the ticket changed. */
    void showTicket(Optional<LinkedTicketId> next) {
        if (next.equals(ticket)) {
            return;
        }
        ticket = next;
        selected = firstSelectable();
    }

    /** Selects the session of the shown ticket with the strongest marker, the first of any tied, else its {@code n new} row, else the first row. */
    void selectStrongestMarker() {
        Optional<RowKey> strongest = ticket
                .flatMap(sessions::strongestSessionOf)
                .map(session -> new LiveSessionRow(session, Optional.empty()).key());
        selected = strongest.or(this::newSessionKey).or(this::firstSelectable);
    }

    /** Unfolds every older past session in place; the selection lands on the first of them. */
    void expand() {
        int index = selectedIndex();
        expanded = true;
        selected = Optional.of(visible().get(index).key());
    }

    /** Puts the selection on the row at {@code index} of the visible ones, when there is one and the cursor may stand on it. */
    void selectAt(int index) {
        visible().stream()
                .skip(index)
                .findFirst()
                .filter(SessionRow::selectable)
                .map(SessionRow::key)
                .ifPresent(key -> selected = Optional.of(key));
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
        selected = RowSelection.moved(selected, step, selectableKeys());
    }

    private Optional<RowKey> newSessionKey() {
        return visible().stream()
                .filter(NewSessionRow.class::isInstance)
                .map(SessionRow::key)
                .findFirst();
    }

    private Optional<RowKey> firstSelectable() {
        return selectableKeys().stream().findFirst();
    }

    private List<RowKey> selectableKeys() {
        return visible().stream().filter(SessionRow::selectable).map(SessionRow::key).toList();
    }

    private int selectableIndexOfSelected() {
        return RowSelection.indexIn(selected, selectableKeys());
    }

    /** Whether {@code key} is an unlinked past session the fold hides, as a selected live session is once it dies idle long enough. */
    private boolean isFolded(RowKey key) {
        return sessions.unlinkedPast().stream()
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
