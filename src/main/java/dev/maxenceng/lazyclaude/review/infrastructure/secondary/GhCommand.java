package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Runs {@code gh} in a project's directory and answers what it printed. Any way {@code gh} fails to answer, a missing binary, a
 * login it lacks or a run it was killed in, is the one unavailable reason, in the words {@code gh} used where it used any.
 */
final class GhCommand {

    // A reader blocks until the process exits, so each gets its own virtual thread rather than a slot of the shared pool.
    private static final Executor READERS = task -> Thread.ofVirtual().start(task);
    private static final int SUCCESS = 0;
    private static final int WORDS_OF_A_COMMAND = 2;

    private final String executable;
    private final Duration patience;

    GhCommand(String executable, Duration patience) {
        this.executable = executable;
        this.patience = patience;
    }

    /** What a call is named in a message: {@code gh} and the two words that say what it does, as in {@code gh pr diff}. */
    static String sourceOf(List<String> arguments) {
        return Stream.concat(Stream.of("gh"), arguments.stream().limit(WORDS_OF_A_COMMAND)).collect(Collectors.joining(" "));
    }

    /** @throws PullRequestsUnavailableException when {@code gh} does not answer, saying why */
    byte[] output(Project project, List<String> arguments) {
        String source = sourceOf(arguments);
        Process process = start(project, arguments);
        CompletableFuture<byte[]> output = readInBackground(process.getInputStream(), source);
        CompletableFuture<byte[]> said = readInBackground(process.getErrorStream(), source);
        try {
            if (!process.waitFor(patience.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new PullRequestsUnavailableException(
                        "%s did not answer within %d s".formatted(source, patience.toSeconds()));
            }
            if (process.exitValue() != SUCCESS) {
                throw new PullRequestsUnavailableException(
                        "%s exited with %d%s".formatted(source, process.exitValue(), firstLine(said.get(patience.toMillis(), TimeUnit.MILLISECONDS))));
            }
            return output.get(patience.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new PullRequestsUnavailableException("interrupted while waiting for %s".formatted(source), e);
        } catch (ExecutionException | TimeoutException e) {
            process.destroyForcibly();
            throw new PullRequestsUnavailableException(
                    "cannot read the answer of %s: %s".formatted(source, e.getMessage()), e);
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

    private static CompletableFuture<byte[]> readInBackground(InputStream stream, String source) {
        return CompletableFuture.supplyAsync(() -> readAll(stream, source), READERS);
    }

    private static byte[] readAll(InputStream stream, String source) {
        try (stream) {
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new PullRequestsUnavailableException("cannot read the answer of %s: %s".formatted(source, e.getMessage()), e);
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
