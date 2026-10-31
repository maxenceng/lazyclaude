package dev.maxenceng.lazyclaude.shell;

/** Runs the shell until the user quits; a seam so that what happens when it ends can be tested without a terminal. */
@FunctionalInterface
interface ShellRun {

    void run() throws Exception;
}
