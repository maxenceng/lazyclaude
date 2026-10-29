package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorInputPayloadFixture.pasted;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorInputPayloadFixture.pressed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.EmulatorInputPayloadFixture.typed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.screenContaining;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.serviceOver;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedEmulatorFixture.textOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.HostedSessionPayloadFixture.newHostedPayload;
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

class HostedEmulatorCommandPtyTest {

    @Test
    void shouldShowWhatIsTypedIntoTheChild() throws Exception {
        HostPort host = hostRunning("exec /bin/cat");
        SessionApplicationService service = serviceOver(host);
        HostedEmulators running = HostedEmulators.listeningTo(service);
        HostedEmulatorCommand emulating = new HostedEmulatorCommand(service, running);
        HostedEmulatorQuery emulators = new HostedEmulatorQuery(running);
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();

        emulating.send(new HostedInputPayload(id, typed('h', Modifiers.none())));
        emulating.send(new HostedInputPayload(id, typed('i', Modifiers.none())));

        assertThat(textOf(screenContaining(emulators, id, "hi"))).startsWith("hi");
        service.endAll();
    }

    @Test
    void shouldGiveTheChildAPasteInsideBracketsWhenItAskedForThem() throws Exception {
        HostPort host = hostRunning("printf '\\033[?2004h'; echo ready; cat -v");
        SessionApplicationService service = serviceOver(host);
        HostedEmulators running = HostedEmulators.listeningTo(service);
        HostedEmulatorCommand emulating = new HostedEmulatorCommand(service, running);
        HostedEmulatorQuery emulators = new HostedEmulatorQuery(running);
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();
        screenContaining(emulators, id, "ready");

        emulating.send(new HostedInputPayload(id, pasted("x")));
        emulating.send(new HostedInputPayload(id, pressed(NamedKey.ENTER)));

        assertThat(textOf(screenContaining(emulators, id, "^[[200~x^[[201~"))).contains("^[[200~x^[[201~");
        service.endAll();
    }

    @Test
    void shouldGiveTheChildTheMouseWhenItAskedForIt() throws Exception {
        HostPort host = hostRunning("printf '\\033[?1000h\\033[?1006h'; echo ready; cat -v");
        SessionApplicationService service = serviceOver(host);
        HostedEmulators running = HostedEmulators.listeningTo(service);
        HostedEmulatorCommand emulating = new HostedEmulatorCommand(service, running);
        HostedEmulatorQuery emulators = new HostedEmulatorQuery(running);
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();
        screenContaining(emulators, id, "ready");

        emulating.send(new HostedInputPayload(id, EmulatorInputPayloadFixture.leftPressAt(4, 2)));
        emulating.send(new HostedInputPayload(id, pressed(NamedKey.ENTER)));

        assertThat(textOf(screenContaining(emulators, id, "^[[<0;5;3M"))).contains("^[[<0;5;3M");
        service.endAll();
    }

    @Test
    void shouldGiveTheChildItsArrowsAsApplicationKeysWhenItAskedForThem() throws Exception {
        HostPort host = hostRunning("printf '\\033[?1h'; echo ready; cat -v");
        SessionApplicationService service = serviceOver(host);
        HostedEmulators running = HostedEmulators.listeningTo(service);
        HostedEmulatorCommand emulating = new HostedEmulatorCommand(service, running);
        HostedEmulatorQuery emulators = new HostedEmulatorQuery(running);
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();
        screenContaining(emulators, id, "ready");

        emulating.send(new HostedInputPayload(id, pressed(NamedKey.UP)));
        emulating.send(new HostedInputPayload(id, pressed(NamedKey.ENTER)));

        assertThat(textOf(screenContaining(emulators, id, "^[OA"))).contains("^[OA");
        service.endAll();
    }

    @Test
    void shouldAnswerTheChildsTerminalQueryOnItsPty() throws Exception {
        HostPort host = hostRunning("printf '\\033[c'; cat -v");
        SessionApplicationService service = serviceOver(host);
        HostedEmulators running = HostedEmulators.listeningTo(service);
        HostedEmulatorCommand emulating = new HostedEmulatorCommand(service, running);
        HostedEmulatorQuery emulators = new HostedEmulatorQuery(running);
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();
        emulating.send(new HostedInputPayload(id, pressed(NamedKey.ENTER)));

        assertThat(textOf(screenContaining(emulators, id, "^[[?"))).contains("^[[?");
        service.endAll();
    }

    @Test
    void shouldRedrawTheChildAtTheSizeOfTheResizedPane() throws Exception {
        HostPort host = hostRunning("trap 'stty size' WINCH; stty size; while true; do sleep 0.05; done");
        SessionApplicationService service = serviceOver(host);
        HostedEmulators running = HostedEmulators.listeningTo(service);
        HostedEmulatorCommand emulating = new HostedEmulatorCommand(service, running);
        HostedEmulatorQuery emulators = new HostedEmulatorQuery(running);
        SessionId id = ((StartedHost) new HostedSessionCommand(service).startUnlinked(newHostedPayload())).id();
        screenContaining(emulators, id, "30 100");

        emulating.resize(new ResizePayload(id, resizedPaneSize()));

        assertThat(textOf(screenContaining(emulators, id, "40 120"))).contains("40 120");
        service.endAll();
    }
}
