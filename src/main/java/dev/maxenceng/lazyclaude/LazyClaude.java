package dev.maxenceng.lazyclaude;

import dev.maxenceng.lazyclaude.shell.Shell;

/** Composition root: the one place that builds adapters and hands them to the shell. */
public final class LazyClaude {

    private LazyClaude() {}

    public static void main(String[] args) throws Exception {
        new Shell().run();
    }
}
