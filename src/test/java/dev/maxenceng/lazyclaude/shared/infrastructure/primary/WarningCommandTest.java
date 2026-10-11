package dev.maxenceng.lazyclaude.shared.infrastructure.primary;

import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.StoppedThreadPayloadFixture.stoppedDocsWatch;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.shared.application.WarningApplicationService;

@ExtendWith(MockitoExtension.class)
class WarningCommandTest {

    @Mock
    private WarningApplicationService warnings;

    @InjectMocks
    private WarningCommand command;

    @Test
    void shouldNameTheStoppedThreadAndItsFailure() {
        command.threadStopped(stoppedDocsWatch());

        verify(warnings).threadStopped("docs-watch", "java.lang.IllegalStateException: gone");
    }
}
