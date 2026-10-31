package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.emulating;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.emulatorsLeft;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.serviceOver;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.paneSize;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.domain.HostPort;

@ExtendWith(MockitoExtension.class)
class HostedEmulatorsTest {

    @Mock
    private HostPort hosts;

    @Test
    void shouldEndTheEmulatorALaunchOfTheSameSessionReplaces() throws Exception {
        HostedEmulators emulators = emulating(serviceOver(hosts));
        emulators.launching(sessionId(), paneSize());

        emulators.launching(sessionId(), paneSize());

        emulatorsLeft(sessionId(), 1);
    }
}
