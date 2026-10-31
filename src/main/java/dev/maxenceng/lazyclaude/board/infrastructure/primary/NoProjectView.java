package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import dev.maxenceng.lazyclaude.board.domain.NoProject;
import dev.maxenceng.lazyclaude.board.published.Reason;

/** A working directory that belongs to no project: no board, and why. */
public record NoProjectView(Reason reason) implements BoardLookupView {

    static NoProjectView from(NoProject none) {
        return new NoProjectView(none.reason());
    }
}
