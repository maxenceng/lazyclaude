package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dev.maxenceng.lazyclaude.session.domain.TimerPort;

/** Runs each task on the executor it is given, which names its thread, and logs a task that fails. */
public final class TimerRepository implements TimerPort {

    private static final String FAILED = "a timed task failed";
    private static final System.Logger LOG = System.getLogger(TimerRepository.class.getName());

    private final ScheduledExecutorService executor;

    public TimerRepository(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void after(Duration delay, Runnable task) {
        executor.schedule(() -> logged(task), delay.toNanos(), TimeUnit.NANOSECONDS);
    }

    /** A scheduled task's failure would be kept in a future nothing reads. */
    private static void logged(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException failure) {
            LOG.log(System.Logger.Level.ERROR, FAILED, failure);
        }
    }
}
