package dev.maxenceng.lazyclaude;

import static org.mockito.Mockito.mock;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureCommand;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketWorktreeCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionCommand;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningCommand;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningQuery;

public final class ContextsFixture {

    private ContextsFixture() {}

    public static Contexts contexts(BoardQuery board, DocsQuery docs, SessionQuery sessions) {
        return contexts(board, docs, sessions, mock(ReviewQuery.class), mock(WarningQuery.class));
    }

    public static Contexts contexts(BoardQuery board, DocsQuery docs, SessionQuery sessions, ReviewQuery review, WarningQuery warnings) {
        return contexts(board, docs, sessions, review, warnings, mock(HostedEmulatorQuery.class));
    }

    public static Contexts contexts(
            BoardQuery board, DocsQuery docs, SessionQuery sessions, ReviewQuery review, WarningQuery warnings, HostedEmulatorQuery emulators) {
        return new Contexts(
                board,
                mock(TicketCaptureCommand.class),
                docs,
                sessions,
                mock(HostedSessionCommand.class),
                mock(HostedEmulatorCommand.class),
                emulators,
                mock(WatchedSessionCommand.class),
                mock(TicketWorktreeCommand.class),
                review,
                mock(PendingReviewCommand.class),
                warnings,
                mock(WarningCommand.class));
    }

    public static Contexts sidecarContexts(
            BoardQuery board,
            SessionQuery sessions,
            HostedSessionCommand hosting,
            WarningQuery warnings,
            DocsQuery docs,
            ReviewQuery review,
            PendingReviewCommand pendingReview) {
        return new Contexts(
                board,
                mock(TicketCaptureCommand.class),
                docs,
                sessions,
                hosting,
                mock(HostedEmulatorCommand.class),
                mock(HostedEmulatorQuery.class),
                mock(WatchedSessionCommand.class),
                mock(TicketWorktreeCommand.class),
                review,
                pendingReview,
                warnings,
                mock(WarningCommand.class));
    }
}
