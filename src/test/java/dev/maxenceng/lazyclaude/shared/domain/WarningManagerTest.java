package dev.maxenceng.lazyclaude.shared.domain;

import static dev.maxenceng.lazyclaude.shared.domain.WarningsFixture.warnings;
import static org.assertj.core.api.Assertions.assertThat;
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
}
