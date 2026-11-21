package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.WorktreeViewFixture.dirtyWorktreeContentsView;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.published.Reason;

class WorktreeRemovalOutcomeViewTest {

    @Test
    void shouldReportARemovalAsRemoved() {
        assertThat(WorktreeRemovalOutcomeView.from(new RemovedWorktreeView())).isEqualTo(new RemovedWorktreeView());
    }

    @Test
    void shouldReportARefusalAsNotRemovedWithWhatStandsInTheWorktree() {
        assertThat(WorktreeRemovalOutcomeView.from(new RefusedRemovalView(dirtyWorktreeContentsView())))
                .isEqualTo(new NotRemovedWorktreeView(new Reason("cannot remove: 1 dirty path — src/Foo.java")));
    }

    @Test
    void shouldReportAFailureAsNotRemovedWithGitsReason() {
        assertThat(WorktreeRemovalOutcomeView.from(new FailedRemovalView(reason())))
                .isEqualTo(new NotRemovedWorktreeView(new Reason("cannot remove: no registry and no claude")));
    }
}
