package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.emptyLaunchLog;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.endedAt;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.endedLaunchEntry;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchEntry;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchEntryBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchLogOf;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.secondLaunchEntry;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class LaunchLogTest {

    @Test
    void shouldAppendALaunchOfAnIdNotLoggedYet() {
        assertThat(launchLogOf(launchEntry()).launched(secondLaunchEntry()))
                .usingRecursiveComparison()
                .isEqualTo(launchLogOf(launchEntry(), secondLaunchEntry()));
    }

    @Test
    void shouldNotAddASecondEntryForAResumeOfAnIdAlreadyLogged() {
        LaunchEntry resume = launchEntryBuilder().command(new ResumeClaude(sessionId())).build();

        assertThat(launchLogOf(launchEntry()).launched(resume).entries())
                .as("the entry of the first launch stays: its directory, command and start are what the resume came from")
                .containsExactly(launchEntry());
    }

    @Test
    void shouldClearTheEndOfAnEntryItsResumeLaunchesAgain() {
        LaunchEntry resume = launchEntryBuilder().command(new ResumeClaude(sessionId())).build();

        assertThat(launchLogOf(endedLaunchEntry()).launched(resume).entries())
                .as("a resumed session is hosted again, and cut off again if lazyclaude quits")
                .containsExactly(launchEntry());
    }

    @Test
    void shouldRecordWhenTheSessionEndedOnItsEntryOnly() {
        assertThat(launchLogOf(launchEntry(), secondLaunchEntry()).ended(sessionId(), endedAt()).entries())
                .containsExactly(endedLaunchEntry(), secondLaunchEntry());
    }

    @Test
    void shouldChangeNothingWhenAnEndNamesAnIdNotLogged() {
        assertThat(launchLogOf(launchEntry()).ended(secondSessionId(), endedAt()).entries())
                .containsExactly(launchEntry());
    }

    @Test
    void shouldDropTheEntriesOfTheIdsItIsToldTo() {
        assertThat(launchLogOf(launchEntry(), secondLaunchEntry()).without(Set.of(sessionId())).entries())
                .containsExactly(secondLaunchEntry());
    }

    @Test
    void shouldListTheIdsItLogs() {
        assertThat(launchLogOf(launchEntry(), secondLaunchEntry()).ids()).containsExactlyInAnyOrder(sessionId(), secondSessionId());
    }

    @Test
    void shouldFindTheEntryOfAnId() {
        assertThat(launchLogOf(launchEntry(), secondLaunchEntry()).entryOf(secondSessionId())).contains(secondLaunchEntry());
    }

    @Test
    void shouldFindNoEntryInAnEmptyLog() {
        assertThat(emptyLaunchLog().entryOf(sessionId())).isEmpty();
    }
}
