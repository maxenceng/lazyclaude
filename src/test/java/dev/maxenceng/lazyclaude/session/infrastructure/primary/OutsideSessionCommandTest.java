package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.anOutsideSessionInKitty;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.kittyRefusingRemoteControl;
import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.outsideSessionApplicationService;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.allowRemoteControlSetting;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.bothLines;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.conflictingAllowSetting;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.kittyConfigFile;
import static dev.maxenceng.lazyclaude.session.domain.KittyConfigFixture.listenOnSetting;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.inKitty;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.OutsideSessionPayloadFixture.jumpPayload;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.domain.HostPort;
import dev.maxenceng.lazyclaude.session.domain.KittyConfigPort;
import dev.maxenceng.lazyclaude.session.domain.KittySettings;
import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentPort;
import dev.maxenceng.lazyclaude.session.domain.TerminalJumpPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;

@ExtendWith(MockitoExtension.class)
class OutsideSessionCommandTest {

    @Mock
    private LiveSessionPort sessions;

    @Mock
    private ProjectLocatorPort locator;

    @Mock
    private WarningPort warnings;

    @Mock
    private HostPort hosts;

    @Mock
    private ProcessEnvironmentPort environments;

    @Mock
    private TerminalJumpPort jumps;

    @Mock
    private KittyConfigPort kittyConfig;

    private OutsideSessionCommand command;

    @BeforeEach
    void setUp() {
        command = new OutsideSessionCommand(
                outsideSessionApplicationService(sessions, locator, warnings, hosts, environments, jumps, kittyConfig));
    }

    @Test
    void shouldOfferNothingWhenTheJumpWorked() {
        anOutsideSessionInKitty(sessions, locator, environments);

        assertThat(command.jump(jumpPayload())).isEmpty();

        verify(jumps).jump(inKitty());
    }

    @Test
    void shouldViewTheLinesToAddWhenKittyRefusesRemoteControl() {
        anOutsideSessionInKitty(sessions, locator, environments);
        kittyRefusingRemoteControl(jumps, kittyConfig, new KittySettings(List.of()));

        assertThat(command.jump(jumpPayload()))
                .contains(AddRemoteControlView.builder()
                        .file(Path.of("/home/max/.config/kitty/kitty.conf"))
                        .lines(List.of("allow_remote_control socket-only", "listen_on unix:@kitty-{kitty_pid}"))
                        .build());
    }

    @Test
    void shouldViewTheLineAndFileThatConflict() {
        anOutsideSessionInKitty(sessions, locator, environments);
        kittyRefusingRemoteControl(jumps, kittyConfig, new KittySettings(List.of(conflictingAllowSetting(), listenOnSetting())));

        assertThat(command.jump(jumpPayload()))
                .contains(ConflictingRemoteControlView.builder()
                        .file(Path.of("/home/max/.config/kitty/user.conf"))
                        .line(12)
                        .setting("allow_remote_control yes")
                        .wanted("allow_remote_control socket-only")
                        .build());
    }

    @Test
    void shouldOfferNothingWhenBothKeysAreSetSoKittyNeedsARestart() {
        anOutsideSessionInKitty(sessions, locator, environments);
        kittyRefusingRemoteControl(jumps, kittyConfig, new KittySettings(List.of(
                allowRemoteControlSetting(), listenOnSetting())));

        assertThat(command.jump(jumpPayload())).isEmpty();
    }

    @Test
    void shouldAddTheRemoteControlLinesWhenTheUserSaysYes() {
        when(kittyConfig.locate()).thenReturn(kittyConfigFile());
        when(kittyConfig.settings(kittyConfigFile(), KittySettings.remoteControlKeys())).thenReturn(new KittySettings(List.of()));

        command.addRemoteControl();

        verify(kittyConfig).append(kittyConfigFile(), bothLines());
    }
}
