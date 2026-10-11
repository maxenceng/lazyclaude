package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.RunningSessionsFixture.runningSessions;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.otherProjectDirectory;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.shared.domain.Project;

class LastLiveReadTest {

    @Test
    void shouldHoldNothingBeforeTheFirstRead() {
        assertThat(new LastLiveRead().of(project())).isEmpty();
    }

    @Test
    void shouldGiveBackTheLastReadOfTheProject() {
        LastLiveRead last = new LastLiveRead();

        last.remember(runningSessions());

        assertThat(last.of(project())).contains(runningSessions());
    }

    @Test
    void shouldNotGiveBackAReadOfAnotherProject() {
        LastLiveRead last = new LastLiveRead();

        last.remember(runningSessions());

        assertThat(last.of(new Project(otherProjectDirectory().path())))
                .as("another project's live sessions are not this one's")
                .isEmpty();
    }

    @Test
    void shouldHoldNothingOnceForgotten() {
        LastLiveRead last = new LastLiveRead();
        last.remember(runningSessions());

        last.forget();

        assertThat(last.of(project())).as("a failed read must not leave the one before it standing").isEmpty();
    }
}
