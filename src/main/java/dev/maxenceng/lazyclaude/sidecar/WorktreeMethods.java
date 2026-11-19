package dev.maxenceng.lazyclaude.sidecar;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.MergedPullRequestsView;
import dev.maxenceng.lazyclaude.review.published.HeadBranch;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.FailedRemovalView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TicketWorktreeCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnreadWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnreadableWorktreeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeBranchPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreePayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeRemovalOutcomeView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeShownView;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;
import dev.maxenceng.lazyclaude.session.published.Reason;

/**
 * The ticket worktree methods of {@code session/}: {@code session/worktreeOf} and {@code session/removeWorktree}, each asked with the
 * ticket's branch alone. The sidecar joins what the shell joins (ADR 0038): the sessions as the last {@code session/watch} asked, and the
 * commit {@code review} last read the branch's pull request merged at, so {@code session} never reads {@code review}. A refusal is a result.
 * {@code session/ticketDoneEnded} tells the window which ticket's {@code /ticket done} session ended.
 */
final class WorktreeMethods {

    private static final ProtocolMethod<WorktreeBranchPayload, WorktreeShownView> WORKTREE_OF =
            new ProtocolMethod<>("session/worktreeOf", WorktreeBranchPayload.class, WorktreeShownView.class);
    private static final ProtocolMethod<WorktreeBranchPayload, WorktreeRemovalOutcomeView> REMOVE =
            new ProtocolMethod<>("session/removeWorktree", WorktreeBranchPayload.class, WorktreeRemovalOutcomeView.class);
    private static final ProtocolNotification<LinkedTicketId> DONE_ENDED =
            new ProtocolNotification<>("session/ticketDoneEnded", LinkedTicketId.class);
    private static final Reason NOT_WATCHED = new Reason("the project's sessions are not watched yet");

    private final SessionQuery sessions;
    private final TicketWorktreeCommand removing;
    private final SidecarOutput output;
    private volatile Optional<SessionPayload> watched = Optional.empty();
    private volatile MergedPullRequestsView merged = new MergedPullRequestsView(List.of());

    WorktreeMethods(SessionQuery sessions, TicketWorktreeCommand removing, SidecarOutput output) {
        this.sessions = sessions;
        this.removing = removing;
        this.output = output;
    }

    static Protocol protocol() {
        return new Protocol(List.of(WORKTREE_OF, REMOVE), List.of(), List.of(DONE_ENDED));
    }

    /** What the last {@code session/watch} asked: the project and the ids of the tickets on its board. */
    void sessionsWatched(SessionPayload payload) {
        watched = Optional.of(payload);
    }

    /** What {@code review} last read as merged, which a ticket's branch is looked up in. */
    void mergedRead(MergedPullRequestsView view) {
        merged = view;
    }

    Map<String, SidecarMethod> methods() {
        sessions.onTicketDoneEnded(ticket -> output.send(DONE_ENDED.message(ticket)));
        return Stream.of(
                        Map.<String, SidecarMethod>entry(WORKTREE_OF.name(), params -> answered(WORKTREE_OF.answer(params, this::worktreeOf))),
                        Map.<String, SidecarMethod>entry(REMOVE.name(), params -> answered(REMOVE.answer(params, this::remove))))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static SidecarAnswer answered(Object view) {
        return SidecarAnswer.of(SidecarJson.tree(view));
    }

    private WorktreeShownView worktreeOf(WorktreeBranchPayload branch) {
        return watched.map(project -> WorktreeShownView.from(sessions.worktreeOf(payload(project, branch)), project))
                .orElseGet(() -> UnreadWorktreeView.builder()
                        .reason(NOT_WATCHED)
                        .refusal(new UnreadableWorktreeView(NOT_WATCHED).refusal())
                        .build());
    }

    private WorktreeRemovalOutcomeView remove(WorktreeBranchPayload branch) {
        return watched.<WorktreeRemovalOutcomeView>map(project -> WorktreeRemovalOutcomeView.from(removing.remove(payload(project, branch))))
                .orElseGet(() -> WorktreeRemovalOutcomeView.from(new FailedRemovalView(NOT_WATCHED)));
    }

    private WorktreePayload payload(SessionPayload project, WorktreeBranchPayload branch) {
        return new WorktreePayload(project, branch.branch(), merged.mergedHeadOf(new HeadBranch(branch.branch())));
    }
}
