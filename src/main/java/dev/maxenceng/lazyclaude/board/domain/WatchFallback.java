package dev.maxenceng.lazyclaude.board.domain;

import java.time.Duration;

import dev.maxenceng.lazyclaude.board.published.Reason;

/** What a watcher tells the domain when it cannot watch one of the trees the board is read from and checks it on a timer instead. */
@FunctionalInterface
public interface WatchFallback {

    /** Said once for each tree that is given up, with the whole cause in the reader's words and how often the tree is checked now. */
    void polling(Reason cause, Duration every);
}
