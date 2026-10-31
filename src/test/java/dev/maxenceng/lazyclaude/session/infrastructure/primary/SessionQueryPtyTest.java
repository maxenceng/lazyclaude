package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.domain.PtyBytesFixture.escapeBytes;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.serviceOver;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.startedOn;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.ptyOutputView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.sessionExitView;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.HostPort;

@ExtendWith(MockitoExtension.class)
class SessionQueryPtyTest {

    @Mock
    private HostPort hosts;

    @Mock
    private Consumer<PtyOutputView> output;

    @Mock
    private Consumer<SessionExitView> exit;

    @Test
    void shouldHandEachReadOfAPtyToTheOutputListenerWithItsSession() {
        SessionApplicationService service = serviceOver(hosts);
        new SessionQuery(service).onOutput(output);
        RunningSession session = startedOn(hosts, service);

        session.output().accept(escapeBytes());

        verify(output).accept(ptyOutputView(session.id()));
    }

    @Test
    void shouldHandTheCodeOfASessionThatExitedByItselfToTheExitListener() {
        SessionApplicationService service = serviceOver(hosts);
        new SessionQuery(service).onExit(exit);
        RunningSession session = startedOn(hosts, service);

        session.exit().accept(new ExitCode(3));

        verify(exit).accept(sessionExitView(session.id(), 3));
    }

    @Test
    void shouldHandNothingToTheOutputListenerWhenASessionLaunchesOrEnds() {
        SessionApplicationService service = serviceOver(hosts);
        new SessionQuery(service).onOutput(output);
        RunningSession session = startedOn(hosts, service);

        session.exit().accept(new ExitCode(0));

        verifyNoInteractions(output);
    }
}
