package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.emptyEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.garbledTmuxPaneEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.inKitty;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.inTmux;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.kittyAndTmuxEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.kittyEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.kittyPidOnlyEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.kittyWithGarbledWindowEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.tmuxEnvironment;
import static dev.maxenceng.lazyclaude.session.domain.ProcessEnvironmentFixture.tmuxPaneOnlyEnvironment;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProcessEnvironmentTest {

    @Test
    void shouldPlaceASessionInTmuxBeforeKittyWhenItCarriesBoth() {
        assertThat(kittyAndTmuxEnvironment().residence())
                .as("a claude in tmux carries the stale KITTY_* of wherever the server was started")
                .isEqualTo(inTmux());
    }

    @Test
    void shouldStillFindTheKittyWindowOfAProcessInTmuxWhenAskedForIt() {
        assertThat(kittyAndTmuxEnvironment().kitty()).contains(inKitty());
    }

    @Test
    void shouldFindNoKittyWindowInAnEnvironmentWithoutOne() {
        assertThat(tmuxEnvironment().kitty()).isEmpty();
    }

    @Test
    void shouldPlaceASessionInKittyWhenItCarriesOnlyKittyVariables() {
        assertThat(kittyEnvironment().residence()).isEqualTo(inKitty());
    }

    @Test
    void shouldPlaceASessionInTmuxWhenItCarriesOnlyTmuxVariables() {
        assertThat(tmuxEnvironment().residence()).isEqualTo(inTmux());
    }

    @Test
    void shouldPlaceASessionNowhereWhenItCarriesNeither() {
        assertThat(emptyEnvironment().residence()).isEqualTo(new Nowhere());
    }

    @Test
    void shouldPlaceASessionInTmuxOnItsPaneAloneWithAnUnknownServer() {
        assertThat(tmuxPaneOnlyEnvironment().residence())
                .isEqualTo(InTmux.builder().pane(new TmuxPane("%7")).server(new UnknownTmuxServer()).build());
    }

    @Test
    void shouldNotPlaceASessionInKittyWithoutAWindow() {
        assertThat(kittyPidOnlyEnvironment().residence()).isEqualTo(new Nowhere());
    }

    @Test
    void shouldNotPlaceASessionInKittyWhenTheWindowIsNotANumber() {
        assertThat(kittyWithGarbledWindowEnvironment().residence()).isEqualTo(new Nowhere());
    }

    @Test
    void shouldNotPlaceASessionInTmuxWhenThePaneIsNotAPaneId() {
        assertThat(garbledTmuxPaneEnvironment().residence()).isEqualTo(new Nowhere());
    }
}
