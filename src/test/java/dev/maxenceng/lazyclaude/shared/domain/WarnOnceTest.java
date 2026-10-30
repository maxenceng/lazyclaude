package dev.maxenceng.lazyclaude.shared.domain;

import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.otherWarning;
import static dev.maxenceng.lazyclaude.shared.domain.WarningFixture.warning;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    void shouldWithdrawTheWarningItRaised() {
        WarnOnce once = new WarnOnce();
        once.raise(warnings, warning());

        once.withdraw(warnings);

        verify(warnings).withdraw(warning());
    }

    @Test
    void shouldWithdrawNothingWhenNothingWasRaised() {
        new WarnOnce().withdraw(warnings);

        verifyNoInteractions(warnings);
    }

    @Test
    void shouldWithdrawTheWarningOnlyOnce() {
        WarnOnce once = new WarnOnce();
        once.raise(warnings, warning());
        once.withdraw(warnings);

        once.withdraw(warnings);

        verify(warnings, times(1)).withdraw(warning());
    }

    @Test
    void shouldRaiseAgainAfterTheWarningWasWithdrawn() {
        WarnOnce once = new WarnOnce();
        once.raise(warnings, warning());
        once.withdraw(warnings);

        once.raise(warnings, otherWarning());

        verify(warnings).raise(otherWarning());
    }
}
