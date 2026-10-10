package dev.maxenceng.lazyclaude.board.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.domain.Board;
import dev.maxenceng.lazyclaude.shared.domain.Project;

class ReadBoardTest {

    @Test
    void shouldReadTheBoardOfTheProjectItIsAskedFor() {
        Project project = new Project(Path.of("/work/app"));
        Board board = Board.of(List.of(), List.of());

        Board read = new ReadBoard(asked -> asked.equals(project) ? board : null).read(project);

        assertThat(read).isSameAs(board);
    }
}
