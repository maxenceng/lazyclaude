package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where a project's tickets are read from: the issues of its GitHub remote. */
public interface BacklogPort {

    /**
     * Every open issue and every one closed as completed or as not planned, ordered by number.
     *
     * @throws BacklogNotFoundException when the project has no GitHub remote
     * @throws BacklogUnavailableException when GitHub cannot be reached or {@code gh} cannot be used
     */
    List<BacklogEntry> entries(Project project);
}
