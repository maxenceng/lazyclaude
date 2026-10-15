package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.board.published.TicketBranch;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/**
 * What {@link BoardQuery} is asked with: the directory lazyclaude was started in, and the names of the branches whose pull request is
 * merged, which {@code board} takes as given and knows nothing more of.
 */
public record BoardPayload(Path from, Set<String> mergedBranches) {

    WorkingDirectory toDomain() {
        return new WorkingDirectory(from);
    }

    Set<TicketBranch> merged() {
        return mergedBranches.stream().map(TicketBranch::new).collect(Collectors.toUnmodifiableSet());
    }
}
