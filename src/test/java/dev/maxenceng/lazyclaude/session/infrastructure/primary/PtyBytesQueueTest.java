package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.PtyBytesFixture.ptyBytesOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class PtyBytesQueueTest {

    @Test
    void shouldReadBackWhatWasOffered() throws Exception {
        PtyBytesQueue queue = new PtyBytesQueue();
        queue.offer(ptyBytesOf("hi"));
        queue.close();

        assertThat(new String(queue.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("hi");
    }

    @Test
    void shouldDropWhatIsOfferedOnceStopped() throws Exception {
        PtyBytesQueue queue = new PtyBytesQueue();
        queue.offer(ptyBytesOf("before"));

        queue.stop();
        queue.offer(ptyBytesOf("after"));

        assertThat(queue.readAllBytes())
                .as("an emulator that died reads nothing more, so nothing it was fed is kept for it")
                .isEmpty();
    }
}
