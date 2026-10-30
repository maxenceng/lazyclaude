package dev.maxenceng.lazyclaude.board.domain;

import dev.maxenceng.lazyclaude.shared.domain.Project;

/** What reading a project's board gives: its tickets, or why there are none. */
public sealed interface BoardReading permits Board, UnavailableBoard {

    Project project();
}
