package dev.maxenceng.lazyclaude.docs.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** What {@link DocsQuery} is asked with: the root of the project whose documents are wanted. */
public record DocsPayload(Path root) {

    Project toDomain() {
        return new Project(root);
    }
}
