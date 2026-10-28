package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSession;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionRunning;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.hostedSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.noneHosted;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.oneHosted;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openStarting;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.openUnread;
import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.twoHosted;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.emptyLaunchLog;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchEntryOf;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.launchLogOf;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.logUnavailable;
import static dev.maxenceng.lazyclaude.session.domain.LaunchLogFixture.ticketLaunchEntry;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.boardTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.factsRunning;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noTickets;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ran;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ticketRun;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.liveSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.newestSession;
import static dev.maxenceng.lazyclaude.session.domain.LiveSessionFixture.sessionWith;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.interruptedLaunchEntry;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.noTranscripts;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastLaunchEntry;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSession;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionActiveMinutesAgo;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastSessionId;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastTranscriptFacts;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.pastTranscriptFactsActiveMinutesAgo;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.quietPastSessions;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.transcriptsGone;
import static dev.maxenceng.lazyclaude.session.domain.PastSessionFixture.transcriptsRead;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.newestReportedSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSession;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedSessionBuilder;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedWaitingFor;
import static dev.maxenceng.lazyclaude.session.domain.ReportedSessionFixture.reportedWith;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.advancingClock;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsHosting;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.availableSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.hostingSessions;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.liveSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.pastSessionsOf;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.recordUnreadable;
import static dev.maxenceng.lazyclaude.session.domain.SessionsFixture.sessionManager;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.earlierStartedAt;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.now;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.startedAt;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.cacheUnwritable;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.registryUnwatchable;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.transcriptsUnlistable;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.transcriptsUnwatchable;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.aiTitle;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.firstPrompt;
import static dev.maxenceng.lazyclaude.session.published.TicketLinkFixture.ticketLink;
import static dev.maxenceng.lazyclaude.session.published.TranscriptReadingFixture.readable;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.inputNeeded;
import static dev.maxenceng.lazyclaude.session.published.WaitingForFixture.permissionPrompt;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.raisedTexts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingForInput;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class SessionManagerTest {

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private LaunchLogPort log;

    @Mock
    private SessionWatchPort watch;

    @Mock
    private PastSessionPort past;

    @Mock
    private TranscriptWatchPort transcriptWatch;

    @Mock
    private WarningPort warnings;

    @Mock
    private Consumer<AvailableSessions> listener;

    @Test
    void shouldListAHostedSessionAsTheLiveSessionItsRecordDescribes() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));

        assertThat(sessionManager(sessions, log, watch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldNameTheLiveSessionByTheIdItWasLaunchedWith() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));

        AvailableSessions read = sessionManager(sessions, log, watch, warnings)
                .sessionsFor(project(), noTickets(), oneHosted());

        assertThat(read.live())
                .as("the row carries the id lazyclaude chose, which no registry record has to repeat")
                .singleElement()
                .extracting(LiveSession::id)
                .isEqualTo(hostedSession().id());
    }

    @Test
    void shouldReadOnlyTheRecordsOfTheProcessesItHosts() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(sessions.recordOf(secondProcessId())).thenReturn(Optional.of(newestReportedSession()));

        sessionManager(sessions, log, watch, warnings)
                .sessionsFor(project(), noTickets(), twoHosted());

        verify(sessions).recordOf(processId());
        verify(sessions).recordOf(secondProcessId());
    }

    @Test
    void shouldListAHostedSessionWithNoRecordYetAsStarting() {
        when(sessions.recordOf(processId())).thenReturn(Optional.empty());

        assertThat(sessionManager(sessions, log, watch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .as("a claude on its trust dialog has written no record, and that is not a failure")
                .isEqualTo(hostingSessions(List.of(openStarting())));
        verifyNoInteractions(warnings);
    }

    @Test
    void shouldListAHostedSessionWhoseRecordCannotBeReadAsUnknownWithOneWarningNamingWhy() {
        when(sessions.recordOf(processId())).thenThrow(recordUnreadable());

        assertThat(sessionManager(sessions, log, watch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .isEqualTo(hostingSessions(List.of(openUnread())));
        assertThat(raisedTexts(warnings))
                .containsExactly("hosted session 70c67de2: %d.json does not parse: Unexpected end-of-input; its state is unknown.".formatted(processId().value()));
    }

    @Test
    void shouldKeepTheStateOfTheOtherSessionsWhenOneRecordCannotBeRead() {
        when(sessions.recordOf(processId())).thenThrow(recordUnreadable());
        when(sessions.recordOf(secondProcessId())).thenReturn(Optional.of(newestReportedSession()));

        AvailableSessions read = sessionManager(sessions, log, watch, warnings)
                .sessionsFor(project(), noTickets(), twoHosted());

        assertThat(read.hosted())
                .extracting(open -> open.phase().live().map(LiveSession::state))
                .containsExactly(Optional.of(new Unknown()), Optional.of(new Busy()));
    }

    @Test
    void shouldWarnOnceHoweverOftenAnUnreadableRecordIsRead() {
        when(sessions.recordOf(processId())).thenThrow(recordUnreadable());
        SessionManager manager = sessionManager(sessions, log, watch, warnings);

        manager.sessionsFor(project(), noTickets(), oneHosted());
        manager.sessionsFor(project(), noTickets(), oneHosted());

        assertThat(raisedTexts(warnings)).hasSize(1);
    }

    @Test
    void shouldWarnAgainWhenARecordFailsAfterItWasReadAgain() {
        when(sessions.recordOf(processId()))
                .thenThrow(recordUnreadable())
                .thenReturn(Optional.of(reportedSession()))
                .thenThrow(recordUnreadable());
        SessionManager manager = sessionManager(sessions, log, watch, warnings);

        manager.sessionsFor(project(), noTickets(), oneHosted());
        manager.sessionsFor(project(), noTickets(), oneHosted());
        manager.sessionsFor(project(), noTickets(), oneHosted());

        assertThat(raisedTexts(warnings)).hasSize(2);
    }

    @Test
    void shouldLinkATicketLaunchToItsTicketBeforeItsTranscriptExists() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));

        AvailableSessions read = sessionManager(sessions, log, watch, warnings)
                .sessionsFor(project(), boardTickets(), hostedSessionsOf(hostedSessionRunning(Verb.DESIGN)));

        assertThat(read.live()).singleElement().extracting(LiveSession::links)
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly(ticketLink("PROJ-1", "design"));
    }

    @Test
    void shouldMeasureEveryAgeOfOneReadFromTheSameInstant() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));

        AvailableSessions read = new SessionManager(sessions, quietPastSessions(), log, watch, mock(TranscriptWatchPort.class), warnings, advancingClock())
                .sessionsFor(project(), noTickets(), oneHosted());

        assertThat(read.ageOf(liveSession()))
                .as("rows of one read drawn at different instants would disagree on how long ago they started")
                .isEqualTo(read.ageOf(liveSession()));
    }

    @Test
    void shouldOrderNewestStartFirst() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(sessions.recordOf(secondProcessId())).thenReturn(Optional.of(newestReportedSession()));

        AvailableSessions read = sessionManager(sessions, log, watch, warnings)
                .sessionsFor(project(), noTickets(), twoHosted());

        assertThat(read.live()).containsExactly(newestSession(), liveSession());
    }

    @Test
    void shouldKeepTheOrderWhenAStateChanges() {
        when(sessions.recordOf(processId()))
                .thenReturn(Optional.of(reportedWith(ReportedStatus.BUSY)))
                .thenReturn(Optional.of(reportedWith(ReportedStatus.IDLE)))
                .thenReturn(Optional.of(reportedWaitingFor(inputNeeded())));
        when(sessions.recordOf(secondProcessId())).thenReturn(Optional.of(newestReportedSession()));
        SessionManager manager = sessionManager(sessions, log, watch, warnings);

        assertThat(manager.sessionsFor(project(), noTickets(), twoHosted()).live()).containsExactly(newestSession(), sessionWith(new Busy()));
        assertThat(manager.sessionsFor(project(), noTickets(), twoHosted()).live()).containsExactly(newestSession(), sessionWith(new YourTurn()));
        assertThat(manager.sessionsFor(project(), noTickets(), twoHosted()).live())
                .containsExactly(newestSession(), sessionWith(new WaitingForInput(inputNeeded())));
    }

    @Test
    void shouldBeBusyWhenTheStatusIsShell() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedWith(ReportedStatus.SHELL)));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new Busy()))));
    }

    @Test
    void shouldWaitForPermissionOnAPermissionPrompt() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedWaitingFor(permissionPrompt())));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .isEqualTo(liveSessionsOf(List.of(sessionWith(waitingForPermission()))));
    }

    @Test
    void shouldWaitForInputWhenNoReasonIsGiven() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedWaitingFor(new NoWaitingReason())));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new WaitingForInput(new NoWaitingReason())))));
    }

    @Test
    void shouldIgnoreAReasonOnASessionThatIsNotWaiting() {
        when(sessions.recordOf(processId()))
                .thenReturn(Optional.of(reportedSessionBuilder()
                        .status(ReportedStatus.IDLE)
                        .waitingFor(permissionPrompt())
                        .build()));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .as("only a waiting session is blocked, whatever stale reason its record carries")
                .isEqualTo(liveSessionsOf(List.of(sessionWith(new YourTurn()))));
    }

    @Test
    void shouldListAHostedSessionAsTheSessionItsRecordNamesOnceItIsCleared() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSessionBuilder().session(secondSessionId()).build()));

        assertThat(sessionManager(sessions, log, watch, warnings).sessionsFor(project(), noTickets(), oneHosted()).live())
                .as("/clear gives the same process a new session, and the row must follow it to its new transcript")
                .singleElement()
                .extracting(LiveSession::id)
                .isEqualTo(secondSessionId());
    }

    @Test
    void shouldHideThePastSessionAHostedSessionRunsSinceItWasCleared() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSessionBuilder().session(secondSessionId()).build()));
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(launchEntryOf(secondSessionId())));
        when(past.transcriptsOf(any())).thenReturn(transcriptsRead(pastTranscriptFacts(secondSessionId())));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()).past())
                .as("resuming the session a tab runs would put a second claude on its transcript")
                .isEmpty();
    }

    @Test
    void shouldListAHostedSessionAsStartingWhileItsPidHoldsARecordLeftByAnEarlierProcess() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSessionBuilder().startedAt(earlierStartedAt()).build()));

        assertThat(sessionManager(sessions, log, watch, warnings)
                        .sessionsFor(project(), noTickets(), hostedSessionsOf(hostedSessionBuilder().spawnedAt(startedAt()).build()))
                        .hosted())
                .as("a crashed claude's record must not lend its session to the new process the OS gave its pid")
                .singleElement()
                .extracting(OpenHostedSession::phase)
                .isInstanceOf(StartingPhase.class);
    }

    @Test
    void shouldDateAHostedSessionWhoseRecordCannotBeReadFromItsSpawn() {
        when(sessions.recordOf(processId())).thenThrow(recordUnreadable());

        assertThat(sessionManager(sessions, log, watch, warnings)
                        .sessionsFor(project(), noTickets(), hostedSessionsOf(hostedSessionBuilder().spawnedAt(startedAt()).build()))
                        .live())
                .as("an unread session's age must not restart at every read")
                .singleElement()
                .extracting(LiveSession::startedAt)
                .isEqualTo(startedAt());
    }

    @Test
    void shouldHandTheListenerTheSessionsReadAgainWhenTheRegistryChanges() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::oneHosted, listener);
        ArgumentCaptor<Runnable> changed = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), changed.capture(), any());

        changed.getValue().run();

        verify(listener).accept(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldWatchOnlyTheRecordsOfTheProcessesItHosts() {
        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::oneHosted, listener);
        ArgumentCaptor<Predicate<ProcessId>> hosted = ArgumentCaptor.captor();
        verify(watch).watch(hosted.capture(), any(), any());

        assertThat(hosted.getValue().test(processId())).isTrue();
        assertThat(hosted.getValue().test(secondProcessId()))
                .as("a claude in another terminal is nobody's business here")
                .isFalse();
    }

    @Test
    void shouldRaiseAWarningNamingWhyAndHowOftenWhenTheWatchFallsBackToTheSweep() {
        sessionManager(sessions, log, watch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);
        ArgumentCaptor<WatchFallback> fallback = ArgumentCaptor.forClass(WatchFallback.class);
        verify(watch).watch(any(), any(), fallback.capture());

        fallback.getValue().sweepingAlone(registryUnwatchable(), Duration.ofSeconds(2));

        verify(warnings)
                .raise(new Warning(
                        "sessions: cannot watch /home/max/.claude/sessions (User limit of inotify watches reached); checking every 2 s instead."));
    }

    @Test
    void shouldRaiseNothingWhileTheRecordsAreRead() {
        sessionManager(sessions, log, watch, warnings).sessionsFor(project(), noTickets(), noneHosted());
        sessionManager(sessions, log, watch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);

        verifyNoInteractions(warnings);
    }


    @BeforeEach
    void setUp() {
        lenient().when(log.read(any(), any())).thenReturn(emptyLaunchLog());
        lenient().when(past.transcriptsOf(any())).thenReturn(noTranscripts());
    }

    @Test
    void shouldListPastSessionsNewestActivityFirstAfterTheLiveOnes() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(2), pastLaunchEntry(1), pastLaunchEntry(3)));
        when(past.transcriptsOf(any()))
                .thenReturn(transcriptsRead(pastTranscriptFactsActiveMinutesAgo(2, 20), pastTranscriptFactsActiveMinutesAgo(1, 10), pastTranscriptFactsActiveMinutesAgo(3, 30)));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .isEqualTo(availableSessionsOf(
                        List.of(liveSession()),
                        List.of(
                                pastSessionActiveMinutesAgo(1, 10),
                                pastSessionActiveMinutesAgo(2, 20),
                                pastSessionActiveMinutesAgo(3, 30))));
    }

    @Test
    void shouldOrderPastSessionsOfTheSameActivityByIdSoTheRowsDoNotShuffle() {
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(2), pastLaunchEntry(1)));
        when(past.transcriptsOf(any()))
                .thenReturn(transcriptsRead(pastTranscriptFactsActiveMinutesAgo(2, 10), pastTranscriptFactsActiveMinutesAgo(1, 10)));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted()))
                .isEqualTo(pastSessionsOf(List.of(pastSessionActiveMinutesAgo(1, 10), pastSessionActiveMinutesAgo(2, 10))));
    }

    @Test
    void shouldListAnInterruptedSessionFirstEvenWhenAnEndedOneWasActiveLater() {
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(1), interruptedLaunchEntry(2)));
        when(past.transcriptsOf(any()))
                .thenReturn(transcriptsRead(pastTranscriptFactsActiveMinutesAgo(1, 10), pastTranscriptFactsActiveMinutesAgo(2, 50)));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted()).past())
                .as("a session lazyclaude cut off is the one the developer most likely wants back")
                .extracting(PastSession::id, PastSession::interrupted)
                .containsExactly(tuple(pastSessionId(2), true), tuple(pastSessionId(1), false));
    }

    @Test
    void shouldListASessionOnceInTheLiveBlockWhenItsTranscriptIsThereToo() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(launchEntryOf(hostedSession().id()), pastLaunchEntry(1)));
        when(past.transcriptsOf(any()))
                .thenReturn(transcriptsRead(pastTranscriptFacts(hostedSession().id()), pastTranscriptFacts(pastSessionId(1))));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .as("a live session with a transcript is live, not also past")
                .isEqualTo(availableSessionsOf(List.of(liveSession()), List.of(pastSession())));
    }

    @Test
    void shouldHideThePastSessionOfAHostedSessionStillStarting() {
        when(sessions.recordOf(processId())).thenReturn(Optional.empty());
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(launchEntryOf(hostedSession().id())));
        when(past.transcriptsOf(any())).thenReturn(transcriptsRead(pastTranscriptFacts(hostedSession().id())));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted()))
                .as("a resumed session shows once, as its tab, before Claude Code has rewritten its record")
                .isEqualTo(availableSessionsHosting(List.of(), List.of(openStarting())));
    }

    @Test
    void shouldAskForTheTranscriptsOfTheLoggedSessionsOnly() {
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(1), pastLaunchEntry(2)));

        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted());

        verify(past).transcriptsOf(Set.of(pastSessionId(1), pastSessionId(2)));
    }

    @Test
    void shouldListNoPastSessionWhenNothingWasLogged() {
        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted()).past())
                .as("a session started in a plain terminal is in no log, so it never shows")
                .isEmpty();
    }

    @Test
    void shouldHoldBackALoggedSessionWhoseTranscriptIsNotReadYetWithoutDroppingIt() {
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(1)));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted()).past()).isEmpty();
        verify(log, never()).change(any(), any(), any());
    }

    @Test
    void shouldDropFromTheLogASessionWithNoProcessWhoseTranscriptIsGone() {
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(1), pastLaunchEntry(2)));
        when(past.transcriptsOf(any()))
                .thenReturn(LoggedTranscripts.builder()
                        .read(List.of(pastTranscriptFactsActiveMinutesAgo(2, 20)))
                        .gone(List.of(pastSessionId(1)))
                        .build());

        AvailableSessions read = sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted());

        assertThat(read.past()).extracting(PastSession::id).containsExactly(pastSessionId(2));
        ArgumentCaptor<UnaryOperator<LaunchLog>> change = ArgumentCaptor.captor();
        verify(log).change(eq(project()), any(), change.capture());
        assertThat(change.getValue().apply(launchLogOf(pastLaunchEntry(1), pastLaunchEntry(2))).entries())
                .as("claude --resume cannot open a session whose transcript is gone")
                .containsExactly(pastLaunchEntry(2));
    }

    @Test
    void shouldKeepInTheLogASessionWithALiveProcessWhoseTranscriptIsNotWrittenYet() {
        when(sessions.recordOf(processId())).thenReturn(Optional.empty());
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(launchEntryOf(hostedSession().id())));
        when(past.transcriptsOf(any())).thenReturn(transcriptsGone(hostedSession().id()));

        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), oneHosted());

        verify(log, never()).change(any(), any(), any());
    }

    @Test
    void shouldRaiseAWarningNamingWhyWhenTheLogCannotBeRead() {
        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), noTickets(), noneHosted());
        ArgumentCaptor<LaunchLogFallback> fallback = ArgumentCaptor.captor();
        verify(log).read(eq(project()), fallback.capture());

        fallback.getValue().unavailable(logUnavailable());

        assertThat(raisedTexts(warnings))
                .containsExactly("launch log: cannot write /state/lazyclaude/launch.log: Permission denied.");
    }

    @Test
    void shouldListNoPastSessionAndWriteNothingWhenTheLogReadsEmpty() {
        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings)
                        .sessionsFor(project(), noTickets(), noneHosted())
                        .past())
                .isEmpty();
        verify(log, never()).change(any(), any(), any());
    }

    @Test
    void shouldLinkAPastSessionToTheTicketItsLaunchRanBeforeItsTranscriptSaysAnything() {
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(ticketLaunchEntry(1, Verb.DESIGN)));
        when(past.transcriptsOf(any())).thenReturn(transcriptsRead(pastTranscriptFacts(pastSessionId(1))));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets(), noneHosted()).past())
                .singleElement()
                .extracting(PastSession::links)
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly(ticketLink("PROJ-1", "design"));
    }

    @Test
    void shouldMoveAPastSessionsLinkWhenItsTranscriptRanALaterVerb() {
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(ticketLaunchEntry(1, Verb.DESIGN)));
        when(past.transcriptsOf(any()))
                .thenReturn(transcriptsRead(TranscriptFacts.builder()
                                .id(pastSessionId(1))
                                .title(aiTitle())
                                .opening(firstPrompt())
                                .reading(readable())
                                .lastActivity(new LastActivity(now()))
                                .links(ran(ticketRun("start", "PROJ-1")))
                                .build()));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets(), noneHosted()).past())
                .singleElement()
                .extracting(PastSession::links)
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly(ticketLink("PROJ-1", "start"));
    }

    @Test
    void shouldLeaveAPastSessionUnlinkedWhenNothingLinkedIt() {
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(1)));
        when(past.transcriptsOf(any())).thenReturn(transcriptsRead(pastTranscriptFacts(pastSessionId(1))));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets(), noneHosted()))
                .isEqualTo(pastSessionsOf(List.of(pastSession())));
    }

    @Test
    void shouldLinkAResumedHostedSessionByTheLaunchThatBeganItLogged() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(ticketLaunchEntry(hostedSession().id(), Verb.DESIGN)));

        AvailableSessions read = sessionManager(sessions, past, log, watch, transcriptWatch, warnings)
                .sessionsFor(project(), boardTickets(), hostedSessionsOf(hostedSessionBuilder().command(new ResumeClaude(hostedSession().id())).build()));

        assertThat(read.live()).singleElement().extracting(LiveSession::links)
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactly(ticketLink("PROJ-1", "design"));
    }

    @Test
    void shouldReuseTheLastReadOfTheHostedSessionsWhenOnlyTheTranscriptsChange() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(1)));
        when(past.transcriptsOf(any())).thenReturn(transcriptsRead(pastTranscriptFacts(pastSessionId(1))));
        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::oneHosted, listener);
        ArgumentCaptor<Runnable> registryChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), registryChanged.capture(), any());
        ArgumentCaptor<Runnable> transcriptsChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), transcriptsChanged.capture(), any(), any(), any());

        registryChanged.getValue().run();
        transcriptsChanged.getValue().run();

        verify(listener, times(2)).accept(availableSessionsOf(List.of(liveSession()), List.of(pastSession())));
        verify(sessions, times(1)).recordOf(processId());
    }

    @Test
    void shouldNotDropASessionThatOpenedSinceTheLastReadOfTheHostedSessionsWhileItsTranscriptIsNotWritten() {
        when(log.read(eq(project()), any()))
                .thenReturn(emptyLaunchLog(), launchLogOf(launchEntryOf(hostedSession().id())));
        when(past.transcriptsOf(any()))
                .thenReturn(LoggedTranscripts.builder().build(), transcriptsGone(hostedSession().id()));
        AtomicReference<HostedSessions> open = new AtomicReference<>(noneHosted());
        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).watch(project(), noTickets(), open::get, listener);
        ArgumentCaptor<Runnable> registryChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), registryChanged.capture(), any());
        ArgumentCaptor<Runnable> transcriptsChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), transcriptsChanged.capture(), any(), any(), any());

        registryChanged.getValue().run();
        open.set(oneHosted());
        transcriptsChanged.getValue().run();

        verify(log, never()).change(any(), any(), any());
    }

    @Test
    void shouldReadTheRecordsWhenTheTranscriptsChangeBeforeTheyWereRead() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::oneHosted, listener);
        ArgumentCaptor<Runnable> transcriptsChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), transcriptsChanged.capture(), any(), any(), any());

        transcriptsChanged.getValue().run();

        verify(listener).accept(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldHaveTheTranscriptWatchReadTheLoggedSessionsOnly() {
        when(log.read(eq(project()), any())).thenReturn(launchLogOf(pastLaunchEntry(1), pastLaunchEntry(2)));
        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);
        ArgumentCaptor<Supplier<Set<SessionId>>> logged = ArgumentCaptor.captor();
        verify(transcriptWatch).watch(logged.capture(), any(), any(), any(), any());

        assertThat(logged.getValue().get())
                .as("a transcript of a session lazyclaude did not start is never read")
                .containsExactlyInAnyOrder(pastSessionId(1), pastSessionId(2));
    }

    @Test
    void shouldRaiseOneWarningWhenTheTranscriptsRootCannotBeListed() {
        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);
        ArgumentCaptor<RootFallback> root = ArgumentCaptor.forClass(RootFallback.class);
        verify(transcriptWatch).watch(any(), any(), any(), any(), root.capture());

        root.getValue().unlistable(transcriptsUnlistable());
        root.getValue().unlistable(transcriptsUnlistable());

        assertThat(raisedTexts(warnings))
                .containsExactly("past sessions: cannot list /home/max/.claude/projects: access denied to /home/max/.claude/projects; only those already read show.");
    }

    @Test
    void shouldRaiseAWarningNamingTheTreeAndHowOftenWhenTheTranscriptsAreOnlyRestated() {
        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);
        ArgumentCaptor<PollingFallback> fallback = ArgumentCaptor.forClass(PollingFallback.class);
        verify(transcriptWatch).watch(any(), any(), fallback.capture(), any(), any());

        fallback.getValue().polling(transcriptsUnwatchable(), Duration.ofSeconds(5));
        fallback.getValue().polling(transcriptsUnwatchable(), Duration.ofSeconds(5));

        assertThat(raisedTexts(warnings))
                .as("named once, with the sysctl the adapter knows it depends on")
                .containsExactly(
                        "past sessions: cannot watch /home/max/.claude/projects (User limit of inotify watches reached); if the watch limit is reached, raise fs.inotify.max_user_watches; checking every 5 s instead.");
    }

    @Test
    void shouldRaiseOneWarningWhenTheCacheCannotBeWritten() {
        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).watch(project(), noTickets(), HostedSessionFixture::noneHosted, listener);
        ArgumentCaptor<CacheFallback> cache = ArgumentCaptor.forClass(CacheFallback.class);
        verify(transcriptWatch).watch(any(), any(), any(), cache.capture(), any());

        cache.getValue().unwritable(cacheUnwritable());
        cache.getValue().unwritable(cacheUnwritable());

        assertThat(raisedTexts(warnings))
                .containsExactly(
                        "past sessions: cannot write the cache /home/max/.cache/lazyclaude/past-sessions.json: Permission denied; they are read in full on every start.");
    }

    @Test
    void shouldLinkALiveSessionThroughItsTranscriptFoundBySessionId() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.transcriptsOf(any()))
                .thenReturn(transcriptsRead(factsRunning(liveSession().id(), ticketRun("start", "PROJ-1"))));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets(), oneHosted()))
                .isEqualTo(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(ticketLink("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldLeaveALiveSessionWithNoTranscriptYetUnlinked() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));

        assertThat(sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets(), oneHosted()))
                .isEqualTo(liveSessionsOf(List.of(liveSession())));
    }

    @Test
    void shouldListALiveSessionLinkedToTwoTicketsWithBothLinks() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.transcriptsOf(any()))
                .thenReturn(transcriptsRead(factsRunning(liveSession().id(), ticketRun("review", "PROJ-1", "PROJ-2"))));

        AvailableSessions read = sessionManager(sessions, past, log, watch, transcriptWatch, warnings).sessionsFor(project(), boardTickets(), oneHosted());

        assertThat(read.live()).singleElement().extracting(LiveSession::links)
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactlyInAnyOrder(ticketLink("PROJ-1", "review"), ticketLink("PROJ-2", "review"));
    }

    @Test
    void shouldMoveALiveSessionUnderItsTicketOnceItsTranscriptNamesIt() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.transcriptsOf(any()))
                .thenReturn(noTranscripts())
                .thenReturn(transcriptsRead(factsRunning(liveSession().id(), ticketRun("start", "PROJ-1"))));
        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).watch(project(), boardTickets(), HostedSessionFixture::oneHosted, listener);
        ArgumentCaptor<Runnable> registryChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), registryChanged.capture(), any());
        ArgumentCaptor<Runnable> transcriptsChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(transcriptWatch).watch(any(), transcriptsChanged.capture(), any(), any(), any());

        registryChanged.getValue().run();
        transcriptsChanged.getValue().run();

        verify(listener).accept(liveSessionsOf(List.of(liveSession())));
        verify(listener).accept(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(ticketLink("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldDeliverTheSessionsOnRefresh() {
        sessionManager(sessions, log, watch, warnings).refresh(project(), noneHosted(), listener);

        verify(listener).accept(liveSessionsOf(List.of()));
    }

    @Test
    void shouldLinkASessionToATicketLearnedAfterTheWatchStarted() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.transcriptsOf(any()))
                .thenReturn(transcriptsRead(factsRunning(liveSession().id(), ticketRun("start", "PROJ-1"))));
        SessionManager manager = sessionManager(sessions, past, log, watch, transcriptWatch, warnings);
        manager.watch(project(), noTickets(), HostedSessionFixture::oneHosted, listener);

        manager.know(project(), boardTickets(), oneHosted(), listener);

        verify(listener)
                .accept(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(ticketLink("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldKeepLinkingAgainstTheTicketsLearnedOnLaterChangesOfTheRegistry() {
        when(sessions.recordOf(processId())).thenReturn(Optional.of(reportedSession()));
        when(past.transcriptsOf(any()))
                .thenReturn(transcriptsRead(factsRunning(liveSession().id(), ticketRun("start", "PROJ-1"))));
        SessionManager manager = sessionManager(sessions, past, log, watch, transcriptWatch, warnings);
        manager.watch(project(), noTickets(), HostedSessionFixture::oneHosted, listener);
        ArgumentCaptor<Runnable> registryChanged = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).watch(any(), registryChanged.capture(), any());
        manager.know(project(), boardTickets(), oneHosted(), listener);

        registryChanged.getValue().run();

        verify(listener, times(2))
                .accept(liveSessionsOf(List.of(liveSessionBuilder().links(List.of(ticketLink("PROJ-1", "start"))).build())));
    }

    @Test
    void shouldAskTheTranscriptWatchToRescanWhenTranscriptsAreRescanned() {
        sessionManager(sessions, past, log, watch, transcriptWatch, warnings).rescanTranscripts();

        verify(transcriptWatch).rescanNow();
    }
}
