package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import dev.maxenceng.lazyclaude.session.domain.CacheFallback;
import dev.maxenceng.lazyclaude.session.domain.LastActivity;
import dev.maxenceng.lazyclaude.session.domain.LocatedPastSession;
import dev.maxenceng.lazyclaude.session.domain.PastSession;
import dev.maxenceng.lazyclaude.session.domain.ProjectLocation;
import dev.maxenceng.lazyclaude.session.domain.RootFallback;
import dev.maxenceng.lazyclaude.session.domain.StartedAt;
import dev.maxenceng.lazyclaude.session.domain.TranscriptLocator;
import dev.maxenceng.lazyclaude.session.published.SessionFacts;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

public final class TranscriptFixture {

    private static final String SESSION_ID = "3f2a91c0-5d4e-4b8a-9c11-0e7d2a6b8f10";
    private static final String OTHER_SESSION_ID = "9b1e44d2-77aa-4c3f-8d20-5a6c0b1f2e33";
    private static final String THIRD_SESSION_ID = "33333333-0000-0000-0000-000000000000";
    private static final String CWD = "/work/app";
    private static final String STARTED = "2026-10-11T08:40:00Z";
    private static final String LATER = "2026-10-11T09:12:00Z";
    private static final String BEYOND_EPOCH_MILLIS = "+999999999-12-31T23:59:59Z";
    private static final String LATEST = "2026-10-11T10:30:00Z";
    private static final String FOLDER = "-work-app";

    private TranscriptFixture() {}

    public static String sessionId() {
        return SESSION_ID;
    }

    public static String otherSessionId() {
        return OTHER_SESSION_ID;
    }

    public static String thirdSessionId() {
        return THIRD_SESSION_ID;
    }

    public static String cwd() {
        return CWD;
    }

    public static String folder() {
        return FOLDER;
    }

    public static String started() {
        return STARTED;
    }

    public static String beyondEpochMillis() {
        return BEYOND_EPOCH_MILLIS;
    }

    public static String later() {
        return LATER;
    }

    public static String latest() {
        return LATEST;
    }

    public static String userLine(String cwd, String timestamp, String prompt) {
        return "{\"type\":\"user\",\"timestamp\":\"%s\",\"cwd\":\"%s\",\"message\":{\"role\":\"user\",\"content\":%s}}\n"
                .formatted(timestamp, cwd, quoted(prompt));
    }

    public static String userLineWithBlocks(String cwd, String timestamp, String prompt) {
        return "{\"type\":\"user\",\"timestamp\":\"%s\",\"cwd\":\"%s\",\"message\":{\"role\":\"user\",\"content\":[{\"type\":\"text\",\"text\":%s}]}}\n"
                .formatted(timestamp, cwd, quoted(prompt));
    }

    public static String toolResultLine(String cwd, String timestamp) {
        return "{\"type\":\"user\",\"timestamp\":\"%s\",\"cwd\":\"%s\",\"message\":{\"role\":\"user\",\"content\":[{\"type\":\"tool_result\",\"content\":\"ok\"}]}}\n"
                .formatted(timestamp, cwd);
    }

    public static String metaLine(String cwd, String timestamp, String text) {
        return "{\"type\":\"user\",\"isMeta\":true,\"timestamp\":\"%s\",\"cwd\":\"%s\",\"message\":{\"role\":\"user\",\"content\":%s}}\n"
                .formatted(timestamp, cwd, quoted(text));
    }

    public static String assistantLine(String cwd, String timestamp) {
        return "{\"type\":\"assistant\",\"timestamp\":\"%s\",\"cwd\":\"%s\",\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"done\"}]}}\n"
                .formatted(timestamp, cwd);
    }




    public static String commandLine(String cwd, String timestamp, String branch, String command, String args) {
        return "{\"type\":\"user\",\"timestamp\":\"%s\",\"cwd\":\"%s\",\"gitBranch\":\"%s\",\"message\":{\"role\":\"user\",\"content\":%s}}\n"
                .formatted(
                        timestamp,
                        cwd,
                        branch,
                        quoted("<command-message>x</command-message>\n<command-name>%s</command-name>\n<command-args>%s</command-args>"
                                .formatted(command, args)));
    }

