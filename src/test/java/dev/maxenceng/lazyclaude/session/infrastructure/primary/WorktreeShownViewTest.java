package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.sessionPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.clearHeldWorktreeView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.dirtyWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.heldWorktreeView;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Reason;

class WorktreeShownViewTest {

    @Test
    void shouldShowAClearWorktreeByItsPathFromTheProjectParent() {
        assertThat(WorktreeShownView.from(clearHeldWorktreeView(), sessionPayload()))
                .isEqualTo(new ClearWorktreeView(Path.of("../app.worktrees/PROJ-1-ticket")));
    }

    @Test
    void shouldShowAWorktreeHoldingSomethingWithWhatStandsInItAndWhyItIsNotRemoved() {
        assertThat(WorktreeShownView.from(heldWorktreeView(dirtyWorktreeContentsView(), false), sessionPayload()))
                .isEqualTo(NotClearWorktreeView.builder()
                        .shown(Path.of("../app.worktrees/PROJ-1-ticket"))
                        .state(dirtyWorktreeContentsView())
                        .held(new Reason("cannot remove: 1 dirty path"))
                        .build());
    }

    @Test
    void shouldShowAWorktreeWithNoMergedPullRequestAsHeldBack() {
        assertThat(WorktreeShownView.from(heldWorktreeView(new NoMergedPullRequestView(), false), sessionPayload()))
                .isInstanceOf(NotClearWorktreeView.class);
    }

    @Test
    void shouldShowATicketWithNoWorktreeAsNone() {
        assertThat(WorktreeShownView.from(new NoWorktreeView(), sessionPayload())).isEqualTo(new NoWorktreeView());
    }

    @Test
    void shouldShowAnUnreadableWorktreeWithItsReasonAndWhyItIsNotRemoved() {
        assertThat(WorktreeShownView.from(new UnreadableWorktreeView(reason()), sessionPayload()))
                .isEqualTo(UnreadWorktreeView.builder()
                        .reason(reason())
                        .held(new Reason("cannot remove: unreadable (no registry and no claude)"))
                        .build());
    }
}
