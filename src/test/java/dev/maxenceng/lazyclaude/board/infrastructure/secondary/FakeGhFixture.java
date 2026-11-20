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
    private static final String TITLE_AS_ISSUE =
            "sed 's|.*|[{\"data\":{\"repository\":{\"issues\":{\"nodes\":[{\"number\":1,\"title\":\"&\",\"body\":\"\",\"labels\":{\"nodes\":[]},\"state\":\"OPEN\",\"stateReason\":null}]}}}}]|'\n";

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
        return script(directory, "printf '%s' \"$(pwd)\" | " + TITLE_AS_ISSUE);
    }

    static Path ghPrintingItsArguments(Path directory) throws IOException {
        return script(directory, "printf '%s' \"$*\" | " + TITLE_AS_ISSUE);
    }

    static String listOfIssues(String... issues) {
        return pagesOf(pageOf(issues));
    }

    static String pagesOf(String... pages) {
        return Stream.of(pages).collect(Collectors.joining(",", "[", "]"));
    }

    static String pageOf(String... issues) {
        return "{\"data\":{\"repository\":{\"issues\":{\"nodes\":[%s]}}}}".formatted(String.join(",", issues));
    }

    static String issueJson(int number, String title, String state, String reason, String... labels) {
        return issueJsonLinkedTo(number, title, state, reason, "", labels);
    }

    static String issueJsonLinkedTo(int number, String title, String state, String reason, String branch, String... labels) {
        String named = Stream.of(labels).map("{\"name\":\"%s\"}"::formatted).collect(Collectors.joining(","));
        String linked = branch.isEmpty() ? "" : "{\"ref\":{\"name\":\"%s\"}}".formatted(branch);
        return ("{\"number\":%d,\"title\":\"%s\",\"body\":\"Body.\",\"labels\":{\"nodes\":[%s]},\"state\":\"%s\","
                        + "\"stateReason\":%s,\"closedAt\":%s,\"linkedBranches\":{\"nodes\":[%s]}}")
                .formatted(
                        number,
                        title,
                        named,
                        state,
                        reason.isEmpty() ? "null" : "\"%s\"".formatted(reason),
                        "CLOSED".equals(state) ? "\"2026-10-01T09:00:00Z\"" : "null",
                        linked);
    }

    static String issueJsonClosedByMerging(int number, String title, String headRefName) {
        return ("{\"number\":%d,\"title\":\"%s\",\"state\":\"CLOSED\",\"stateReason\":\"COMPLETED\",\"closedAt\":\"2026-10-01T09:00:00Z\","
                        + "\"linkedBranches\":{\"nodes\":[]},"
                        + "\"timelineItems\":{\"nodes\":[{\"closer\":{\"headRefName\":\"%s\",\"merged\":true}}]}}")
                .formatted(number, title, headRefName);
    }

    private static Path script(Path directory, String body) throws IOException {
        Path script = Files.createTempFile(directory, "gh", ".sh");
        Files.writeString(script, "#!/bin/sh\n" + body, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));
        return script;
    }
}
