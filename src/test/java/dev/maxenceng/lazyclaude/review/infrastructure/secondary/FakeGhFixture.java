package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import static dev.maxenceng.lazyclaude.review.published.PullRequestFieldsFixture.pullRequestNumber;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.review.domain.MergedPullRequest;
import dev.maxenceng.lazyclaude.review.domain.PendingComment;
import dev.maxenceng.lazyclaude.review.domain.PendingReview;
import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.review.domain.ReviewThread;
import dev.maxenceng.lazyclaude.review.domain.ReviewedPullRequest;
import dev.maxenceng.lazyclaude.review.published.CheckOutcome;
import dev.maxenceng.lazyclaude.shared.domain.Project;

public final class FakeGhFixture {

    private static final Duration PATIENCE = Duration.ofSeconds(10);
    private static final Duration IMPATIENCE = Duration.ofMillis(300);
    private static final int LINES_OF_A_LARGE_DIFF = 5000;

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

    public static GhPullRequestDiffRepository diffRepositoryOver(Path gh) {
        return new GhPullRequestDiffRepository(gh.toString(), PATIENCE);
    }

    public static GhPullRequestDiffRepository impatientDiffRepositoryOver(Path gh) {
        return new GhPullRequestDiffRepository(gh.toString(), IMPATIENCE);
    }

    public static GhPullRequestDiffRepository diffRepositoryOverMissingGh() {
        return new GhPullRequestDiffRepository(missingCommand(), PATIENCE);
    }

    public static Path ghDiffingWithItsArguments(Path directory) throws IOException {
        return script(directory, "printf 'diff --git a/x b/x\\n--- a/x\\n+++ b/x\\n@@ -1 +1 @@\\n+%s\\n' \"$*\"\n");
    }

    public static Path ghDiffingWithItsDirectory(Path directory) throws IOException {
        return script(directory, "printf 'diff --git a/x b/x\\n--- a/x\\n+++ b/x\\n@@ -1 +1 @@\\n+%s\\n' \"$(pwd)\"\n");
    }

    public static Path ghRefusingALargeDiff(Path directory) throws IOException {
        return ghFailing(directory, 1, "could not find pull request diff: HTTP 406: Sorry, the diff exceeded the maximum number of lines (20000)");
    }

    public static Path ghPrintingALargeDiff(Path directory) throws IOException {
        return script(
                directory,
                "printf 'diff --git a/x b/x\\n--- a/x\\n+++ b/x\\n@@ -0,0 +1,%d @@\\n'\nseq 1 %d | sed 's/^/+a line longer than a pipe buffer would hold in total /'\n"
                        .formatted(LINES_OF_A_LARGE_DIFF, LINES_OF_A_LARGE_DIFF));
    }

    public static int linesOfALargeDiff() {
        return LINES_OF_A_LARGE_DIFF;
    }

