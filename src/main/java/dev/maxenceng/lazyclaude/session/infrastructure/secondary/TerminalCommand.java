package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.maxenceng.lazyclaude.session.domain.TerminalJumpFailedException;

/** Runs a command that brings a window forward and says how it ended and what it printed, standard error included. */
final class TerminalCommand {

    // A reader blocks until the process exits, so each gets its own virtual thread rather than a slot of the shared pool.
    private static final Executor READERS = task -> Thread.ofVirtual().start(task);
    private static final String CANNOT_RUN = "cannot run %s: %s";
    private static final String TIMEOUT = "%s did not answer within %d s";
    private static final String INTERRUPTED = "interrupted while waiting for %s";
    private static final String UNREADABLE = "cannot read the answer of %s: %s";

    private final List<String> command;
    private final Duration patience;

    TerminalCommand(List<String> command, Duration patience) {
        this.command = command;
        this.patience = patience;
    }

    String program() {
        return command.getFirst();
    }

    /** @throws TerminalJumpFailedException when the command cannot be run, does not answer in time, or its answer cannot be read */
    CommandOutput run() {
        Process process = start();
        try {
            CompletableFuture<String> printed = CompletableFuture.supplyAsync(() -> readAll(process), READERS);
            if (!process.waitFor(patience.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new TerminalJumpFailedException(TIMEOUT.formatted(program(), patience.toSeconds()));
            }
            return new CommandOutput(process.exitValue(), printed.get(patience.toMillis(), TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new TerminalJumpFailedException(INTERRUPTED.formatted(program()), e);
        } catch (ExecutionException | TimeoutException e) {
            process.destroyForcibly();
            throw new TerminalJumpFailedException(UNREADABLE.formatted(program(), e.getMessage()), e);
        }
    }

    private Process start() {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            process.getOutputStream().close();
            return process;
        } catch (IOException e) {
            throw new TerminalJumpFailedException(CANNOT_RUN.formatted(program(), e.getMessage()), e);
        }
    }

    private String readAll(Process process) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new TerminalJumpFailedException(UNREADABLE.formatted(program(), e.getMessage()), e);
        }
    }
}
