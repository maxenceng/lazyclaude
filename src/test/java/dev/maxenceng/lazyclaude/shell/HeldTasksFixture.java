package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

final class HeldTasksFixture implements Executor {

    private final Deque<Runnable> held = new ArrayDeque<>();

    @Override
    public void execute(Runnable task) {
        held.push(task);
    }

    void runNewestFirst() {
        while (!held.isEmpty()) {
            held.pop().run();
        }
    }
}
