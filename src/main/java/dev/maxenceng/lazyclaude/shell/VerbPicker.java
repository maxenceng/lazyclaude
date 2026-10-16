package dev.maxenceng.lazyclaude.shell;

import java.util.List;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketView;
import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.VerbChoiceView;

/**
 * The verb picker over one ticket: its verbs, the cursor, and whether the cursor has been moved. The cursor starts on the
 * preselected verb and follows a later preselection only until it is moved, so the person's own choice is never taken from them.
 */
final class VerbPicker {

    private final TicketView ticket;
    private VerbChoiceView choice;
    private boolean mergedPending;
    private int cursor;
    private boolean moved;

    VerbPicker(TicketView ticket, VerbChoiceView choice, boolean mergedPending) {
        this.ticket = ticket;
        this.choice = choice;
        this.mergedPending = mergedPending;
        this.cursor = choice.verbs().indexOf(choice.preselected());
    }

    TicketView ticket() {
        return ticket;
    }

    List<Verb> verbs() {
        return choice.verbs();
    }

    int cursor() {
        return cursor;
    }

    Verb selected() {
        return choice.verbs().get(cursor);
    }

    /** Whether the verbs wait on review's first merged read, which only the preselection of {@code done} depends on. */
    boolean checking() {
        return mergedPending && choice.verbs().contains(Verb.DONE);
    }

    void down() {
        moveTo(cursor + 1);
    }

    void up() {
        moveTo(cursor - 1);
    }

    /** Takes {@code next} as the choice; the cursor goes to its preselected verb only if it was never moved. */
    void preselecting(VerbChoiceView next) {
        choice = next;
        cursor = moved ? Math.min(cursor, next.verbs().size() - 1) : next.verbs().indexOf(next.preselected());
    }

    void checked() {
        mergedPending = false;
    }

    private void moveTo(int target) {
        int next = Math.clamp(target, 0, choice.verbs().size() - 1);
        moved |= next != cursor;
        cursor = next;
    }
}
