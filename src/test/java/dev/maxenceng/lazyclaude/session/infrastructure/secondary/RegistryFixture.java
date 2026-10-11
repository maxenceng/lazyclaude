package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import dev.maxenceng.lazyclaude.session.domain.NoEntrypoint;
import dev.maxenceng.lazyclaude.session.domain.OtherEntrypoint;
import dev.maxenceng.lazyclaude.session.domain.ReportedSession;
import dev.maxenceng.lazyclaude.session.domain.ReportedStatus;
import dev.maxenceng.lazyclaude.session.domain.StartedAt;
import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.Version;
import dev.maxenceng.lazyclaude.session.published.WaitingFor;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

public final class RegistryFixture {

    private static final long SESSION_PID = 17234;
    private static final String SESSION_ID = "70c67de2-4563-47c2-b946-eb5198915ba2";
    private static final String MACHINE_ID = "8627a0d904304418a34c85cb0414dcff";
    private static final String PID_NAMESPACE = "pid:[4026531836]";
    private static final String STAT_FILLER = "0 ".repeat(17);

    private RegistryFixture() {}

    public static long registeredPid() {
        return SESSION_PID;
    }

    public static String registeredSessionId() {
        return SESSION_ID;
    }

    public static String thisMachinesDomain() {
        return "linux:%s:%s".formatted(MACHINE_ID, PID_NAMESPACE);
    }

    public static String anotherMachinesDomain() {
        return "linux:96044f509dff43fba2dabbbf78c21148:pid:[4026532219]";
    }

    public static String record(long pid, String sessionId, Path cwd, String status) {
        return recordIn(thisMachinesDomain(), pid, sessionId, cwd, "175781", status);
    }

    public static String recordIn(String domain, long pid, String sessionId, Path cwd, String procStart, String status) {
        return ("{\"pid\":%d,\"sessionId\":\"%s\",\"cwd\":\"%s\",\"startedAt\":1791662734987,\"procStart\":\"%s\","
                        + "\"version\":\"2.1.296\",\"peerProtocol\":1,\"peerFeatures\":[\"notify_idle\"],\"kind\":\"interactive\","
                        + "\"entrypoint\":\"cli\",\"pidDomain\":\"%s\",\"messagingSocketPath\":\"/run/user/1000/cc-socks/%d.sock\","
                        + "\"name\":\"lazyclaude-b6\",\"nameSource\":\"derived\",\"updatedAt\":1791667770457,\"status\":\"%s\","
                        + "\"statusUpdatedAt\":1791667770457}")
                .formatted(pid, sessionId, cwd, procStart, domain, pid, status);
    }

    public static String waitingRecord(long pid, String sessionId, Path cwd, String waitingFor) {
        return record(pid, sessionId, cwd, "waiting").replace("}", ",\"waitingFor\":\"%s\"}".formatted(waitingFor));
    }

    public static String sdkCliRecord(long pid, String sessionId, Path cwd) {
        return record(pid, sessionId, cwd, "busy").replace("\"entrypoint\":\"cli\"", "\"entrypoint\":\"sdk-cli\"");
    }

    public static void writeRecord(Path sessions, String fileName, String json) throws IOException {
        Files.createDirectories(sessions);
        Files.writeString(sessions.resolve(fileName), json, StandardCharsets.UTF_8);
    }

    public static Path machineIdFile(Path directory) throws IOException {
        Path file = directory.resolve("machine-id");
        Files.writeString(file, MACHINE_ID + "\n", StandardCharsets.UTF_8);
        return file;
    }

    public static Path procWithNamespace(Path directory) throws IOException {
        Path proc = directory.resolve("proc");
        Path namespaces = Files.createDirectories(proc.resolve("self/ns"));
        Files.createSymbolicLink(namespaces.resolve("pid"), Path.of(PID_NAMESPACE));
        return proc;
    }

    public static void runningProcess(Path proc, long pid, String comm, String procStart) throws IOException {
        Files.createDirectories(proc.resolve(Long.toString(pid)));
        String stat = "%d (%s) S 1 %s%s 0 0\n".formatted(pid, comm, STAT_FILLER, procStart);
        Files.writeString(proc.resolve(Long.toString(pid)).resolve("stat"), stat, StandardCharsets.UTF_8);
    }

    public static String agentsJson(List<String> elements) {
        return "[" + String.join(",", elements) + "]";
    }

    public static String agent(long pid, String sessionId, Path cwd, String status) {
        return ("{\"pid\":%d,\"cwd\":\"%s\",\"kind\":\"interactive\",\"startedAt\":1791662734987,"
                        + "\"sessionId\":\"%s\",\"name\":\"lazyclaude-b6\",\"status\":\"%s\"}")
                .formatted(pid, cwd, sessionId, status);
    }

    public static ReportedSession registered(Path cwd, ReportedStatus status, WaitingFor waitingFor) {
        return ReportedSession.builder()
                .id(new SessionId(SESSION_ID))
                .pid(new ProcessId(SESSION_PID))
                .name(new SessionName("lazyclaude-b6"))
                .directory(new WorkingDirectory(cwd))
                .startedAt(new StartedAt(Instant.ofEpochMilli(1791662734987L)))
                .version(new Version("2.1.296"))
                .status(status)
                .waitingFor(waitingFor)
                .entrypoint(new OtherEntrypoint())
                .build();
    }

    public static ReportedSession registeredBusy(Path cwd) {
        return registered(cwd, ReportedStatus.BUSY, new NoWaitingReason());
    }

    public static ReportedSession reportedByAgents(Path cwd, ReportedStatus status) {
        return ReportedSession.builder()
                .id(new SessionId(SESSION_ID))
                .pid(new ProcessId(SESSION_PID))
                .name(new SessionName("lazyclaude-b6"))
                .directory(new WorkingDirectory(cwd))
                .startedAt(new StartedAt(Instant.ofEpochMilli(1791662734987L)))
                .version(new NoVersion())
                .status(status)
                .waitingFor(new NoWaitingReason())
                .entrypoint(new NoEntrypoint())
                .build();
    }

    public static LiveSessionRepository repositoryOver(SessionRegistryRepository registry, ClaudeAgentsRepository agents) {
        return new LiveSessionRepository(registry, agents, Duration.ofSeconds(2));
    }
}
