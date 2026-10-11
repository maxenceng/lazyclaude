package dev.maxenceng.lazyclaude.shared.infrastructure.primary;

import static dev.maxenceng.lazyclaude.shared.domain.WarningsFixture.twoWarnings;
import static dev.maxenceng.lazyclaude.shared.domain.WarningsFixture.warnings;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningViewFixture.twoWarningsView;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningViewFixture.warningView;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.shared.application.WarningApplicationService;
import dev.maxenceng.lazyclaude.shared.domain.WarningPort;
import dev.maxenceng.lazyclaude.shared.domain.WarningWatchPort;
import dev.maxenceng.lazyclaude.shared.domain.Warnings;

@ExtendWith(MockitoExtension.class)
class WarningQueryTest {

    @Mock
    private WarningWatchPort watch;

    @Mock
    private WarningPort warnings;

    @Test
    void shouldHandTheListenerTheNewestWarningAndHowManyThereAre() {
        List<WarningView> received = new ArrayList<>();
        new WarningQuery(new WarningApplicationService(watch, warnings)).subscribe(received::add);

        listener().accept(warnings());
        listener().accept(twoWarnings());

        assertThat(received).containsExactly(warningView(), twoWarningsView());
    }

    Consumer<Warnings> listener() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Warnings>> listener = ArgumentCaptor.forClass(Consumer.class);
        verify(watch).subscribe(listener.capture());
        return listener.getValue();
    }
}
