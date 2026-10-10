package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.maxenceng.lazyclaude.shared.domain.Project;
import dev.maxenceng.lazyclaude.shared.domain.ProjectLocatorPort;
import dev.maxenceng.lazyclaude.shared.domain.ProjectNotFoundException;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

/** Finds the main worktree through `git`, so a ticket worktree or subdirectory leads to the same project. */
public final class ProjectLocatorRepository implements ProjectLocatorPort {

    private static final String GIT_DIRECTORY = ".git";
    private static final String GIT = "git";
    private static final String IN_DIRECTORY = "-C";
    private static final String REV_PARSE = "rev-parse";
    private static final String ABSOLUTE_PATHS = "--path-format=absolute";
    private static final String COMMON_DIR = "--git-common-dir";
    private static final int SUCCESS = 0;

    @Override
    public Project locate(WorkingDirectory from) {
        Path start = from.path();
        if (!Files.isDirectory(start)) {
            throw new ProjectNotFoundException("%s is not a directory".formatted(start));
        }
        return new Project(mainWorktree(commonDir(start)));
    }

    // deferred: started inside a git submodule, the common dir is `.git/modules/<name>` and no main worktree is found — close it when a project needs to run from one.
    private static Path mainWorktree(Path commonDir) {
        Path name = commonDir.getFileName();
        if (name == null || !name.toString().equals(GIT_DIRECTORY)) {
            throw new ProjectNotFoundException("the repository at %s has no main worktree".formatted(commonDir));
        }
        return commonDir.getParent();
    }

    private static Path commonDir(Path start) {
        try {
            Process process = new ProcessBuilder(GIT, IN_DIRECTORY, start.toString(), REV_PARSE, ABSOLUTE_PATHS, COMMON_DIR)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            if (process.waitFor() != SUCCESS) {
                throw new ProjectNotFoundException("%s is not inside a git repository".formatted(start));
            }
            return Path.of(output);
        } catch (IOException e) {
            throw new ProjectNotFoundException("git could not be run: %s".formatted(e.getMessage()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProjectNotFoundException("interrupted while asking git for the repository", e);
        }
    }
}
