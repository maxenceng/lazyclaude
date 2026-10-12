package dev.maxenceng.lazyclaude.session.domain;

/** The code a hosted {@code claude} exited with; zero is a clean exit. */
public record ExitCode(int value) {

    private static final int CLEAN = 0;

    public boolean isFailure() {
        return value != CLEAN;
    }
}
