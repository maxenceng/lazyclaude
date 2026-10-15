package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.session.domain.WorktreeUnavailableException;

/** Runs {@code git} in a directory and waits for it. A run that ends non-zero is a result; only a run that cannot happen is a failure. */
final class GitProcess {

    private static final String IN_DIRECTORY = "-C";

    private final String executable;

    GitProcess(String executable) {
        this.executable = executable;
    }

    /** @throws WorktreeUnavailableException when git cannot be run, or the run is interrupted */
    GitRun run(Path in, List<String> arguments) {
        try {
            Process process = new ProcessBuilder(Stream.concat(Stream.of(executable, IN_DIRECTORY, in.toString()), arguments.stream()).toList())
                    .start();
            CompletableFuture<byte[]> said = CompletableFuture.supplyAsync(() -> readAll(process));
            byte[] output = process.getInputStream().readAllBytes();
            int exitValue = process.waitFor();
            return new GitRun(exitValue, output, said.get());
        } catch (IOException e) {
            throw new WorktreeUnavailableException("%s could not be run: %s".formatted(executable, e.getMessage()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WorktreeUnavailableException("interrupted while waiting for %s".formatted(executable), e);
        } catch (ExecutionException e) {
            throw new WorktreeUnavailableException("cannot read what %s said: %s".formatted(executable, e.getMessage()), e);
        }
    }

    private static byte[] readAll(Process process) {
        try {
            return process.getErrorStream().readAllBytes();
        } catch (IOException e) {
            throw new WorktreeUnavailableException("cannot read what git said: %s".formatted(e.getMessage()), e);
        }
    }
}
