package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureViewFixture.capturePayload;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureViewFixture.capturedTicketView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureViewFixture.refusedTicketCaptureView;
import static dev.maxenceng.lazyclaude.sidecar.CaptureSidecarFixture.captureParams;
import static dev.maxenceng.lazyclaude.sidecar.CaptureSidecarFixture.capturedOn;
import static dev.maxenceng.lazyclaude.sidecar.CaptureSidecarFixture.noBoardAfterCapture;
import static dev.maxenceng.lazyclaude.sidecar.CaptureSidecarFixture.refusedBecauseGhFailed;
import static dev.maxenceng.lazyclaude.sidecar.CaptureSidecarFixture.refusedBecauseOfTheReason;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.afterChangeOverABoard;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.afterChangeOverAReason;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.linesWritten;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.payload;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ready;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.releasingOnceSubscribed;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.request;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.resultOf;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ticketlessBoard;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.waitingFor;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.watchProject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardView;
import dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketCaptureCommand;

@ExtendWith(MockitoExtension.class)
class CaptureSidecarTest {

    @Mock
    private BoardQuery board;

    @Mock
    private TicketCaptureCommand capturing;

    @Test
    void answers_a_captured_ticket_with_the_board_read_once_github_took_it() {
        when(board.boardAfterChange(afterChangeOverAReason())).thenReturn(Optional.of(boardViewOf()));
        when(capturing.capture(capturePayload())).thenReturn(capturedTicketView());

        assertThat(linesWritten(board, capturing, request(1, "board/capture", captureParams())))
                .as("the frontend selects the new ticket on the board its answer carries, so no push is the capture's")
                .containsExactly(ready(), resultOf(1, capturedOn(ticketlessBoard())));
    }

    @Test
    void answers_a_refused_capture_as_a_result() {
        when(capturing.capture(capturePayload())).thenReturn(refusedTicketCaptureView());

        assertThat(linesWritten(board, capturing, request(1, "board/capture", captureParams())))
                .as("a refusal is what the capture came to, which the popup shows, not a failure of the call")
                .containsExactly(ready(), resultOf(1, refusedBecauseOfTheReason()));
    }

    @Test
    void answers_a_capture_that_threw_as_a_refusal_saying_what_it_threw() {
        when(capturing.capture(capturePayload())).thenThrow(new IllegalStateException("gh is not installed"));

        assertThat(linesWritten(board, capturing, request(1, "board/capture", captureParams())))
                .as("the shell shows a thrown capture as a refusal, so the popup stays and says why")
                .containsExactly(ready(), resultOf(1, refusedBecauseGhFailed()));
    }

    @Test
    void reads_the_board_after_a_capture_against_the_board_a_watched_project_last_sent() {
        BoardView shown = boardViewOf();
        when(board.boardFor(payload())).thenReturn(shown);
        when(board.boardAfterChange(afterChangeOverABoard())).thenReturn(Optional.of(shown));
        CountDownLatch watching = new CountDownLatch(1);
        doAnswer(releasingOnceSubscribed(watching)).when(board).subscribe(any(), any());
        when(capturing.capture(capturePayload())).thenAnswer(waitingFor(watching, capturedTicketView()));

        assertThat(linesWritten(board, capturing, watchProject(1), request(2, "board/capture", captureParams())))
                .containsExactly(ready(), resultOf(1, ticketlessBoard()), resultOf(2, capturedOn(ticketlessBoard())));
    }

    @Test
    void answers_a_captured_ticket_with_no_board_when_it_cannot_be_read_after_the_capture() {
        when(board.boardAfterChange(afterChangeOverAReason())).thenReturn(Optional.empty());
        when(capturing.capture(capturePayload())).thenReturn(capturedTicketView());

        assertThat(linesWritten(board, capturing, request(1, "board/capture", captureParams())))
                .as("a failed read leaves the frontend the board it shows, rather than a board resent as if it were new")
                .containsExactly(ready(), resultOf(1, capturedOn(noBoardAfterCapture())));
    }

    @Test
    void reads_no_board_after_a_refused_capture() {
        when(capturing.capture(capturePayload())).thenReturn(refusedTicketCaptureView());

        linesWritten(board, capturing, request(1, "board/capture", captureParams()));

        verify(board, never()).boardAfterChange(any());
    }
}
