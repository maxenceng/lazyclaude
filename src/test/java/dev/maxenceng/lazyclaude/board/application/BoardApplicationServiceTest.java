package dev.maxenceng.lazyclaude.board.application;

import static dev.maxenceng.lazyclaude.board.domain.TicketFileFixture.ticketFile;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticket;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;

@ExtendWith(MockitoExtension.class)
class BoardApplicationServiceTest {

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private BacklogPort backlog;

    @InjectMocks
    private BoardApplicationService service;

    @Test
    void shouldReadTheBoardOfTheProjectTheWorkingDirectoryBelongsTo() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFile()));

        assertThat(service.boardFor(workingDirectory()).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticket());
    }
}
