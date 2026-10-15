package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogFixture.backlogIn;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogFixture.ticketMarkdown;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GitFixture {

    private GitFixture() {}

    public static Path repositoryWithoutBacklog(Path directory) throws Exception {
        Path repository = Files.createDirectories(directory).toRealPath();
        git(repository, "init", "-q", "-b", "main");
        return repository;
    }

    public static Path repositoryWithBacklog(Path directory) throws Exception {
        Path repository = repositoryWithoutBacklog(directory);
        Files.writeString(backlogIn(repository).resolve("PROJ-1.md"), ticketMarkdown());
        git(repository, "add", ".");
        git(repository, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "init");
        return repository;
    }

    public static String missingGit() {
        return "lazyclaude-no-such-git";
    }

    public static Path linkedWorktree(Path repository, Path at) throws Exception {
        git(repository, "worktree", "add", "-q", "-b", "PROJ-1-ticket", at.toString());
        return at.toRealPath();
    }

    public static Path linkedWorktreeOn(Path repository, Path at, String branch) throws Exception {
        git(repository, "worktree", "add", "-q", "-b", branch, at.toString());
        return at.toRealPath();
    }

    public static Path removedWorktreeOn(Path repository, Path at, String branch) throws Exception {
        Path linked = linkedWorktreeOn(repository, at, branch);
        try (var paths = Files.walk(linked)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
        return linked;
    }

    public static Path detachedWorktree(Path repository, Path at) throws Exception {
        git(repository, "worktree", "add", "-q", "--detach", at.toString());
        return at.toRealPath();
    }

    public static void branchWithBacklogFile(Path repository, String branch, String file, String content) throws Exception {
        git(repository, "checkout", "-q", "-b", branch);
        Files.writeString(backlogIn(repository).resolve(file), content);
        git(repository, "add", ".");
        git(repository, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "on " + branch);
        git(repository, "checkout", "-q", "main");
    }

    public static void branchWithFile(Path repository, String branch, String file, String content) throws Exception {
        git(repository, "checkout", "-q", "-b", branch);
        Path path = repository.resolve(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        git(repository, "add", ".");
        git(repository, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "on " + branch);
        git(repository, "checkout", "-q", "main");
    }

    public static void branchAtHead(Path repository, String branch) throws Exception {
        git(repository, "branch", branch);
    }

    public static void tag(Path repository, String name, String target) throws Exception {
        git(repository, "tag", name, target);
    }

    public static void branchPointingNowhere(Path repository, String branch) throws IOException {
        Files.writeString(repository.resolve(".git").resolve("refs").resolve("heads").resolve(branch), "1".repeat(40) + "\n");
    }

    public static void removeBacklog(Path repository) throws IOException {
        Path backlog = repository.resolve("docs").resolve("backlog");
        try (var files = Files.list(backlog)) {
            for (Path file : files.toList()) {
                Files.delete(file);
            }
        }
        Files.delete(backlog);
    }

    private static void git(Path in, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", in.toString()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(output).isZero();
    }
}
