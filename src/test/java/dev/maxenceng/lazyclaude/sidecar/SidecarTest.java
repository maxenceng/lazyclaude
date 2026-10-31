package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.unavailableBoardView;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.boardForOtherProject;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.boardForProject;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.changed;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.errorResponse;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.exitCodeOf;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.faultWithCode;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.linesWritten;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.notACall;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.notificationWithoutId;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.otherPayload;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.payload;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ready;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.releasing;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.request;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.resultOf;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.signalAtOnce;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ticketBoard;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ticketlessBoard;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.unavailableBoard;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.unparseableLine;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.waitingFor;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.watchProject;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
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

@ExtendWith(MockitoExtension.class)
class SidecarTest {

    @Mock
    private BoardQuery board;

    @Test
    void says_it_is_ready_before_anything_else() {
        assertThat(linesWritten(board)).containsExactly(ready());
    }

    @Test
    void ends_with_exit_code_zero_when_stdin_closes_at_once() {
        assertThat(exitCodeOf(board)).isZero();
    }

    @Test
    void ignores_an_empty_line() {
        assertThat(linesWritten(board, "", "   ")).containsExactly(ready());
    }

    @Test
    void answers_an_unparseable_line_with_the_parse_error_code() {
        assertThat(linesWritten(board, unparseableLine()).get(1)).startsWith(faultWithCode("null", -32700));
    }

    @Test
    void answers_json_that_is_not_a_call_with_the_invalid_request_code() {
        assertThat(linesWritten(board, notACall()).get(1))
                .isEqualTo(errorResponse("null", -32600, "not a JSON-RPC call: it has no method"));
    }

    @Test
    void answers_an_unknown_method_with_the_method_not_found_code_and_the_requests_id() {
        assertThat(linesWritten(board, request(7, "no/such", "{}")).get(1))
                .isEqualTo(errorResponse("7", -32601, "unknown method: no/such"));
    }

    @Test
    void does_not_answer_a_notification_it_does_not_know() {
        assertThat(linesWritten(board, notificationWithoutId())).containsExactly(ready());
    }

    @Test
    void answers_params_without_a_directory_with_the_invalid_params_code() {
        assertThat(linesWritten(board, request(3, "board/boardFor", "{}")).get(1)).startsWith(faultWithCode("3", -32602));
    }

    @Test
    void answers_params_that_are_not_an_object_with_the_invalid_params_code() {
        assertThat(linesWritten(board, request(3, "board/boardFor", "\"here\"")).get(1)).startsWith(faultWithCode("3", -32602));
    }

    @Test
    void answers_the_board_of_the_project() {
        when(board.boardFor(payload())).thenReturn(boardViewOf());

        assertThat(linesWritten(board, boardForProject(1))).containsExactly(ready(), resultOf(1, ticketlessBoard()));
    }

    @Test
    void writes_a_ticket_with_its_ids_as_strings_and_its_absent_shapes_by_kind() {
        when(board.boardFor(payload())).thenReturn(boardViewOf(ticketView()));

        assertThat(linesWritten(board, boardForProject(1))).containsExactly(ready(), resultOf(1, ticketBoard()));
    }

    @Test
    void answers_an_unavailable_board_as_a_result() {
        when(board.boardFor(payload())).thenReturn(unavailableBoardView());

        assertThat(linesWritten(board, boardForProject(1))).containsExactly(ready(), resultOf(1, unavailableBoard()));
    }

    @Test
    void answers_a_failure_nobody_caught_with_the_internal_code_and_its_message() {
        when(board.boardFor(payload())).thenThrow(new IllegalStateException("the directory is no project"));

        assertThat(linesWritten(board, boardForProject(1)).get(1))
                .isEqualTo(errorResponse("1", -32603, "the directory is no project"));
    }

    @Test
    void answers_the_current_board_to_a_watch_and_pushes_the_board_read_again_on_a_change() {
        BoardView read = boardViewOf();
        when(board.boardFor(payload())).thenReturn(read);
        when(board.boardReadAgainFor(payload())).thenReturn(Optional.of(read));
        doAnswer(signalAtOnce()).when(board).subscribe(any(), any());

        assertThat(linesWritten(board, watchProject(1)))
                .containsExactlyInAnyOrder(ready(), resultOf(1, ticketlessBoard()), changed(ticketlessBoard()));
    }

    @Test
    void opens_no_second_watch_on_a_second_request_to_watch() {
        when(board.boardFor(payload())).thenReturn(boardViewOf());

        assertThat(linesWritten(board, watchProject(1), watchProject(2)).stream().filter(line -> line.contains("\"result\"")))
                .hasSize(2);
        verify(board, times(1)).subscribe(any(), any());
    }

    @Test
    void pushes_nothing_when_the_board_cannot_be_read_again() {
        when(board.boardFor(payload())).thenReturn(boardViewOf());
        when(board.boardReadAgainFor(payload())).thenReturn(Optional.empty());
        doAnswer(signalAtOnce()).when(board).subscribe(any(), any());

        assertThat(linesWritten(board, watchProject(1))).containsExactlyInAnyOrder(ready(), resultOf(1, ticketlessBoard()));
    }

    @Test
    void answers_a_request_before_one_sent_earlier_when_that_one_is_slower() {
        CountDownLatch answeredOther = new CountDownLatch(1);
        when(board.boardFor(payload())).thenAnswer(waitingFor(answeredOther, boardViewOf()));
        when(board.boardFor(otherPayload())).thenAnswer(releasing(answeredOther, boardViewOf()));

        assertThat(linesWritten(board, boardForProject(1), boardForOtherProject(2)))
                .containsExactly(ready(), resultOf(2, ticketlessBoard()), resultOf(1, ticketlessBoard()));
    }
}
