package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedSessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionViewBuilder;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.published.OpeningPromptFixture.noFirstPrompt;
import static dev.maxenceng.lazyclaude.session.published.OpeningPromptFixture.openingUnder;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.firstPrompt;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.noTitle;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.ticketLink;
import static dev.maxenceng.lazyclaude.session.published.TranscriptReadingFixture.unreadable;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.blankScreens;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellHosting;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnSessions;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnTicketSessionsPanel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.published.StartedHost;

@ExtendWith(MockitoExtension.class)
class ShellSessionCardTest {

    @Mock
    private HostedSessionCommand hosting;

    @Test
    void shouldShowTheCardOfASelectedPastSessionInTheMainView() {
        Shell shell = shellOnSessions(pastSessionsViewOf(List.of(pastSessionView())));

        assertThat(Screen.of(shell).mainViewRows())
                .containsSubsequence(
                        "Refine PROJ-1",
                        "› /ticket start PROJ-1",
                        "",
                        "/work/app",
                        "started 2026-10-10 12:40 · last active 10m ago");
    }

    @Test
    void shouldNameTheLinkedTicketsWithTheirLastVerb() {
        Shell shell = shellOnTicketSessionsPanel(sessionsViewOf(
                List.of(), List.of(pastSessionViewBuilder().links(List.of(ticketLink("PROJ-1", "start"))).build())));

        assertThat(Screen.of(shell).mainViewRows())
                .containsSubsequence("› /ticket start PROJ-1", "", "PROJ-1  start", "/work/app");
    }

    @Test
    void shouldOmitTheTicketLineOfASessionLinkedToNoTicket() {
        Shell shell = shellOnSessions(pastSessionsViewOf(List.of(pastSessionView())));

        assertThat(Screen.of(shell).mainViewRows()).noneMatch(row -> row.startsWith("PROJ-"));
    }

    @Test
    void shouldHeadTheCardWithTheFirstPromptAndOmitTheLineBehindTheMarkWhenTheTranscriptHasNoTitle() {
        Shell shell = shellOnSessions(
                pastSessionsViewOf(List.of(pastSessionViewBuilder().title(firstPrompt()).opening(openingUnder(firstPrompt())).build())));

        assertThat(Screen.of(shell).mainViewRows())
                .containsOnlyOnce("/ticket start PROJ-1")
                .noneMatch(row -> row.startsWith("›"));
    }

    @Test
    void shouldHeadTheCardWithTheShortIdWhenTheTranscriptHoldsNeitherTitleNorPrompt() {
        Shell shell = shellOnSessions(pastSessionsViewOf(List.of(pastSessionViewBuilder().title(noTitle()).opening(noFirstPrompt()).build())));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).isEqualTo(pastSessionView().id().shortValue());
    }

    @Test
    void shouldKeepWhatAnEarlierReadSaidAndAddWhyTheTranscriptCannotBeReadNow() {
        Shell shell = shellOnSessions(pastSessionsViewOf(List.of(pastSessionViewBuilder().reading(unreadable()).build())));

        assertThat(Screen.of(shell).mainViewRows())
                .containsSubsequence(
                        "Refine PROJ-1",
                        "› /ticket start PROJ-1",
                        "transcript unreadable: no registry and no claude",
                        "/work/app",
                        "started 2026-10-10 12:40 · last active 10m ago");
    }

    @Test
    void shouldHeadTheCardOfATranscriptNeverReadWithTheShortIdAndWhyItCannotBeRead() {
        Shell shell = shellOnSessions(pastSessionsViewOf(List.of(
                pastSessionViewBuilder().title(noTitle()).opening(noFirstPrompt()).reading(unreadable()).build())));

        assertThat(Screen.of(shell).mainViewRows())
                .containsSubsequence(pastSessionView().id().shortValue(), "transcript unreadable: no registry and no claude")
                .noneMatch(row -> row.startsWith("›"));
    }

    @Test
    void shouldStillResumeAPastSessionWhoseTranscriptCannotBeReadWithEnter() {
        when(hosting.resume(any())).thenReturn(new StartedHost(hostedSessionIdOf(1)));
        Shell shell = shellHosting(
                pastSessionsViewOf(List.of(pastSessionViewBuilder().reading(unreadable()).build())), hosting, blankScreens());
        shell.press(alt('j'));

        shell.press(enter());

        verify(hosting).resume(any());
    }

    @Test
    void shouldListEnterResumeInTheKeyBarWhileAPastSessionIsSelected() {
        Shell shell = shellOnSessions(pastSessionsViewOf(List.of(pastSessionView())));

        assertThat(Screen.of(shell).lines().getLast()).contains("enter resume", "alt+h/j/k/l focus", "q quit");
    }

    @Test
    void shouldNotListEnterResumeWhileAHostedSessionIsSelected() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(sessionView())));

        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("enter resume");
    }

    @Test
    void shouldDoNothingOnOAndOfferNoJumpInTheKeyBar() {
        Shell shell = shellOnSessions(sessionsViewOf(List.of(sessionView())));

        assertThat(shell.press(key('o'))).as("there is no outside session to bring forward").isFalse();
        assertThat(Screen.of(shell).lines().getLast()).doesNotContain("o jump");
    }
}
