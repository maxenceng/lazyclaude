package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where a project's tickets are read from: the issues of its GitHub remote. */
public interface BacklogPort {

    /**
     * Every ticket, open or closed, in no particular order: the board orders them.
     *
     * @throws BacklogUnavailableException when the project has no GitHub remote, GitHub cannot be reached or {@code gh} cannot be used
     */
    List<BacklogEntry> entries(Project project);
}
