package dev.maxenceng.lazyclaude.sidecar;

import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningViewFixture.twoWarningsView;
import static dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningViewFixture.warningView;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.linesWritten;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.ready;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.request;
import static dev.maxenceng.lazyclaude.sidecar.SidecarFixture.resultOf;
import static dev.maxenceng.lazyclaude.sidecar.WarningSidecarFixture.noWarning;
import static dev.maxenceng.lazyclaude.sidecar.WarningSidecarFixture.oneWarning;
import static dev.maxenceng.lazyclaude.sidecar.WarningSidecarFixture.twoWarnings;
import static dev.maxenceng.lazyclaude.sidecar.WarningSidecarFixture.warningsAtOnce;
import static dev.maxenceng.lazyclaude.sidecar.WarningSidecarFixture.warningsChanged;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.shared.infrastructure.primary.NoWarningView;
import dev.maxenceng.lazyclaude.shared.infrastructure.primary.WarningQuery;

@ExtendWith(MockitoExtension.class)
class WarningSidecarTest {

    @Mock
    private WarningQuery warnings;

    @Test
    void answers_the_warning_held_to_a_watch() {
        when(warnings.held()).thenReturn(warningView());

        assertThat(linesWritten(warnings, request(1, "warning/watch", "{}"))).containsExactly(ready(), resultOf(1, oneWarning()));
    }

    @Test
    void answers_that_none_is_held_to_a_watch() {
        when(warnings.held()).thenReturn(new NoWarningView());

        assertThat(linesWritten(warnings, request(1, "warning/watch", "{}"))).containsExactly(ready(), resultOf(1, noWarning()));
    }

    @Test
    void pushes_the_next_warning_after_the_answer() {
        when(warnings.held()).thenReturn(warningView());
        doAnswer(warningsAtOnce(twoWarningsView())).when(warnings).watch(any());

        assertThat(linesWritten(warnings, request(1, "warning/watch", "{}")))
                .as("a change pushed before the watch's answer would be overwritten by the older warning it carries")
                .containsExactly(ready(), resultOf(1, oneWarning()), warningsChanged(twoWarnings()));
    }

    @Test
    void pushes_that_none_is_held_when_the_last_one_is_withdrawn() {
        when(warnings.held()).thenReturn(warningView());
        doAnswer(warningsAtOnce(new NoWarningView())).when(warnings).watch(any());

        assertThat(linesWritten(warnings, request(1, "warning/watch", "{}")))
                .containsExactly(ready(), resultOf(1, oneWarning()), warningsChanged(noWarning()));
    }

    @Test
    void opens_no_second_watch_on_a_second_request_to_watch() {
        when(warnings.held()).thenReturn(new NoWarningView());

        linesWritten(warnings, request(1, "warning/watch", "{}"), request(2, "warning/watch", "{}"));

        verify(warnings, times(1)).watch(any());
    }
}
