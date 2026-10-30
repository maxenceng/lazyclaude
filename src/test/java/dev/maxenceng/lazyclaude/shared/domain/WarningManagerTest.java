package dev.maxenceng.lazyclaude.shared.domain;

import static dev.maxenceng.lazyclaude.shared.domain.WarningsFixture.warnings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WarningManagerTest {

    @Mock
    private WarningWatchPort watch;

    @Mock
    private WarningPort warnings;

    @InjectMocks
    private WarningManager manager;

    @Test
    void shouldHandTheListenerWhatTheWatchReports() {
        List<Warnings> received = new ArrayList<>();

        manager.subscribe(received::add);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Warnings>> listener = ArgumentCaptor.forClass(Consumer.class);
        verify(watch).subscribe(listener.capture());
        listener.getValue().accept(warnings());

        assertThat(received).containsExactly(warnings());
    }

    @Test
    void shouldTellTheListenerWhatTheWatchSaysWhenNoWarningIsHeldAnyMore() {
        Runnable listener = mock(Runnable.class);

        manager.onNoneHeld(listener);
        ArgumentCaptor<Runnable> watched = ArgumentCaptor.forClass(Runnable.class);
        verify(watch).onNoneHeld(watched.capture());
        watched.getValue().run();

        verify(listener).run();
    }

    @Test
    void shouldRaiseAWarningNamingTheThreadThatStoppedAndWhy() {
        manager.threadStopped("docs-watch", "java.lang.IllegalStateException: gone");

        verify(warnings)
                .raise(new Warning(
                        "docs-watch stopped (java.lang.IllegalStateException: gone); what it kept current no longer updates."));
    }
}
