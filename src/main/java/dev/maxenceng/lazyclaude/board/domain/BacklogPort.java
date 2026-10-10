package dev.maxenceng.lazyclaude.board.domain;

import java.util.List;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where a project's backlog files are read from. */
public interface BacklogPort {

    /**
     * Every backlog file that claims to be a ticket, ordered by file name; files that do not
     * are left out.
     *
     * @throws BacklogNotFoundException when the project has no backlog
     * @throws BacklogUnavailableException when the backlog exists but cannot be listed
     */
    List<BacklogEntry> entries(Project project);
}
