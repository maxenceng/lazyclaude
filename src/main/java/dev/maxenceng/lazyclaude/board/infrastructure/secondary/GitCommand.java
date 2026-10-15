package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.domain.BranchCopyUnavailableException;

/** Runs {@code git} in a directory and waits for it. A run that ends non-zero is a result; only a run that cannot happen is a failure. */
final class GitCommand {

    private static final String IN_DIRECTORY = "-C";

    private final String executable;

    GitCommand(String executable) {
        this.executable = executable;
    }

    /** @throws BranchCopyUnavailableException when git cannot be run, or the run is interrupted */
    GitResult run(Path in, List<String> arguments) {
        try {
            Process process = new ProcessBuilder(Stream.concat(Stream.of(executable, IN_DIRECTORY, in.toString()), arguments.stream()).toList())
                    .start();
            CompletableFuture<byte[]> said = CompletableFuture.supplyAsync(() -> readAll(process));
            byte[] output = process.getInputStream().readAllBytes();
            int exitValue = process.waitFor();
            return new GitResult(exitValue, output, said.get());
        } catch (IOException e) {
            throw new BranchCopyUnavailableException("%s could not be run: %s".formatted(executable, e.getMessage()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BranchCopyUnavailableException("interrupted while waiting for %s".formatted(executable), e);
        } catch (ExecutionException e) {
            throw new BranchCopyUnavailableException("cannot read what %s said: %s".formatted(executable, e.getMessage()), e);
        }
    }

    private static byte[] readAll(Process process) {
        try {
            return process.getErrorStream().readAllBytes();
        } catch (IOException e) {
            throw new BranchCopyUnavailableException("cannot read what git said: %s".formatted(e.getMessage()), e);
        }
    }
}
