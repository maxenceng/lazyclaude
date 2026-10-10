package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** What {@link SessionQuery} is asked with: the root of the project whose sessions are wanted. */
public record SessionPayload(Path root) {

    Project toDomain() {
        return new Project(root);
    }
}
