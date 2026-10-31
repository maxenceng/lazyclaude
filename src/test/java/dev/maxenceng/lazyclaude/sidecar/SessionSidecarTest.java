package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.newHostedPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.ptyInputPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.ptyOutputView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.ptyResizePayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.resumeHostedPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.resumeTicketHostedPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.sessionExitView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.ticketHostedPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.typedPtyInputPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.appliedOnReturn;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.changingAtOnce;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.exitingAtOnce;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.newHostedParams;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.noSessions;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.notStartedHost;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.outputtingAtOnce;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.ptyInputParams;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.ptyOutput;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.ptyResizeParams;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.repaintParams;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.resumeHostedParams;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.resumeTicketHostedParams;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.sessionExited;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.sessionsChanged;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.sessionsParams;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.startedHost;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.ticketHostedParams;
import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.typedPtyInputParams;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.failingAfterReleasing;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.linesWritten;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.notification;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ready;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.request;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.resultOf;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.waitingFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.PtyInputPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.RepaintPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.published.NotStartedHost;
import dev.maxenceng.lazyclaude.session.published.StartedHost;

@ExtendWith(MockitoExtension.class)
class SessionSidecarTest {

    @Mock
    private SessionQuery sessions;

    @Mock
    private HostedSessionCommand hosting;

    @Test
    void answers_an_unlinked_start_with_the_started_session() {
        when(hosting.startUnlinked(newHostedPayload())).thenReturn(new StartedHost(sessionId()));

        assertThat(linesWritten(sessions, hosting, request(1, "session/startUnlinked", newHostedParams())))
                .containsExactly(ready(), resultOf(1, startedHost()));
    }

    @Test
    void answers_a_start_the_host_could_not_make_as_a_result() {
        when(hosting.startUnlinked(newHostedPayload())).thenReturn(new NotStartedHost());

        assertThat(linesWritten(sessions, hosting, request(1, "session/startUnlinked", newHostedParams())))
                .containsExactly(ready(), resultOf(1, notStartedHost()));
    }

    @Test
    void answers_a_ticket_start_with_the_started_session() {
        when(hosting.startTicket(ticketHostedPayload(Verb.REVIEW))).thenReturn(new StartedHost(sessionId()));

        assertThat(linesWritten(sessions, hosting, request(1, "session/startTicket", ticketHostedParams())))
                .containsExactly(ready(), resultOf(1, startedHost()));
    }

    @Test
    void answers_a_resume_the_directory_being_gone_refuses_as_a_result() {
        when(hosting.resume(resumeHostedPayload())).thenReturn(new NotStartedHost());

        assertThat(linesWritten(sessions, hosting, request(1, "session/resume", resumeHostedParams())))
                .containsExactly(ready(), resultOf(1, notStartedHost()));
    }

    @Test
    void answers_a_resume_in_the_ticket_worktree_with_the_started_session() {
        when(hosting.resumeInTicketWorktree(resumeTicketHostedPayload())).thenReturn(new StartedHost(sessionId()));

        assertThat(linesWritten(sessions, hosting, request(1, "session/resumeInTicketWorktree", resumeTicketHostedParams())))
                .containsExactly(ready(), resultOf(1, startedHost()));
    }

    @Test
    void writes_the_decoded_bytes_of_pty_input_and_answers_nothing() {
        assertThat(linesWritten(sessions, hosting, notification("pty/input", ptyInputParams()))).containsExactly(ready());

        verify(hosting).write(ptyInputPayload(sessionId()));
    }

    @Test
    void writes_pty_inputs_in_the_order_they_arrive() {
        List<PtyInputPayload> applied = new CopyOnWriteArrayList<>();
        doAnswer(appliedOnReturn(applied)).when(hosting).write(any());

        linesWritten(sessions, hosting, notification("pty/input", ptyInputParams()), notification("pty/input", typedPtyInputParams()));

        assertThat(applied)
                .as("a keystroke written before the one typed ahead of it reorders what the user typed")
                .containsExactly(ptyInputPayload(sessionId()), typedPtyInputPayload(sessionId()));
    }

    @Test
    void resizes_the_pty_to_the_columns_and_rows_and_answers_nothing() {
        assertThat(linesWritten(sessions, hosting, notification("pty/resize", ptyResizeParams()))).containsExactly(ready());

        verify(hosting).resizePty(ptyResizePayload(sessionId()));
    }

    @Test
    void repaints_the_session_and_answers_nothing() {
        assertThat(linesWritten(sessions, hosting, notification("session/repaint", repaintParams()))).containsExactly(ready());

        verify(hosting).repaint(new RepaintPayload(sessionId()));
    }

    @Test
    void pushes_each_read_of_a_pty_as_base64() {
        doAnswer(outputtingAtOnce(ptyOutputView(sessionId()))).when(sessions).onOutput(any());

        assertThat(linesWritten(sessions, hosting)).containsExactlyInAnyOrder(ready(), ptyOutput());
    }

    @Test
    void pushes_the_exit_of_a_session_whose_claude_exited_by_itself() {
        doAnswer(exitingAtOnce(sessionExitView(sessionId(), 3))).when(sessions).onExit(any());

        assertThat(linesWritten(sessions, hosting)).containsExactlyInAnyOrder(ready(), sessionExited(3));
    }

    @Test
    void answers_the_sessions_to_a_watch_and_pushes_the_next_ones() {
        when(sessions.sessionsFor(any())).thenReturn(emptySessionsView());
        doAnswer(changingAtOnce(emptySessionsView())).when(sessions).subscribe(any(), any());

        assertThat(linesWritten(sessions, hosting, request(1, "session/watch", sessionsParams())))
                .as("a change pushed before the watch's answer would be overwritten by the older sessions it carries")
                .containsExactly(ready(), resultOf(1, noSessions()), sessionsChanged(noSessions()));
    }

    @Test
    void opens_no_second_watch_on_a_second_request_to_watch() {
        when(sessions.sessionsFor(any())).thenReturn(emptySessionsView());

        assertThat(linesWritten(sessions, hosting, request(1, "session/watch", sessionsParams()), request(2, "session/watch", sessionsParams())))
                .filteredOn(line -> line.contains("\"result\""))
                .hasSize(2);
        verify(sessions, times(1)).subscribe(any(), any());
    }

    @Test
    void opens_the_watch_again_on_a_request_that_overlaps_one_failing_to_start() {
        CountDownLatch failing = new CountDownLatch(1);
        when(sessions.sessionsFor(any())).thenReturn(emptySessionsView()).thenAnswer(waitingFor(failing, emptySessionsView()));
        doAnswer(failingAfterReleasing(failing)).doNothing().when(sessions).subscribe(any(), any());

        linesWritten(sessions, hosting, request(1, "session/watch", sessionsParams()), request(2, "session/watch", sessionsParams()));

        verify(sessions, times(2).description("a watch that let the second request in before forgetting the failed claim leaves the project unwatched"))
                .subscribe(any(), any());
    }

    @Test
    void ends_every_hosted_claude_when_stdin_closes() {
        linesWritten(sessions, hosting);

        verify(hosting).endAll();
    }
}
