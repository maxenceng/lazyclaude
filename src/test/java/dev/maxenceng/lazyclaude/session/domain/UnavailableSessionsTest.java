package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionRunning;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openUnread;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.startingIn;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessions;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.unavailableSessionsHosting;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.worktreeDirectory;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.SessionName;

class UnavailableSessionsTest {

    @Test
    void shouldKeepTheHostedSessionsOpenAsUnread() {
        Sessions hosting = unavailableSessions().hosting(hostedSessionsOf(hostedSession()));

        assertThat(hosting)
                .as("a registry that cannot be read must not close the tabs of claudes that are running")
                .isEqualTo(unavailableSessionsHosting(List.of(openUnread())));
    }

    @Test
    void shouldFindNoLiveSessionWhenTheRegistryCannotBeRead() {
        assertThat(unavailableSessions().liveSession(sessionId())).isEmpty();
    }

    @Test
    void shouldHaveRunTheVerbAHostedSessionWasLaunchedWithWhateverTheRegistryHolds() {
        Sessions hosting = unavailableSessions().hosting(hostedSessionsOf(hostedSessionRunning(Verb.DESIGN)));

        assertThat(hosting.ranVerb(linkedTicketId(), Verb.DESIGN)).isTrue();
    }

    @Test
    void shouldHaveRunNoVerbWithNoHostedSession() {
        assertThat(unavailableSessions().ranVerb(linkedTicketId(), Verb.DESIGN)).isFalse();
    }

    @Test
    void shouldNameTheHostedSessionsInTheWorktreeWhateverTheRegistryHolds() {
        UnavailableSessions sessions = unavailableSessionsHosting(List.of(startingIn(worktreeDirectory()), openUnread()));

        assertThat(sessions.namesIn(worktreeDirectory()))
                .as("a session lazyclaude started is known to run there even when the registry cannot be read")
                .containsExactly(new SessionName("claude"));
    }

    @Test
    void shouldNameNoSessionWhenNoHostedOneIsInTheWorktree() {
        assertThat(unavailableSessions().namesIn(worktreeDirectory())).isEmpty();
    }
}
