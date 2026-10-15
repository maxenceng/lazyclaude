package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dev.maxenceng.lazyclaude.review.domain.MergedPullRequest;
import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;
import dev.maxenceng.lazyclaude.shared.domain.Project;

public final class FakeGhFixture {

    private static final Duration PATIENCE = Duration.ofSeconds(10);
    private static final Duration IMPATIENCE = Duration.ofMillis(300);

    private FakeGhFixture() {}

    public static Project projectAt(Path directory) {
        return new Project(directory);
    }

    public static GhPullRequestsRepository repositoryOver(Path gh) {
        return new GhPullRequestsRepository(gh.toString(), PATIENCE);
    }

    public static GhPullRequestsRepository impatientRepositoryOver(Path gh) {
        return new GhPullRequestsRepository(gh.toString(), IMPATIENCE);
    }

    public static GhPullRequestsRepository repositoryOverMissingGh() {
        return new GhPullRequestsRepository(missingCommand(), PATIENCE);
    }

    public static List<PullRequest> readPrinted(Path directory, String output) throws IOException {
        return repositoryOver(ghPrinting(directory, output)).openPullRequests(projectAt(directory));
    }

    public static CheckOutcome outcomeOfTheOnlyCheck(Path directory, String check) throws IOException {
        List<PullRequest> read = readPrinted(directory, listOfPullRequests(pullRequestJson(1, "t", "b", false, check)));
        return read.getFirst().checks().checks().getFirst().outcome();
    }

    public static GhMergedPullRequestsRepository mergedRepositoryOver(Path gh) {
        return new GhMergedPullRequestsRepository(gh.toString(), PATIENCE);
    }

    public static GhMergedPullRequestsRepository impatientMergedRepositoryOver(Path gh) {
        return new GhMergedPullRequestsRepository(gh.toString(), IMPATIENCE);
    }

    public static GhMergedPullRequestsRepository mergedRepositoryOverMissingGh() {
        return new GhMergedPullRequestsRepository(missingCommand(), PATIENCE);
    }

    public static List<MergedPullRequest> readMergedPrinted(Path directory, String output) throws IOException {
        return mergedRepositoryOver(ghPrinting(directory, output)).mergedPullRequests(projectAt(directory));
    }

    public static Path ghPrinting(Path directory, String output) throws IOException {
        return script(directory, "cat <<'JSON'\n%s\nJSON\n".formatted(output));
    }

    public static Path ghFailing(Path directory, int exitCode, String said) throws IOException {
        return script(directory, "echo '%s' >&2\nexit %d\n".formatted(said, exitCode));
    }

    public static Path ghHanging(Path directory) throws IOException {
        return script(directory, "exec sleep 30\n");
    }

    public static Path ghTitlingWithItsArguments(Path directory) throws IOException {
        return script(directory, "printf '[{\"number\":1,\"title\":\"%s\",\"headRefName\":\"b\",\"isDraft\":false,\"statusCheckRollup\":[]}]' \"$*\"\n");
    }

    public static Path ghTitlingWithItsDirectory(Path directory) throws IOException {
        return script(directory, "printf '[{\"number\":1,\"title\":\"%s\",\"headRefName\":\"b\",\"isDraft\":false,\"statusCheckRollup\":[]}]' \"$(pwd)\"\n");
    }

    public static Path ghMergedWithItsArguments(Path directory) throws IOException {
        return script(directory, "printf '[{\"number\":1,\"headRefName\":\"%s\",\"headRefOid\":\"abc\"}]' \"$*\"\n");
    }

    public static Path ghMergedWithItsDirectory(Path directory) throws IOException {
        return script(directory, "printf '[{\"number\":1,\"headRefName\":\"%s\",\"headRefOid\":\"abc\"}]' \"$(pwd)\"\n");
    }

    public static String mergedPullRequestJson(int number, String head, String oid) {
        return mergedPullRequestJsonFrom(false, number, head, oid);
    }

    public static String forkMergedPullRequestJson(int number, String head, String oid) {
        return mergedPullRequestJsonFrom(true, number, head, oid);
    }

    public static String missingCommand() {
        return "lazyclaude-no-such-gh";
    }

    public static String listOfPullRequests(String... pullRequests) {
        return Arrays.stream(pullRequests).collect(Collectors.joining(",", "[", "]"));
    }

    public static String pullRequestJson(int number, String title, String head, boolean draft, String... checks) {
        return pullRequestJsonFrom(false, number, title, head, draft, checks);
    }

    public static String forkPullRequestJson(int number, String title, String head) {
        return pullRequestJsonFrom(true, number, title, head, false);
    }

    public static String checkRunJson(String name, String status, String conclusion) {
        return """
                {"__typename":"CheckRun","name":"%s","status":"%s","conclusion":"%s","workflowName":"CI"}"""
                .formatted(name, status, conclusion);
    }

    public static String statusContextJson(String context, String state) {
        return """
                {"__typename":"StatusContext","context":"%s","state":"%s"}""".formatted(context, state);
    }

    private static String mergedPullRequestJsonFrom(boolean fork, int number, String head, String oid) {
        return """
                {"number":%d,"headRefName":"%s","headRefOid":"%s","isCrossRepository":%b,"unknown":"ignored"}"""
                .formatted(number, head, oid, fork);
    }

    private static String pullRequestJsonFrom(boolean fork, int number, String title, String head, boolean draft, String... checks) {
        return """
                {"number":%d,"title":"%s","headRefName":"%s","isDraft":%b,"isCrossRepository":%b,"statusCheckRollup":[%s],"unknown":"ignored"}"""
                .formatted(number, title, head, draft, fork, String.join(",", checks));
    }

    private static Path script(Path directory, String body) throws IOException {
        Path script = directory.resolve("gh");
        Files.writeString(script, "#!/bin/sh\n" + body, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
        return script;
    }
}
