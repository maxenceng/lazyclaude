package dev.maxenceng.lazyclaude.shared.application;

import static dev.maxenceng.lazyclaude.shared.domain.WarningsFixture.twoWarnings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.shared.domain.Warning;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.Warnings;

@ExtendWith(MockitoExtension.class)
class WarningApplicationServiceTest {

    @Mock
    private WarningWatchPort watch;

    @Mock
    private WarningPort warnings;

    @InjectMocks
    private WarningApplicationService service;

    @Test
    void shouldHandTheListenerTheWarningsRaisedSoFar() {
        List<Warnings> received = new ArrayList<>();

        service.subscribe(received::add);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Warnings>> listener = ArgumentCaptor.forClass(Consumer.class);
        verify(watch).subscribe(listener.capture());
        listener.getValue().accept(twoWarnings());

        assertThat(received).containsExactly(twoWarnings());
    }

    @Test
    void shouldTellTheListenerWhenNoWarningIsHeldAnyMore() {
        Runnable listener = mock(Runnable.class);

        service.onNoneHeld(listener);
        ArgumentCaptor<Runnable> watched = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).onNoneHeld(watched.capture());
        watched.getValue().run();

        verify(listener).run();
    }

    @Test
    void shouldRaiseAWarningForAThreadThatStopped() {
        service.threadStopped("session-watch", "java.lang.IllegalStateException: gone");

        verify(warnings)
                .raise(new Warning(
                        "session-watch stopped (java.lang.IllegalStateException: gone); what it kept current no longer updates."));
    }

    @Test
    void shouldAnswerWhatTheWatchHolds() {
        when(watch.held()).thenReturn(Optional.of(twoWarnings()));

        assertThat(service.held()).contains(twoWarnings());
    }
}
