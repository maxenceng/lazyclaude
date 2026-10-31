package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * What {@link BoardQuery} is asked with after a change: the directory lazyclaude was started in, and whether the frontend shows a
 * board with tickets rather than the reason there are none.
 */
public record BoardAfterChangePayload(Path from, boolean boardShown) {

    WorkingDirectory toDomain() {
        return new WorkingDirectory(from);
    }
}
