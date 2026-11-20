package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/** Runs {@code git} in a directory and waits for it. A run that ends non-zero is a result; only a run that cannot happen is a failure. */
public final class GitCommand {

    // A reader blocks until the process exits, so each gets its own virtual thread rather than a slot of the shared pool.
    private static final Executor READERS = task -> Thread.ofVirtual().start(task);
    private static final String IN_DIRECTORY = "-C";

    private final String executable;
    private final Map<String, String> environment;

    public GitCommand(String executable) {
        this(executable, Map.of());
    }

    /** Runs {@code executable} with {@code environment} added to what lazyclaude was started with. */
    public GitCommand(String executable, Map<String, String> environment) {
        this.executable = executable;
        this.environment = environment;
    }

    /** @throws GitNotRunException when git cannot be run, or the run is interrupted */
    public GitResult run(Path in, List<String> arguments) {
        try {
            ProcessBuilder builder =
                    new ProcessBuilder(Stream.concat(Stream.of(executable, IN_DIRECTORY, in.toString()), arguments.stream()).toList());
            builder.environment().putAll(environment);
            Process process = builder.start();
            CompletableFuture<byte[]> said = CompletableFuture.supplyAsync(() -> readAll(process), READERS);
            byte[] output = process.getInputStream().readAllBytes();
            int exitValue = process.waitFor();
            return new GitResult(exitValue, output, said.get());
        } catch (IOException e) {
            throw new GitNotRunException("%s could not be run: %s".formatted(executable, e.getMessage()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitNotRunException("interrupted while waiting for %s".formatted(executable), e);
        } catch (ExecutionException e) {
            throw new GitNotRunException("cannot read what %s said: %s".formatted(executable, e.getMessage()), e);
        }
    }

    private static byte[] readAll(Process process) {
        try {
            return process.getErrorStream().readAllBytes();
        } catch (IOException e) {
            throw new GitNotRunException("cannot read what git said: %s".formatted(e.getMessage()), e);
        }
    }
}
