package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Runs {@code gh} in a project's directory and waits for it. A run that ends non-zero is a result; only one that cannot happen is a failure. */
final class GhCommand {

    private static final String GH = "gh";
    private static final Duration PATIENCE = Duration.ofSeconds(30);
    // A reader blocks until the process exits, so each gets its own virtual thread rather than a slot of the shared pool.
    private static final Executor READERS = task -> Thread.ofVirtual().start(task);

    private final String executable;
    private final Duration patience;

    GhCommand() {
        this(GH, PATIENCE);
    }

    GhCommand(String executable, Duration patience) {
        this.executable = executable;
        this.patience = patience;
    }

    /** @throws BacklogUnavailableException when {@code gh} cannot be run, is interrupted or does not answer in time */
    GhResult run(Project project, List<String> arguments) {
        List<String> command = Stream.concat(Stream.of(executable), arguments.stream()).toList();
        try {
            Process process = new ProcessBuilder(command).directory(project.root().toFile()).start();
            return finished(process);
        } catch (IOException e) {
            throw new BacklogUnavailableException("cannot run %s: %s".formatted(executable, e.getMessage()), e);
        }
    }

    private GhResult finished(Process process) {
        CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> read(process.getInputStream()), READERS);
        CompletableFuture<byte[]> said = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()), READERS);
        try {
            if (!process.waitFor(patience.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new BacklogUnavailableException("%s did not answer within %d s".formatted(executable, patience.toSeconds()));
            }
            return new GhResult(
                    process.exitValue(),
                    output.get(patience.toMillis(), TimeUnit.MILLISECONDS),
                    said.get(patience.toMillis(), TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new BacklogUnavailableException("interrupted while waiting for %s".formatted(executable), e);
        } catch (ExecutionException | TimeoutException e) {
            process.destroyForcibly();
            throw new BacklogUnavailableException("cannot read what %s said: %s".formatted(executable, e.getMessage()), e);
        }
    }

    private static byte[] read(InputStream stream) {
        try (stream) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new BacklogUnavailableException("cannot read what gh said: %s".formatted(e.getMessage()), e);
        }
    }
}