    public static <T> T whileEveryCommonPoolThreadIsBusy(Callable<T> read) throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        List<CompletableFuture<Void>> holders = IntStream.range(0, ForkJoinPool.getCommonPoolParallelism())
                .mapToObj(any -> CompletableFuture.runAsync(() -> awaitQuietly(release), ForkJoinPool.commonPool()))
                .toList();
        try {
            return read.call();
        } finally {
            release.countDown();
            CompletableFuture.allOf(holders.toArray(CompletableFuture[]::new)).join();
        }
    }

    public static GhThreadRepository threadRepositoryOver(Path gh) {
        return new GhThreadRepository(gh.toString(), PATIENCE);
    }

    public static GhThreadRepository impatientThreadRepositoryOver(Path gh) {
        return new GhThreadRepository(gh.toString(), IMPATIENCE);
    }

    public static GhThreadRepository threadRepositoryOverMissingGh() {
        return new GhThreadRepository(missingCommand(), PATIENCE);
    }

    public static List<ReviewThread> readThreadsPrinted(Path directory, String output) throws IOException {
        return threadRepositoryOver(ghPrinting(directory, output)).threads(projectAt(directory), pullRequestNumber());
    }

    public static Path ghAnsweringThreadsWithItsArguments(Path directory) throws IOException {
        return script(
                directory,
                "printf '{\"data\":{\"repository\":{\"pullRequest\":{\"reviewThreads\":{\"nodes\":[{\"id\":\"PRRT_1\",\"path\":\"p\",\"line\":1,\"startLine\":null,\"diffSide\":\"RIGHT\",\"isResolved\":false,\"isOutdated\":false,\"comments\":{\"nodes\":[{\"author\":{\"login\":\"a\"},\"state\":\"SUBMITTED\",\"body\":\"%s\"}]}}]}}}}}' \"$(echo $*)\"\n");
    }

    public static String threadsPageJson(String... threads) {
        return """
                {"data":{"repository":{"pullRequest":{"reviewThreads":{"pageInfo":{"hasNextPage":false,"endCursor":null},"nodes":[%s]}}}}}"""
                .formatted(String.join(",", threads));
    }

    public static String threadJson(String path, String line, String startLine, String side, boolean resolved, boolean outdated, String... comments) {
        return """
                {"id":"PRRT_1","path":%s,"line":%s,"startLine":%s,"diffSide":"%s","isResolved":%b,"isOutdated":%b,"comments":{"nodes":[%s]},"unknown":"ignored"}"""
                .formatted(path == null ? "null" : "\"" + path + "\"", line, startLine, side, resolved, outdated, String.join(",", comments));
    }

    public static String commentJson(String login, String body) {
        return commentJsonIn(login, body, "SUBMITTED");
    }

    public static String pendingCommentJson(String login, String body) {
        return commentJsonIn(login, body, "PENDING");
    }

    private static String commentJsonIn(String login, String body, String state) {
        return """
                {"author":%s,"state":"%s","body":"%s"}"""
                .formatted(login == null ? "null" : "{\"login\":\"" + login + "\"}", state, body);
    }

    public static GhPendingReviewRepository pendingRepositoryOver(Path gh) {
        return new GhPendingReviewRepository(gh.toString(), PATIENCE);
    }

    public static GhPendingReviewRepository impatientPendingRepositoryOver(Path gh) {
        return new GhPendingReviewRepository(gh.toString(), IMPATIENCE);
    }

    public static GhPendingReviewRepository pendingRepositoryOverMissingGh() {
        return new GhPendingReviewRepository(missingCommand(), PATIENCE);
    }

    public static ReviewedPullRequest readPendingPrinted(Path directory, String output) throws IOException {
        return pendingRepositoryOver(ghPrinting(directory, output)).pullRequest(projectAt(directory), pullRequestNumber());
    }

    public static List<PendingComment> readPendingCommentsPrinted(Path directory, String output) throws IOException {
        return ((PendingReview) readPendingPrinted(directory, output).review()).comments();
    }

    public static Path ghAnsweringPendingWithItsArguments(Path directory) throws IOException {
        return script(
                directory,
                "printf '{\"data\":{\"repository\":{\"pullRequest\":{\"id\":\"PR_1\",\"headRefOid\":\"abc\",\"reviews\":{\"nodes\":[{\"id\":\"PRR_1\",\"body\":\"%s\",\"commit\":{\"oid\":\"abc\"}}]},\"reviewThreads\":{\"nodes\":[]}}}}}' \"$(echo $*)\"\n");
    }

    public static Path ghRecordingItsArguments(Path directory, String output) throws IOException {
        return script(directory, "printf '%%s\\0' \"$@\" > '%s'\nprintf '%%s' '%s'\n".formatted(recordedArguments(directory), output));
    }

    public static List<String> argumentsRecordedIn(Path directory) throws IOException {
        return Arrays.asList(Files.readString(recordedArguments(directory), StandardCharsets.UTF_8).split("\0"));
    }

    private static Path recordedArguments(Path directory) {
        return directory.resolve("arguments");
    }

    public static String pendingPageJson(String reviews, String... threads) {
        return """
                {"data":{"repository":{"pullRequest":{"id":"PR_1","headRefOid":"9f3c2e1a0b8d7c6e5f4a3b2c1d0e9f8a7b6c5d4e","reviews":{"nodes":[%s]},"reviewThreads":{"pageInfo":{"hasNextPage":false,"endCursor":null},"nodes":[%s]}}}}}"""
                .formatted(reviews, String.join(",", threads));
    }

    public static String pendingReviewJson(String id, String body, String oid) {
        return """
                {"id":"%s","body":"%s","commit":{"oid":"%s"},"comments":{"nodes":[]}}""".formatted(id, body, oid);
    }

    public static String pendingThreadJson(
            String id, String path, String line, String originalLine, String startLine, String side, boolean outdated, String... comments) {
        return """
                {"id":"%s","path":"%s","line":%s,"originalLine":%s,"startLine":%s,"originalStartLine":null,"diffSide":"%s","isResolved":false,"isOutdated":%b,"comments":{"nodes":[%s]}}"""
                .formatted(id, path, line, originalLine, startLine, side, outdated, String.join(",", comments));
    }

    public static String threadCommentJson(String id, String state, String body) {
        return """
                {"id":"%s","author":{"login":"alice"},"state":"%s","body":"%s"}""".formatted(id, state, body);
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

    private static void awaitQuietly(CountDownLatch release) {
        try {
            release.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Path script(Path directory, String body) throws IOException {
        Path script = directory.resolve("gh");
        Files.writeString(script, "#!/bin/sh\n" + body, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"));
        return script;
    }
}
