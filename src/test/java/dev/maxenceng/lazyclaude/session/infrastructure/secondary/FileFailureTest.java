package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

import org.junit.jupiter.api.Test;

class FileFailureTest {

    @Test
    void shouldSayAccessWasDeniedRatherThanOnlyNameThePath() {
        assertThat(FileFailure.causeOf(new AccessDeniedException("/home/max/.cache/lazyclaude")))
                .isEqualTo("access denied to /home/max/.cache/lazyclaude");
    }

    @Test
    void shouldSayAFileIsMissingRatherThanOnlyNameThePath() {
        assertThat(FileFailure.causeOf(new NoSuchFileException("/home/max/.claude/projects")))
                .isEqualTo("no such file /home/max/.claude/projects");
    }

    @Test
    void shouldKeepTheMessageOfAnyOtherFailure() {
        assertThat(FileFailure.causeOf(new IOException("No space left on device"))).isEqualTo("No space left on device");
    }
}
