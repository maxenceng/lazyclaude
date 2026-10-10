package dev.maxenceng.lazyclaude.board.application;

import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.board.domain.TicketFile;
import dev.maxenceng.lazyclaude.board.domain.WorkingDirectory;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class BoardApplicationServiceTest {

    @Test
    void shouldReadTheBoardOfTheProjectTheWorkingDirectoryBelongsTo() {
        Project project = new Project(Path.of("/work/app"));
        WorkingDirectory from = new WorkingDirectory(Path.of("/work/app/src"));
        TicketFile file = ticketFile("A-1.md", "A-1", Status.TODO);

        Board board = new BoardApplicationService(
                        asked -> asked.equals(from) ? project : null,
                        asked -> asked.equals(project) ? List.of(file) : List.of())
                .boardFor(from);

        assertThat(board.groups().get(Status.TODO.ordinal()).tickets()).containsExactly(file.ticket());
    }
}
