package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.maxenceng.lazyclaude.board.domain.ProjectLocator;
import dev.maxenceng.lazyclaude.board.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/** Finds the main worktree through `git`, so a ticket worktree or subdirectory leads to the same project. */
public final class GitProjectLocator implements ProjectLocator {

    private static final String BACKLOG = "docs/backlog";

    @Override
    public Project locate(Path from) {
        Path start = from.toAbsolutePath().normalize();
        if (!Files.isDirectory(start)) {
            throw new ProjectNotFoundException(start + " is not a directory");
        }
        Path commonDir = commonDir(start);
        Path root = commonDir.getFileName() != null && commonDir.getFileName().toString().equals(".git")
                ? commonDir.getParent()
                : null;
        if (root == null) {
            throw new ProjectNotFoundException("the repository at " + commonDir + " has no main worktree");
        }
        if (!Files.isDirectory(root.resolve(BACKLOG))) {
            throw new ProjectNotFoundException("the main worktree has no " + root.resolve(BACKLOG));
        }
        return new Project(root);
    }

    private static Path commonDir(Path start) {
        Process process;
        try {
            process = new ProcessBuilder("git", "-C", start.toString(), "rev-parse", "--path-format=absolute", "--git-common-dir")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            if (process.waitFor() != 0) {
                throw new ProjectNotFoundException(start + " is not inside a git repository");
            }
            return Path.of(output);
        } catch (IOException e) {
            throw new ProjectNotFoundException("git could not be run: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProjectNotFoundException("interrupted while asking git for the repository", e);
        }
    }
}
