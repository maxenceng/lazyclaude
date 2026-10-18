package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.nio.file.Path;

import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** What {@link ReviewQuery} is asked a pull request's diff or threads with: the root of the project whose repository holds it, and its number. */
public record PullRequestPayload(Path root, PullRequestNumber number) {

    Project project() {
        return new Project(root);
    }
}
