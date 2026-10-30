package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.shared.domain.Project;

public final class FakeGhFixture {

    private static final Duration PATIENCE = Duration.ofSeconds(10);
    private static final Duration IMPATIENCE = Duration.ofMillis(300);

    private FakeGhFixture() {}

    static Project projectAt(Path directory) {
        return new Project(directory);
    }

    static IssuesRepository repositoryOver(Path gh) {
        return new IssuesRepository(new GhCommand(gh.toString(), PATIENCE));
    }

    static IssuesRepository impatientRepositoryOver(Path gh) {
        return new IssuesRepository(new GhCommand(gh.toString(), IMPATIENCE));
    }

    static IssuesRepository repositoryOverMissingGh() {
        return new IssuesRepository(new GhCommand(missingCommand(), PATIENCE));
    }

    static String missingCommand() {
        return "lazyclaude-no-such-gh";
    }

    static Path ghPrinting(Path directory, String output) throws IOException {
        return script(directory, "cat <<'JSON'\n%s\nJSON\n".formatted(output));
    }

    static Path ghFailing(Path directory, int exitCode, String said) throws IOException {
        return script(directory, "echo '%s' >&2\nexit %d\n".formatted(said, exitCode));
    }

    static Path ghHanging(Path directory) throws IOException {
        return script(directory, "exec sleep 30\n");
    }

    static Path ghPrintingItsDirectory(Path directory) throws IOException {
        return script(directory, "printf '[{\"number\":1,\"title\":\"%s\",\"body\":\"\",\"labels\":[],\"state\":\"OPEN\",\"stateReason\":\"\"}]' \"$(pwd)\"\n");
    }

    static Path ghPrintingItsArguments(Path directory) throws IOException {
        return script(directory, "printf '[{\"number\":1,\"title\":\"%s\",\"body\":\"\",\"labels\":[],\"state\":\"OPEN\",\"stateReason\":\"\"}]' \"$*\"\n");
    }

    static String listOfIssues(String... issues) {
        return Stream.of(issues).collect(Collectors.joining(",", "[", "]"));
    }

    static String issueJson(int number, String title, String state, String reason, String... labels) {
        String named = Stream.of(labels).map("{\"name\":\"%s\"}"::formatted).collect(Collectors.joining(","));
        return "{\"number\":%d,\"title\":\"%s\",\"body\":\"Body.\",\"labels\":[%s],\"state\":\"%s\",\"stateReason\":\"%s\"}"
                .formatted(number, title, named, state, reason);
    }

    private static Path script(Path directory, String body) throws IOException {
        Path script = Files.createTempFile(directory, "gh", ".sh");
        Files.writeString(script, "#!/bin/sh\n" + body, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));
        return script;
    }
}
