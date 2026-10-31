package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.otherProjectPath;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.projectPath;
import static dev.maxenceng.lazyclaude.sidecar.WatchedRootFixture.failingToOpen;
import static dev.maxenceng.lazyclaude.sidecar.WatchedRootFixture.watchedRoot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class WatchedRootTest {

    @Test
    void refuses_another_root_than_the_one_claimed_as_an_invalid_param() {
        WatchedRoot watched = watchedRoot();
        watched.claim(projectPath());

        assertThatThrownBy(() -> watched.claim(otherProjectPath()))
                .isInstanceOfSatisfying(JsonRpcFaultException.class, fault -> assertThat(fault.code()).isEqualTo(ErrorCode.INVALID_PARAMS));
    }

    @Test
    void lets_the_claimed_root_be_claimed_again() {
        WatchedRoot watched = watchedRoot();
        watched.claim(projectPath());

        assertThatCode(() -> watched.claim(projectPath())).doesNotThrowAnyException();
    }

    @Test
    void releases_the_root_of_a_watch_that_failed_to_open() {
        WatchedRoot watched = watchedRoot();
        watched.claim(projectPath());

        assertThatThrownBy(() -> watched.open(failingToOpen(), () -> {})).isInstanceOf(IllegalStateException.class);

        assertThatCode(() -> watched.claim(otherProjectPath()))
                .as("a root whose watch never opened holds nothing, so another project may be watched instead")
                .doesNotThrowAnyException();
    }

    @Test
    void runs_again_on_every_open_after_the_first() {
        WatchedRoot watched = watchedRoot();
        AtomicInteger subscribed = new AtomicInteger();
        AtomicInteger again = new AtomicInteger();

        watched.open(subscribed::incrementAndGet, again::incrementAndGet);
        watched.open(subscribed::incrementAndGet, again::incrementAndGet);
        watched.open(subscribed::incrementAndGet, again::incrementAndGet);

        assertThat(subscribed).hasValue(1);
        assertThat(again).hasValue(2);
    }
}
