package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WatchServiceStateFixture.limitReached;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WatchServiceStateFixture.notWatchingForLimit;
import static dev.maxenceng.lazyclaude.shared.infrastructure.secondary.WatchServiceStateFixture.watching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.WatchService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchServiceStateTest {

    @Mock
    private WatchServiceFactory services;

    @Mock
    private WatchService service;

    @Test
    void shouldBeWatchingTheServiceTheFactoryMade() throws IOException {
        when(services.create()).thenReturn(service);

        assertThat(WatchServiceState.of(services)).isEqualTo(watching(service));
    }

    @Test
    void shouldBeNotWatchingWithTheReasonTheFactoryFailedFor() throws IOException {
        when(services.create()).thenThrow(limitReached());

        assertThat(WatchServiceState.of(services))
                .as("a machine that cannot watch says why, so the owner can tell the person who has to raise the limit")
                .isEqualTo(notWatchingForLimit());
    }
}
