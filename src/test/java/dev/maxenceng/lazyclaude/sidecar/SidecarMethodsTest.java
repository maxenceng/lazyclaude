package dev.maxenceng.lazyclaude.sidecar;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningQuery;

@ExtendWith(MockitoExtension.class)
class SidecarMethodsTest {

    @Mock
    private BoardQuery board;

    @Mock
    private SessionQuery sessions;

    @Mock
    private HostedSessionCommand hosting;

    @Mock
    private WarningQuery warnings;

    @Test
    void serves_exactly_the_methods_and_inputs_the_schema_states() {
        List<String> served = List.copyOf(new Sidecar(board, sessions, hosting, warnings).methods(new SidecarOutput(new ByteArrayOutputStream())).keySet());

        assertThat(served).as("a handler without a declaration is missing from the schema, a declaration without one cannot be called")
                .containsExactlyInAnyOrderElementsOf(
                        Stream.concat(Sidecar.protocolMethods().stream().map(ProtocolMethod::name), Sidecar.protocolInputs().stream().map(ProtocolInput::name))
                                .toList());
    }

    @Test
    void declares_the_notifications_it_pushes() {
        assertThat(Sidecar.protocolNotifications().stream().map(ProtocolNotification::name))
                .containsExactlyInAnyOrder("board/changed", "session/changed", "pty/output", "session/exited", "warning/changed");
    }

    @Test
    void declares_the_calls_it_takes_without_answering() {
        assertThat(Sidecar.protocolInputs().stream().map(ProtocolInput::name))
                .containsExactlyInAnyOrder("pty/input", "pty/resize", "session/repaint");
    }
}
