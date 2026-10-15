package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Runs {@code gh pr list} in a project's directory and answers what it printed. Any way {@code gh} fails to answer, a missing binary, a
 * login it lacks or a run it was killed in, is the one unavailable reason, in the words {@code gh} used where it used any.
 */
final class GhCommand {

    static final String SOURCE = "gh pr list";

    private static final int SUCCESS = 0;

    private final String executable;
    private final Duration patience;

    GhCommand(String executable, Duration patience) {
        this.executable = executable;
        this.patience = patience;
    }

    /** @throws PullRequestsUnavailableException when {@code gh} does not answer, saying why */
    byte[] output(Project project, List<String> arguments) {
        Process process = start(project, arguments);
        CompletableFuture<byte[]> output = readInBackground(process.getInputStream());
        CompletableFuture<byte[]> said = readInBackground(process.getErrorStream());
        try {
            if (!process.waitFor(patience.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new PullRequestsUnavailableException(
                        "%s did not answer within %d s".formatted(SOURCE, patience.toSeconds()));
            }
            if (process.exitValue() != SUCCESS) {
                throw new PullRequestsUnavailableException(
                        "%s exited with %d%s".formatted(SOURCE, process.exitValue(), firstLine(said.get(patience.toMillis(), TimeUnit.MILLISECONDS))));
            }
            return output.get(patience.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new PullRequestsUnavailableException("interrupted while waiting for %s".formatted(SOURCE), e);
        } catch (ExecutionException | TimeoutException e) {
            process.destroyForcibly();
            throw new PullRequestsUnavailableException(
                    "cannot read the answer of %s: %s".formatted(SOURCE, e.getMessage()), e);
        }
    }

    private Process start(Project project, List<String> arguments) {
        try {
            return new ProcessBuilder(Stream.concat(Stream.of(executable), arguments.stream()).toList())
                    .directory(project.root().toFile())
                    .start();
        } catch (IOException e) {
            throw new PullRequestsUnavailableException("cannot run %s: %s".formatted(executable, e.getMessage()), e);
        }
    }

    private static CompletableFuture<byte[]> readInBackground(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> readAll(stream));
    }

    private static byte[] readAll(InputStream stream) {
        try (stream) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new PullRequestsUnavailableException("cannot read the answer of %s: %s".formatted(SOURCE, e.getMessage()), e);
        }
    }

    private static String firstLine(byte[] said) {
        return new String(said, StandardCharsets.UTF_8)
                .lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .findFirst()
                .map(": "::concat)
                .orElse("");
    }
}
