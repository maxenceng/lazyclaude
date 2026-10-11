package dev.maxenceng.lazyclaude.shared.application;

import static dev.maxenceng.lazyclaude.shared.domain.WarningsFixture.twoWarnings;
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

import dev.maxenceng.lazyclaude.shared.domain.WarningWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.Warnings;

@ExtendWith(MockitoExtension.class)
class WarningApplicationServiceTest {

    @Mock
    private WarningWatchPort watch;

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
}
