package dev.maxenceng.lazyclaude.shared.infrastructure.secondary;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class GitFixture {

    private GitFixture() {}

    public static Path repositoryWithoutACommit(Path directory) throws Exception {
        Path repository = Files.createDirectories(directory).toRealPath();
        git(repository, "init", "-q", "-b", "main");
        return repository;
    }

    public static Path repositoryWithACommit(Path directory) throws Exception {
        Path repository = repositoryWithoutACommit(directory);
        Path file = repository.resolve("docs").resolve("backlog").resolve("PROJ-1.md");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "A file to commit.");
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
        try (Stream<Path> paths = Files.walk(linked)) {
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

    public static void branchWithFile(Path repository, String branch, String file, String content) throws Exception {
        git(repository, "checkout", "-q", "-b", branch);
        Path path = repository.resolve(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        git(repository, "add", ".");
        git(repository, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "-m", "on " + branch);
        git(repository, "checkout", "-q", "main");
    }

    public static void commitEverything(Path in, String message) throws Exception {
        git(in, "add", ".");
        git(in, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "-q", "--allow-empty", "-m", message);
    }

    public static Path worktreeOnExistingBranch(Path repository, Path at, String branch) throws Exception {
        git(repository, "worktree", "add", "-q", at.toString(), branch);
        return at.toRealPath();
    }

    public static void checkoutNewBranch(Path in, String branch) throws Exception {
        git(in, "checkout", "-q", "-b", branch);
    }

    public static void deleteBranch(Path repository, String branch) throws Exception {
        git(repository, "branch", "-q", "-D", branch);
    }

    public static void packRefs(Path repository) throws Exception {
        git(repository, "pack-refs", "--all");
    }

    public static void removeLinkedWorktree(Path repository, Path worktree) throws Exception {
        git(repository, "worktree", "remove", "--force", worktree.toString());
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

    public static String headOf(Path in) throws Exception {
        return gitOutput(in, "rev-parse", "HEAD").strip();
    }

    public static void remoteBeside(Path repository, Path remote) throws Exception {
        git(remote.getParent(), "init", "-q", "--bare", remote.toString());
        git(repository, "remote", "add", "origin", remote.toString());
    }

    public static void pushHeadAs(Path in, String branch) throws Exception {
        git(in, "push", "-q", "origin", "HEAD:refs/heads/" + branch);
    }

    public static boolean branchExistsIn(Path repository, String branch) throws Exception {
        return !gitOutput(repository, "branch", "--list", branch).isBlank();
    }

    private static String gitOutput(Path in, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", in.toString()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(output).isZero();
        return output;
    }

    private static void git(Path in, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", in.toString()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor()).as(output).isZero();
    }
}
