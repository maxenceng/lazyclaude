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
class BoardMethodsTest {

    @Mock
    private BoardQuery board;

    @Test
    void serves_exactly_the_methods_it_declares_a_signature_for() {
        List<String> served = List.copyOf(new BoardMethods(board, new SidecarOutput(new ByteArrayOutputStream())).methods().keySet());

        assertThat(served).as("a handler without a signature is missing from the schema, a signature without one cannot be called")
                .containsExactlyInAnyOrderElementsOf(BoardMethods.signatures().stream().map(ProtocolMethod::name).toList());
    }

    @Test
    void declares_the_notification_it_pushes() {
        assertThat(BoardMethods.notifications().stream().map(ProtocolNotification::name)).containsExactly("board/changed");
    }
}
