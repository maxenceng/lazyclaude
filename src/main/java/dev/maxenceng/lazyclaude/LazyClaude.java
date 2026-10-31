package dev.maxenceng.lazyclaude;

import java.util.List;

/** Composition root: runs the {@link Launch} its arguments pick, the shell, the sidecar or the protocol schema, and exits with its code. */
public final class LazyClaude {

    private LazyClaude() {}

    static void main(String[] arguments) throws Exception {
        System.exit(Launch.from(List.of(arguments)).run());
    }
}
