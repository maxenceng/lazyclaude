package dev.maxenceng.lazyclaude.shared.domain;

import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.otherWarning;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.warning;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WarnOnceTest {

    @Mock
    private WarningPort warnings;

    @Test
    void shouldRaiseOnlyTheFirstWarningItIsHanded() {
        WarnOnce once = new WarnOnce();

        once.raise(warnings, warning());
        once.raise(warnings, otherWarning());

        verify(warnings).raise(warning());
        verifyNoMoreInteractions(warnings);
    }
}
