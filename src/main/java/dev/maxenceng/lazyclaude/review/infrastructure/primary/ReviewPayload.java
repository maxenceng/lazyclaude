package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** What {@link ReviewQuery} is asked with: the root of the project whose repository's pull requests are wanted. */
public record ReviewPayload(Path root) {

    Project toDomain() {
        return new Project(root);
    }
}
