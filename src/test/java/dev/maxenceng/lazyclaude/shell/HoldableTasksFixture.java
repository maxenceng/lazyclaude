package dev.maxenceng.lazyclaude.shell;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

final class HoldableTasksFixture implements Executor {

    private final Deque<Runnable> held = new ArrayDeque<>();
    private boolean holding;

    @Override
    public void execute(Runnable task) {
        if (holding) {
            held.add(task);
        } else {
            task.run();
        }
    }

    void startHolding() {
        holding = true;
    }

    void releaseInOrder() {
        holding = false;
        while (!held.isEmpty()) {
            held.poll().run();
        }
    }
}
