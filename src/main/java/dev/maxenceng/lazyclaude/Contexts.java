package dev.maxenceng.lazyclaude;

import java.time.Duration;

import dev.maxenceng.lazyclaude.board.application.BoardApplicationService;
import dev.maxenceng.lazyclaude.board.application.TicketCaptureApplicationService;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureCommand;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.BranchChangesRepository;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssuesRepository;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.IssuesWatchRepository;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.TicketCaptureRepository;
import dev.maxenceng.lazyclaude.board.infrastructure.secondary.TicketTemplateRepository;
import dev.maxenceng.lazyclaude.docs.application.DocsApplicationService;
import dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsQuery;
import dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsRepository;
import dev.maxenceng.lazyclaude.docs.infrastructure.secondary.DocsWatchRepository;
import dev.maxenceng.lazyclaude.review.application.PendingReviewApplicationService;
import dev.maxenceng.lazyclaude.review.application.PullRequestDiffApplicationService;
import dev.maxenceng.lazyclaude.review.application.ReviewApplicationService;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewCommand;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewQuery;
import dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhMergedPullRequestsRepository;
import dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhPendingReviewRepository;
import dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhPullRequestDiffRepository;
import dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhPullRequestsRepository;
import dev.maxenceng.lazyclaude.review.infrastructure.secondary.GhThreadRepository;
import dev.maxenceng.lazyclaude.review.infrastructure.secondary.PullRequestWatchRepository;
import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulators;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketWorktreeCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WatchedSessionCommand;
import dev.maxenceng.lazyclaude.shared.application.WarningApplicationService;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningCommand;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningQuery;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.ProjectLocatorRepository;
import dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WarningRepository;

/**
 * Every context's Query and Command, built once and handed whole to whichever frontend lazyclaude opens, the shell or the sidecar,
 * so a context is wired in one place however many frontends read it (ADR 0077). Each frontend reads the ones it drives.
 */
public record Contexts(
        BoardQuery board,
        TicketCaptureCommand capturing,
        DocsQuery docs,
        SessionQuery sessions,
        HostedSessionCommand hosting,
        HostedEmulatorCommand emulating,
        HostedEmulatorQuery emulators,
        WatchedSessionCommand watching,
        TicketWorktreeCommand removing,
        ReviewQuery review,
        PendingReviewCommand pendingReview,
        WarningQuery warnings,
        WarningCommand warning) {

    private static final String GH = "gh";
    private static final Duration GH_PATIENCE = Duration.ofSeconds(30);

    /** The emulators of hosted sessions are built idle: only a frontend that draws them asks them to run. */
    static Contexts of(WarningRepository warnings, SessionApplicationService sessions) {
        HostedEmulators emulators = HostedEmulators.over(sessions);
        WarningApplicationService warningService = new WarningApplicationService(warnings, warnings);
        PendingReviewApplicationService pendingReview =
                new PendingReviewApplicationService(new GhPendingReviewRepository(GH, GH_PATIENCE));
        return new Contexts(
                new BoardQuery(new BoardApplicationService(
                        new ProjectLocatorRepository(),
                        new IssuesRepository(),
                        new BranchChangesRepository(),
                        new IssuesWatchRepository(),
                        warnings)),
                new TicketCaptureCommand(new TicketCaptureApplicationService(new TicketCaptureRepository(), new TicketTemplateRepository())),
                new DocsQuery(new DocsApplicationService(new DocsRepository(), new DocsWatchRepository(), warnings)),
                new SessionQuery(sessions),
                new HostedSessionCommand(sessions),
                new HostedEmulatorCommand(sessions, emulators),
                new HostedEmulatorQuery(emulators),
                new WatchedSessionCommand(sessions),
                new TicketWorktreeCommand(sessions),
                new ReviewQuery(
                        new ReviewApplicationService(
                                new GhPullRequestsRepository(GH, GH_PATIENCE),
                                new GhMergedPullRequestsRepository(GH, GH_PATIENCE),
                                new PullRequestWatchRepository()),
                        new PullRequestDiffApplicationService(
                                new GhPullRequestDiffRepository(GH, GH_PATIENCE), new GhThreadRepository(GH, GH_PATIENCE)),
                        pendingReview),
                new PendingReviewCommand(pendingReview),
                new WarningQuery(warningService),
                new WarningCommand(warningService));
    }
}
