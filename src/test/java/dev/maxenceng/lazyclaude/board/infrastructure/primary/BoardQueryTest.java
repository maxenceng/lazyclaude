package dev.maxenceng.lazyclaude.board.infrastructure.primary;

import static dev.maxenceng.lazyclaude.board.domain.BranchChangesFixture.frontendChanges;
import static dev.maxenceng.lazyclaude.board.domain.FrontmatterFixture.datedFrontmatter;
import static dev.maxenceng.lazyclaude.board.domain.TicketFileFixture.inReviewTicketFile;
import static dev.maxenceng.lazyclaude.board.domain.TicketFileFixture.ticketFile;
import static dev.maxenceng.lazyclaude.board.domain.TicketFileFixture.ticketFileOf;
import static dev.maxenceng.lazyclaude.board.domain.TicketFixture.ticketBuilder;
import static dev.maxenceng.lazyclaude.board.domain.UnreadableTicketFixture.unreadableTicket;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQueryFixture.boardQuery;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.FrontmatterEntryViewFixture.datedFrontmatterViews;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketViewBuilder;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.UnreadableTicketViewFixture.unreadableTicketView;
import static dev.maxenceng.lazyclaude.board.published.FileNameFixture.fileName;
import static dev.maxenceng.lazyclaude.board.published.TicketBranchFixture.ticketBranch;
import static dev.maxenceng.lazyclaude.board.published.TicketIdFixture.ticketId;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.workingDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.domain.BacklogPort;
import dev.maxenceng.lazyclaude.board.domain.BranchChangesPort;
import dev.maxenceng.lazyclaude.board.domain.BranchCopyPort;
import dev.maxenceng.lazyclaude.board.published.NoBody;
import dev.maxenceng.lazyclaude.board.published.NoTitle;
import dev.maxenceng.lazyclaude.board.published.Status;
import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectNotFoundException;

@ExtendWith(MockitoExtension.class)
class BoardQueryTest {

    private static final BoardPayload FROM_THE_PROJECT = new BoardPayload(workingDirectory().path(), Set.of());

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private BacklogPort backlog;

    @Mock
    private BranchCopyPort copies;

    @Mock
    private BranchChangesPort changes;

    @Test
    void shouldViewEveryStatusGroupInLifecycleOrder() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).groups())
                .extracting(StatusGroupView::status)
                .containsExactly(Status.values());
    }

    @Test
    void shouldViewTheRootOfTheProjectTheBoardWasReadFrom() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of());

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).root())
                .as("the shell hands it on to the contexts that work in the same project")
                .isEqualTo(BoardViewFixture.projectRoot());
    }

    @Test
    void shouldViewTheVerbsAnInProgressTicketIsAllowedWithDesignWhenItsBranchTouchesTheFrontend() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFileOf(ticketBuilder().status(Status.IN_PROGRESS).build())));
        when(changes.changesOf(project(), ticketBranch())).thenReturn(frontendChanges());

        assertThat(boardQuery(locator, backlog, copies, changes)
                        .boardFor(FROM_THE_PROJECT)
                        .groups()
                        .get(Status.IN_PROGRESS.ordinal())
                        .tickets())
                .extracting(TicketView::verbs)
                .containsExactly(List.of(Verb.REVIEW, Verb.DESIGN));
    }

    @Test
    void shouldViewATicketsFields() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFile()));

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticketViewBuilder().status(Status.TODO).verbs(List.of(Verb.START)).build());
    }

    @Test
    void shouldViewEveryFrontmatterKeyInFileOrderWithItsText() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFileOf(ticketBuilder().frontmatter(datedFrontmatter()).build())));

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).groups().get(Status.TODO.ordinal()).tickets())
                .containsExactly(ticketViewBuilder()
                        .status(Status.TODO)
                        .frontmatter(datedFrontmatterViews())
                        .verbs(List.of(Verb.START))
                        .build());
    }

    @Test
    void shouldViewATicketWithNoBodyAsHavingNoBody() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFileOf(ticketBuilder().body(new NoBody()).build())));

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).groups().get(Status.TODO.ordinal()).tickets())
                .as("a ticket with no body has no title to derive")
                .containsExactly(ticketViewBuilder().status(Status.TODO).title(new NoTitle()).body(new NoBody()).verbs(List.of(Verb.START)).build());
    }

    @Test
    void shouldViewAnUnreadableFileWithItsReason() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(unreadableTicket()));

        assertThat(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).unreadable())
                .containsExactly(unreadableTicketView());
    }

    @Test
    void shouldViewTheBranchCopyOfATicketWhoseBranchIsNotMerged() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFile()));
        when(copies.onBranch(project(), ticketBranch(), fileName())).thenReturn(Optional.of(inReviewTicketFile()));

        assertThat(boardQuery(locator, backlog, copies).boardFor(FROM_THE_PROJECT).groups().get(Status.IN_REVIEW.ordinal()).tickets())
                .extracting(TicketView::id)
                .containsExactly(ticketId());
    }

    @Test
    void shouldViewTheRootCopyOfATicketWhoseBranchIsNamedAsMerged() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFile()));
        BoardPayload merged = new BoardPayload(workingDirectory().path(), Set.of(ticketBranch().value()));

        assertThat(boardQuery(locator, backlog, copies).boardFor(merged).groups().get(Status.TODO.ordinal()).tickets())
                .extracting(TicketView::id)
                .containsExactly(ticketId());
    }

    @Test
    void shouldViewTheBoardReadAgain() {
        when(locator.locate(workingDirectory())).thenReturn(project());
        when(backlog.entries(project())).thenReturn(List.of(ticketFile()));

        assertThat(boardQuery(locator, backlog).refreshedBoardFor(FROM_THE_PROJECT))
                .get()
                .extracting(board -> board.groups().get(Status.TODO.ordinal()).tickets())
                .isEqualTo(boardQuery(locator, backlog).boardFor(FROM_THE_PROJECT).groups().get(Status.TODO.ordinal()).tickets());
    }

    @Test
    void shouldViewNothingWhenTheBoardCannotBeReadAgain() {
        when(locator.locate(workingDirectory())).thenThrow(new ProjectNotFoundException("not here"));

        assertThat(boardQuery(locator, backlog).refreshedBoardFor(FROM_THE_PROJECT))
                .as("a board already on screen stays, rather than being replaced by a failure")
                .isEmpty();
    }
}
