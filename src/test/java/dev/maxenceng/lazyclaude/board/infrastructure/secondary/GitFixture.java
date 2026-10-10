package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogFixture.backlogIn;
import static dev.maxenceng.lazyclaude.board.infrastructure.secondary.BacklogFixture.ticketMarkdown;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    public static Path linkedWorktree(Path repository, Path at) throws Exception {
        git(repository, "worktree", "add", "-q", "-b", "PROJ-1-ticket", at.toString());
        return at.toRealPath();
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
