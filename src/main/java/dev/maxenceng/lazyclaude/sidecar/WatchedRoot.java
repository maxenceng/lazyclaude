package dev.maxenceng.lazyclaude.sidecar;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The one project a group's watch serves, where the adapter behind it holds a single project: the first root watched claims it and a
 * watch of any other is refused, since sharing the adapter would silently stop the first one's pushes. A sidecar serves one project.
 */
// deferred: a frontend cannot watch two projects over one sidecar — close it when a frontend opens a second project, with an adapter per root.
final class WatchedRoot {

    private static final String ANOTHER_ROOT = "%s already watches %s; a sidecar serves one project";

    private final String method;
    private Optional<Path> claimed = Optional.empty();
    private boolean opened;

    WatchedRoot(String method) {
        this.method = method;
    }

    /** Called before the watch answers, so a second root is refused as the request's error rather than after its result. */
    synchronized void claim(Path root) {
        claimed.filter(held -> !held.equals(root)).ifPresent(held -> {
            throw new JsonRpcFaultException(ErrorCode.INVALID_PARAMS, ANOTHER_ROOT.formatted(method, held));
        });
        claimed = Optional.of(root);
    }

    /**
     * Opens the watch the first time and runs {@code again} on every later one. A watch that failed to open releases the root before
     * another watch is let in, so that one tries again, of this root or another.
     */
    synchronized void open(Runnable subscribe, Runnable again) {
        if (opened) {
            again.run();
            return;
        }
        try {
            subscribe.run();
            opened = true;
        } catch (RuntimeException failure) {
            claimed = Optional.empty();
            throw failure;
        }
    }
}
