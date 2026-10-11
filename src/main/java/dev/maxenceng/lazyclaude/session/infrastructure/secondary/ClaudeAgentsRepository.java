package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dev.maxenceng.lazyclaude.session.domain.LiveSessionPort;
import dev.maxenceng.lazyclaude.session.domain.ReportedSession;
import dev.maxenceng.lazyclaude.session.domain.SessionsUnavailableException;
import dev.maxenceng.lazyclaude.session.domain.SourceFallback;

/**
 * Asks {@code claude agents --json}, Claude Code's documented listing, which already leaves out dead
 * processes. It reports no version and no entrypoint, so an {@code sdk-cli} session cannot be told apart here.
 */
public final class ClaudeAgentsRepository implements LiveSessionPort {

    private static final String SOURCE = "claude agents --json";
    private static final int SUCCESS = 0;

    private final List<String> command;
    private final Duration patience;

    public ClaudeAgentsRepository(List<String> command, Duration patience) {
        this.command = command;
        this.patience = patience;
    }

    /** The leaf sources have no other to fall back to, so there is nothing to tell {@code fallback}. */
    @Override
    public List<ReportedSession> liveSessions(SourceFallback fallback) {
        return liveSessions();
    }

    public List<ReportedSession> liveSessions() {
        byte[] output = run();
        return SessionRecords.records(output, SOURCE).stream()
                .map(record -> SessionRecords.session(record, SOURCE))
                .toList();
    }

    private byte[] run() {
        Process process = start();
        try {
            CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> readAll(process));
            if (!process.waitFor(patience.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new SessionsUnavailableException("%s did not answer within %d s".formatted(SOURCE, patience.toSeconds()));
            }
            if (process.exitValue() != SUCCESS) {
                throw new SessionsUnavailableException("%s exited with %d".formatted(SOURCE, process.exitValue()));
            }
            return output.get(patience.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new SessionsUnavailableException("interrupted while waiting for %s".formatted(SOURCE), e);
        } catch (ExecutionException | TimeoutException e) {
            process.destroyForcibly();
            throw new SessionsUnavailableException("cannot read the answer of %s: %s".formatted(SOURCE, e.getMessage()), e);
        }
    }

    private Process start() {
        try {
            return new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException e) {
            throw new SessionsUnavailableException("cannot run %s: %s".formatted(command.getFirst(), e.getMessage()), e);
        }
    }

    private static byte[] readAll(Process process) {
        try {
            return process.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new SessionsUnavailableException("cannot read the answer of %s: %s".formatted(SOURCE, e.getMessage()), e);
        }
    }
}
