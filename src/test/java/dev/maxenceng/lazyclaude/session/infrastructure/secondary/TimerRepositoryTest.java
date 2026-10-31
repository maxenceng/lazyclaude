package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.session.domain.HostedSessionFixture.repaintGap;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimerRepositoryTest {

    @Mock
    private ScheduledExecutorService executor;

    @Mock
    private Runnable task;

    @Captor
    private ArgumentCaptor<Runnable> scheduled;

    @Test
    void shouldRunTheTaskOnTheExecutorAfterTheDelay() {
        new TimerRepository(executor).after(repaintGap(), task);

        verify(executor).schedule(scheduled.capture(), eq(repaintGap().toNanos()), eq(TimeUnit.NANOSECONDS));
        scheduled.getValue().run();
        verify(task).run();
    }

    @Test
    void shouldKeepATaskThatFailsFromEscapingTheExecutorUnseen() {
        doThrow(new IllegalStateException("the pty is closed")).when(task).run();
        new TimerRepository(executor).after(repaintGap(), task);
        verify(executor).schedule(scheduled.capture(), eq(repaintGap().toNanos()), eq(TimeUnit.NANOSECONDS));

        assertThatCode(() -> scheduled.getValue().run())
                .as("a scheduled task's exception lands in a future nothing reads, so it is logged instead")
                .doesNotThrowAnyException();
    }
}
