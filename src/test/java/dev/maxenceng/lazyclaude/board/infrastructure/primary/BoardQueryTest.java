package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static dev.maxenceng.lazyclaude.board.domain.TicketFileFixture.ticketFile;
import static dev.maxenceng.lazyclaude.board.domain.UnreadableTicketFixture.unreadableTicket;
import static dev.maxenceng.lazyclaude.board.domain.WorkingDirectoryFixture.workingDirectory;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQueryFixture.boardQuery;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewBuilder;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.UnreadableTicketViewFixture.unreadableTicketView;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.board.published.Status;

@ExtendWith(MockitoExtension.class)
class BoardQueryTest {

    private static final BoardPayload FROM_THE_PROJECT = new BoardPayload(workingDirectory().path());

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private BacklogPort backlog;

    @Test
    void shouldViewEveryStatusGroupInLifecycleOrder() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).groups())
                .extracting(StatusGroupView::status)
                .containsExactly(Status.values());
    }

    @Test
    void shouldViewATicketsFields() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFile()));

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticketViewBuilder().status(Status.TODO).build());
    }

    @Test
    void shouldViewAnUnreadableFileWithItsReason() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(unreadableTicket()));

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).unreadable())
                .containsExactly(unreadableTicketView());
    }
}
