package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.domain.Status;
import dev.maxenceng.lazyclaude.board.domain.Ticket;
import dev.maxenceng.lazyclaude.board.domain.UnreadableTicket;

/** The Board panel's rows and the two things the shell remembers about them: the selection and the folded groups. */
final class BoardRows {

    private static final String UNREADABLE = "unreadable";

    private final Board board;
    private final Set<String> folded = new HashSet<>(Set.of(Status.DONE.label()));
    private String selected;

    BoardRows(Board board) {
        this.board = board;
        this.selected = visible().getFirst().key();
    }

    sealed interface Row {
        String key();

        record Header(String label, int count, boolean folded) implements Row {
            @Override
            public String key() {
                return "group:" + label;
            }

            String text() {
                return (folded ? "▸ " : "▾ ") + label + " (" + count + ")";
            }
        }

        record TicketRow(Ticket ticket) implements Row {
            @Override
            public String key() {
                return "ticket:" + ticket.id().value();
            }

            String text() {
                return "  " + ticket.id().value() + " " + ticket.title();
            }
        }

        record UnreadableRow(UnreadableTicket unreadable) implements Row {
            @Override
            public String key() {
                return "file:" + unreadable.file();
            }

            String text() {
                return "  " + unreadable.file() + " — " + unreadable.reason();
            }
        }
    }

    List<Row> visible() {
        List<Row> rows = new ArrayList<>();
        for (Status status : Status.values()) {
            List<Ticket> tickets = board.tickets(status);
            boolean isFolded = folded.contains(status.label());
            rows.add(new Row.Header(status.label(), tickets.size(), isFolded));
            if (!isFolded) {
                tickets.forEach(ticket -> rows.add(new Row.TicketRow(ticket)));
            }
        }
        if (!board.unreadable().isEmpty()) {
            boolean isFolded = folded.contains(UNREADABLE);
            rows.add(new Row.Header(UNREADABLE, board.unreadable().size(), isFolded));
            if (!isFolded) {
                board.unreadable().forEach(unreadable -> rows.add(new Row.UnreadableRow(unreadable)));
            }
        }
        return rows;
    }

    Row selected() {
        return visible().stream().filter(row -> row.key().equals(selected)).findFirst().orElseThrow();
    }

    int selectedIndex() {
        return visible().indexOf(selected());
    }

    void down() {
        move(1);
    }

    void up() {
        move(-1);
    }

    Optional<Row.Header> selectedHeader() {
        return selected() instanceof Row.Header header ? Optional.of(header) : Optional.empty();
    }

    void toggleFold() {
        selectedHeader().ifPresent(header -> {
            if (!folded.remove(header.label())) {
                folded.add(header.label());
            }
        });
    }

    private void move(int step) {
        List<Row> rows = visible();
        int target = Math.max(0, Math.min(rows.size() - 1, rows.indexOf(selected()) + step));
        selected = rows.get(target).key();
    }
}
