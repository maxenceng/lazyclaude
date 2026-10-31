package dev.maxenceng.lazyclaude.sidecar;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardQuery;

@ExtendWith(MockitoExtension.class)
class SidecarMethodsTest {

    @Mock
    private BoardQuery board;

    @Test
    void serves_exactly_the_methods_the_schema_states() {
        List<String> served = List.copyOf(new Sidecar(board).methods(new SidecarOutput(new ByteArrayOutputStream())).keySet());

        assertThat(served).as("a handler without a declaration is missing from the schema, a declaration without one cannot be called")
                .containsExactlyInAnyOrderElementsOf(Sidecar.protocolMethods().stream().map(ProtocolMethod::name).toList());
    }

    @Test
    void declares_the_notification_it_pushes() {
        assertThat(Sidecar.protocolNotifications().stream().map(ProtocolNotification::name)).containsExactly("board/changed");
    }
}
