package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.sidecar.SessionSidecarFixture.sessionExited;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.noProject;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.notification;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.observedRepaintGap;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ready;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.request;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.resultOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

@Timeout(120)
class SidecarProtocolTest {

    @TempDir
    Path root;

    @Test
    void writes_ready_as_its_first_line() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            assertThat(sidecar.nextLine()).isEqualTo(ready());
        }
    }

    @Test
    void ends_with_exit_code_zero_when_stdin_closes() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            assertThat(sidecar.closeStdinAndAwaitExit()).isZero();
        }
    }

    @Test
    void answers_the_board_of_the_project_read_through_gh() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            sidecar.nextLine();
            sidecar.send(request(1, "board/boardFor", "{\"from\":\"%s\"}".formatted(sidecar.projectPath())));

            assertThat(sidecar.nextLine())
                    .startsWith("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"kind\":\"board\"")
                    .contains("\"id\":\"7\"", "\"title\":\"A fixture ticket\"");
        }
    }

    @Test
    void answers_a_watch_with_the_board_and_then_pushes_it_again_when_the_issues_cannot_be_checked() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            sidecar.nextLine();
            sidecar.send(request(1, "board/watch", "{\"from\":\"%s\"}".formatted(sidecar.projectPath())));

            assertThat(List.of(sidecar.nextLine(), sidecar.nextLine()))
                    .as("the watch's answer comes before any change it pushes")
                    .satisfiesExactly(
                            line -> assertThat(line).startsWith("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"kind\":\"board\""),
                            line -> assertThat(line).startsWith("{\"jsonrpc\":\"2.0\",\"method\":\"board/changed\",\"params\":{\"kind\":\"board\""));
        }
    }

    @Test
    void answers_the_diff_of_a_pull_request_read_through_gh() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            sidecar.nextLine();
            sidecar.send(request(1, "review/diffOf", "{\"root\":\"%s\",\"number\":23}".formatted(sidecar.projectPath())));

            assertThat(sidecar.nextLine())
                    .startsWith("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"kind\":\"readDiff\"")
                    .contains("\"src/Fixture.java\"");
        }
    }

    @Test
    void answers_a_pending_review_write_gh_refuses_as_a_result() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            sidecar.nextLine();
            sidecar.send(request(1, "review/submit", "{\"root\":\"%s\",\"number\":23,\"summary\":\"Looks close.\"}".formatted(sidecar.projectPath())));

            assertThat(sidecar.nextLine())
                    .as("a refusal is the write's outcome, so it is a result and never a JSON-RPC error")
                    .startsWith("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"kind\":\"refusedWrite\",\"reason\":");
        }
    }

    @Test
    void answers_a_directory_in_no_project_as_a_result() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            sidecar.nextLine();
            sidecar.send(request(2, "board/boardFor", "{\"from\":\"/no/such/directory\"}"));

            assertThat(sidecar.nextLine()).isEqualTo(resultOf(2, noProject("/no/such/directory is not a directory")));
        }
    }

    @Test
    void writes_nothing_but_json_rpc_messages_to_stdout() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            sidecar.send("not json");
            sidecar.send(request(1, "no/such", "{}"));
            sidecar.send(request(2, "board/boardFor", "{\"from\":\"/no/such/directory\"}"));
            sidecar.closeStdinAndAwaitExit();

            assertThat(sidecar.remainingLines()).hasSize(4).allSatisfy(line -> assertThat(line).startsWith("{\"jsonrpc\":\"2.0\""));
        }
    }

    @Test
    void prints_the_protocol_schema_and_nothing_else_for_protocol_schema() throws Exception {
        assertThat(SpawnedSidecar.protocolSchemaPrinted()).isEqualTo(Sidecar.protocolSchema());
    }

    @Test
    void hosts_a_claude_that_echoes_what_is_sent_to_its_pty() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            String session = sidecar.startedSession();

            sidecar.send(notification("pty/input", "{\"session\":\"%s\",\"data\":\"eHl6\"}".formatted(session)));

            assertThat(sidecar.awaitOutputContaining("xyz")).contains("ready");
        }
    }

    @Test
    void resizes_the_pty_of_a_hosted_claude_to_the_columns_and_rows_it_is_sent() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            String session = sidecar.startedSession();

            sidecar.send(notification("pty/resize", "{\"session\":\"%s\",\"cols\":90,\"rows\":20}".formatted(session)));

            assertThat(sidecar.awaitResizes(1)).extracting(StubResize::size).containsExactly("20 90");
        }
    }

    @Test
    void repaints_by_shrinking_the_pty_one_row_and_restoring_it_after_the_gap() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            String session = sidecar.startedSession();

            sidecar.send(notification("session/repaint", "{\"session\":\"%s\"}".formatted(session)));

            List<StubResize> resizes = sidecar.awaitResizes(2);
            assertThat(resizes).extracting(StubResize::size).containsExactly("29 100", "30 100");
            assertThat(resizes.get(1).at().minus(resizes.get(0).at()))
                    .as("two resizes with no gap repaint nothing (ADR 0049), and the stub saw one after the other")
                    .isGreaterThanOrEqualTo(observedRepaintGap());
        }
    }

    @Test
    void pushes_the_exit_code_of_a_claude_that_exits_by_itself() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            String session = sidecar.startedSession();

            sidecar.send(notification("pty/input", "{\"session\":\"%s\",\"data\":\"cQ==\"}".formatted(session)));

            assertThat(sidecar.awaitLine(line -> line.contains("\"method\":\"session/exited\"")))
                    .isEqualTo(sessionExited(session, 7));
        }
    }

    @Test
    void ends_every_hosted_claude_when_stdin_closes_and_logs_no_end_for_it() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject(root)) {
            sidecar.startedSession();

            assertThat(sidecar.closeStdinAndAwaitExit()).isZero();

            assertThat(sidecar.claudeIsAlive()).as("closing stdin ends the claude it hosted").isFalse();
            assertThat(sidecar.remainingLines()).noneMatch(line -> line.contains("session/exited"));
            assertThat(sidecar.launchLogLines())
                    .as("ADR 0048: a session cut off by the frontend closing is logged as interrupted, with no end")
                    .singleElement()
                    .satisfies(line -> assertThat(SidecarJson.parse(line).path("endedAt").isString()).isFalse());
        }
    }
}
