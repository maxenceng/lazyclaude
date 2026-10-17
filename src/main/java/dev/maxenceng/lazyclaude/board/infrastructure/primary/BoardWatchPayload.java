package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** What {@link BoardQuery} is asked to watch with: the directory lazyclaude was started in. */
public record BoardWatchPayload(Path from) {

    WorkingDirectory toDomain() {
        return new WorkingDirectory(from);
    }
}
