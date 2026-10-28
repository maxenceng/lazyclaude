package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.newHostedPayload;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedTerminalsFixture.screenContaining;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedTerminalsFixture.serviceOver;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedTerminalsFixture.textOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.TerminalInputFixture.pasted;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.TerminalInputFixture.pressed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.TerminalInputFixture.typed;
import static dev.maxenceng.lazyclaude.session.infrastructure.secondary.PtyHostFixture.hostRunning;
import static dev.maxenceng.lazyclaude.session.published.PaneSizeFixture.resizedPaneSize;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.application.SessionApplicationService;
import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.published.Modifiers;
import dev.maxenceng.lazyclaude.session.published.NamedKey;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.StartedHost;

class HostedTerminalsPtyTest {

    @Test
    void shouldShowWhatIsTypedIntoTheChild() throws Exception {
        HostPort host = hostRunning("exec /bin/cat");
        SessionApplicationService service = serviceOver(host);
        HostedTerminals terminals = new HostedTerminals(service);
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();

        terminals.send(new HostedInputPayload(id, typed('h', Modifiers.none())));
        terminals.send(new HostedInputPayload(id, typed('i', Modifiers.none())));

        assertThat(textOf(screenContaining(terminals, id, "hi"))).startsWith("hi");
        service.endAll();
    }

    @Test
    void shouldGiveTheChildAPasteInsideBracketsWhenItAskedForThem() throws Exception {
        HostPort host = hostRunning("printf '\\033[?2004h'; echo ready; cat -v");
        SessionApplicationService service = serviceOver(host);
        HostedTerminals terminals = new HostedTerminals(service);
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();
        screenContaining(terminals, id, "ready");

        terminals.send(new HostedInputPayload(id, pasted("x")));
        terminals.send(new HostedInputPayload(id, pressed(NamedKey.ENTER)));

        assertThat(textOf(screenContaining(terminals, id, "^[[200~x^[[201~"))).contains("^[[200~x^[[201~");
        service.endAll();
    }

    @Test
    void shouldGiveTheChildTheMouseWhenItAskedForIt() throws Exception {
        HostPort host = hostRunning("printf '\\033[?1000h\\033[?1006h'; echo ready; cat -v");
        SessionApplicationService service = serviceOver(host);
        HostedTerminals terminals = new HostedTerminals(service);
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();
        screenContaining(terminals, id, "ready");

        terminals.send(new HostedInputPayload(id, TerminalInputFixture.leftPressAt(4, 2)));
        terminals.send(new HostedInputPayload(id, pressed(NamedKey.ENTER)));

        assertThat(textOf(screenContaining(terminals, id, "^[[<0;5;3M"))).contains("^[[<0;5;3M");
        service.endAll();
    }

    @Test
    void shouldGiveTheChildItsArrowsAsApplicationKeysWhenItAskedForThem() throws Exception {
        HostPort host = hostRunning("printf '\\033[?1h'; echo ready; cat -v");
        SessionApplicationService service = serviceOver(host);
        HostedTerminals terminals = new HostedTerminals(service);
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();
        screenContaining(terminals, id, "ready");

        terminals.send(new HostedInputPayload(id, pressed(NamedKey.UP)));
        terminals.send(new HostedInputPayload(id, pressed(NamedKey.ENTER)));

        assertThat(textOf(screenContaining(terminals, id, "^[OA"))).contains("^[OA");
        service.endAll();
    }

    @Test
    void shouldAnswerTheChildsTerminalQueryOnItsPty() throws Exception {
        HostPort host = hostRunning("printf '\\033[c'; cat -v");
        SessionApplicationService service = serviceOver(host);
        HostedTerminals terminals = new HostedTerminals(service);
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();
        terminals.send(new HostedInputPayload(id, pressed(NamedKey.ENTER)));

        assertThat(textOf(screenContaining(terminals, id, "^[[?"))).contains("^[[?");
        service.endAll();
    }

    @Test
    void shouldRedrawTheChildAtTheSizeOfTheResizedPane() throws Exception {
        HostPort host = hostRunning("trap 'stty size' WINCH; stty size; while true; do sleep 0.05; done");
        SessionApplicationService service = serviceOver(host);
        HostedTerminals terminals = new HostedTerminals(service);
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();
        screenContaining(terminals, id, "30 100");

        terminals.resize(new ResizePayload(id, resizedPaneSize()));

        assertThat(textOf(screenContaining(terminals, id, "40 120"))).contains("40 120");
        service.endAll();
    }
}