    public static String skillLine(String cwd, String timestamp, String branch, String skill, String args) {
        return "{\"type\":\"assistant\",\"timestamp\":\"%s\",\"cwd\":\"%s\",\"gitBranch\":\"%s\",\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"tool_use\",\"name\":\"Skill\",\"input\":{\"skill\":%s,\"args\":%s}}]}}\n"
                .formatted(timestamp, cwd, branch, quoted(skill), quoted(args));
    }

    public static String skillLineWithoutArgs(String cwd, String timestamp, String skill) {
        return "{\"type\":\"assistant\",\"timestamp\":\"%s\",\"cwd\":\"%s\",\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"tool_use\",\"name\":\"Skill\",\"input\":{\"skill\":%s}}]}}\n"
                .formatted(timestamp, cwd, quoted(skill));
    }

    public static String branchLine(String cwd, String timestamp, String branch) {
        return "{\"type\":\"assistant\",\"timestamp\":\"%s\",\"cwd\":\"%s\",\"gitBranch\":\"%s\",\"message\":{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"done\"}]}}\n"
                .formatted(timestamp, cwd, branch);
    }

    public static String branchLineWithoutTimestamp(String branch) {
        return "{\"type\":\"file-history-snapshot\",\"gitBranch\":\"%s\"}\n".formatted(branch);
    }

    public static String lineWithoutCwd(String timestamp) {
        return "{\"type\":\"queue-operation\",\"timestamp\":\"%s\"}\n".formatted(timestamp);
    }

    public static String aiTitleLine(String title) {
        return "{\"type\":\"ai-title\",\"aiTitle\":%s,\"sessionId\":\"%s\"}\n".formatted(quoted(title), SESSION_ID);
    }

    public static String notJsonLine() {
        return "{\"type\":\"user\",\"timestamp\": oops this is not json\n";
    }

    public static Path writeTranscript(Path projects, String folder, String sessionId, String... lines) throws IOException {
        Path file = projects.resolve(folder).resolve(sessionId + ".jsonl");
        Files.createDirectories(file.getParent());
        Files.writeString(file, String.join("", lines), StandardCharsets.UTF_8);
        return file;
    }

    public static Path writeSubagentTranscript(Path projects, String folder, String sessionId, String... lines)
            throws IOException {
        Path file = projects.resolve(folder).resolve(sessionId).resolve("subagents").resolve("agent-1.jsonl");
        Files.createDirectories(file.getParent());
        Files.writeString(file, String.join("", lines), StandardCharsets.UTF_8);
        return file;
    }

    public static void append(Path file, String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    public static void rewriteInPlace(Path file, String content) throws IOException {
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    public static void touch(Path file, Instant modified) throws IOException {
        Files.setLastModifiedTime(file, FileTime.from(modified));
    }

    public static Instant modifiedAt(Path file) throws IOException {
        return Files.getLastModifiedTime(file).toInstant();
    }

    public static LocatedPastSession pastSessionOf(
            String sessionId, SessionFacts facts, String cwd, String lastActivity, String startedAt, ProjectLocation location) {
        return LocatedPastSession.builder()
                .session(PastSession.builder()
                        .id(new SessionId(sessionId))
                        .facts(facts)
                        .directory(new WorkingDirectory(Path.of(cwd)))
                        .lastActivity(new LastActivity(Instant.parse(lastActivity)))
                        .startedAt(new StartedAt(Instant.parse(startedAt)))
                        .build())
                .location(location)
                .build();
    }

    public static Path writeFile(Path directory, String content) throws IOException {
        Path file = directory.resolve("transcript.jsonl");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    public static String textOf(byte[] line) {
        return new String(line, StandardCharsets.UTF_8);
    }

    static Instant lastActivityOf(TranscriptRepository repository) {
        return repository.pastSessions().getFirst().session().lastActivity().value();
    }

    static TranscriptRepository scanned(
            Path projects, Path cache, TranscriptLocator locator, CacheFallback unwritable, RootFallback unlistable) {
        TranscriptRepository repository = new TranscriptRepository(projects, new TranscriptCacheFile(cache));
        repository.rescanAll(locator, unwritable, unlistable);
        return repository;
    }

    private static String quoted(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
