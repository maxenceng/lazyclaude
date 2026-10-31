package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.faultWithCode;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ready;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.request;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(120)
class SidecarProtocolTest {

    @Test
    void writes_ready_as_its_first_line() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject()) {
            assertThat(sidecar.nextLine()).isEqualTo(ready());
        }
    }

    @Test
    void ends_with_exit_code_zero_when_stdin_closes() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject()) {
            assertThat(sidecar.closeStdinAndAwaitExit()).isZero();
        }
    }

    @Test
    void answers_the_board_of_the_project_read_through_gh() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject()) {
            sidecar.nextLine();
            sidecar.send(request(1, "board/boardFor", "{\"from\":\"%s\"}".formatted(sidecar.projectPath())));

            assertThat(sidecar.nextLine())
                    .startsWith("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"kind\":\"board\"")
                    .contains("\"id\":\"7\"", "\"title\":\"A fixture ticket\"");
        }
    }

    @Test
    void answers_a_watch_with_the_board_and_then_pushes_it_again_when_the_issues_cannot_be_checked() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject()) {
            sidecar.nextLine();
            sidecar.send(request(1, "board/watch", "{\"from\":\"%s\"}".formatted(sidecar.projectPath())));

            assertThat(Stream.of(sidecar.nextLine(), sidecar.nextLine()))
                    .anyMatch(line -> line.startsWith("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"kind\":\"board\""))
                    .anyMatch(line -> line.startsWith("{\"jsonrpc\":\"2.0\",\"method\":\"board/changed\",\"params\":{\"kind\":\"board\""));
        }
    }

    @Test
    void answers_a_failure_with_the_internal_code_and_writes_its_stack_trace_to_stderr_only() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject()) {
            sidecar.nextLine();
            sidecar.send(request(2, "board/boardFor", "{\"from\":\"/no/such/directory\"}"));
            String answer = sidecar.nextLine();
            sidecar.closeStdinAndAwaitExit();

            assertThat(answer).startsWith(faultWithCode("2", -32603));
            assertThat(sidecar.stderrText()).contains("ProjectNotFoundException");
        }
    }

    @Test
    void writes_nothing_but_json_rpc_messages_to_stdout() throws Exception {
        try (SpawnedSidecar sidecar = SpawnedSidecar.inFixtureProject()) {
            sidecar.send("not json");
            sidecar.send(request(1, "no/such", "{}"));
            sidecar.send(request(2, "board/boardFor", "{\"from\":\"/no/such/directory\"}"));
            sidecar.closeStdinAndAwaitExit();

            assertThat(sidecar.remainingLines()).hasSize(4).allSatisfy(line -> assertThat(line).startsWith("{\"jsonrpc\":\"2.0\""));
        }
    }
}
