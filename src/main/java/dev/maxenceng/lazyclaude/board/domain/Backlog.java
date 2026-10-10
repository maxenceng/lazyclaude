package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Where a project's tickets are read from. */
public interface Backlog {

    Board read(Project project);
}
