package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.board.domain.WorkingDirectory;

/** What {@link BoardQuery} is asked with: the directory lazyclaude was started in. */
public record BoardPayload(Path from) {

    WorkingDirectory toDomain() {
        return new WorkingDirectory(from);
    }
}
