package dev.maxenceng.lazyclaude.session.domain;

import java.time.Duration;

/** Runs a task once, a while from now, on a thread of the timer's own. */
public interface TimerPort {

    void after(Duration delay, Runnable task);
}
