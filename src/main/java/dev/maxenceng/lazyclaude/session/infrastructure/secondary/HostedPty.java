package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

import dev.maxenceng.lazyclaude.session.domain.ExitCode;
import dev.maxenceng.lazyclaude.session.domain.HostUnavailableException;
import dev.maxenceng.lazyclaude.session.domain.PtyBytes;
import dev.maxenceng.lazyclaude.session.published.PaneSize;
import dev.maxenceng.lazyclaude.session.published.ProcessId;

/** One {@code claude} on a pty: the process, its output read as bytes and handed on unread, and the bytes written to it as they come. */
final class HostedPty {

    private static final String READER_NAME = "pty-%d";
    private static final String START_FAILURE = "claude could not be started: %s";
    private static final long GRACE_SECONDS = 1;
    private static final int READ_SIZE = 8192;
    private static final int END_OF_OUTPUT = -1;

    private final PtyProcess process;

    private HostedPty(PtyProcess process) {
        this.process = process;
    }

    /**
     * Starts {@code argv} on a pty of {@code size} in {@code directory}.
     *
     * @throws HostUnavailableException when the process cannot be started
     */
    static HostedPty spawn(List<String> argv, Map<String, String> environment, Path directory, PaneSize size) {
        return new HostedPty(started(argv, environment, directory, size));
    }

    private static PtyProcess started(List<String> argv, Map<String, String> environment, Path directory, PaneSize size) {
        try {
            return new PtyProcessBuilder(argv.toArray(String[]::new))
                    .setEnvironment(environment)
                    .setDirectory(directory.toString())
                    .setInitialColumns(size.width().cells())
                    .setInitialRows(size.height().cells())
                    .start();
        } catch (IOException | RuntimeException failure) {
            throw new HostUnavailableException(START_FAILURE.formatted(reason(failure)), failure);
        }
    }

    private static String reason(Exception failure) {
        return Objects.requireNonNullElse(failure.getMessage(), failure.getClass().getSimpleName());
    }

    ProcessId pid() {
        return new ProcessId(process.pid());
    }

    /** Starts reading the child's output on a thread of its own, which ends by telling {@code onExit} the exit code. */
    void listen(Consumer<PtyBytes> onOutput, Consumer<ExitCode> onExit) {
        Thread.ofPlatform().daemon().name(READER_NAME.formatted(process.pid())).start(() -> read(onOutput, onExit));
    }

    private void read(Consumer<PtyBytes> onOutput, Consumer<ExitCode> onExit) {
        try {
            InputStream output = process.getInputStream();
            byte[] buffer = new byte[READ_SIZE];
            int read = output.read(buffer);
            while (read != END_OF_OUTPUT) {
                onOutput.accept(new PtyBytes(Arrays.copyOf(buffer, read)));
                read = output.read(buffer);
            }
        } catch (IOException closed) {
            // the pty closing under the reader is how a child's end shows up, so the exit code below is the news
        } finally {
            // a child still running once nothing reads it — its output closed, or the listener threw — is a tab nothing closes
            end();
            onExit.accept(new ExitCode(exitCode()));
        }
    }

    private int exitCode() {
        try {
            return process.waitFor();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return process.isAlive() ? -1 : process.exitValue();
        }
    }

    void write(PtyBytes bytes) {
        if (bytes.isEmpty()) {
            return;
        }
        try {
            OutputStream input = process.getOutputStream();
            input.write(bytes.value());
            input.flush();
        } catch (IOException gone) {
            // the pty is closed, and the reader ends the child and closes its tab; what was typed at it has nowhere to go
        }
    }

    void resize(PaneSize size) {
        try {
            process.setWinSize(new WinSize(size.width().cells(), size.height().cells()));
        } catch (IllegalStateException closed) {
            // pty4j's answer for a pty whose child just exited; the reader is about to close its tab
        }
    }

    /** Asks the child to end, and makes sure of it when it does not within a moment. */
    void end() {
        process.destroy();
        try {
            if (!process.waitFor(GRACE_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }
}
