package dev.maxenceng.lazyclaude.session.infrastructure.secondary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import dev.maxenceng.lazyclaude.session.domain.ReportedSession;
import dev.maxenceng.lazyclaude.session.domain.ReportedStatus;
import dev.maxenceng.lazyclaude.session.domain.StartedAt;
import dev.maxenceng.lazyclaude.session.published.NoWaitingReason;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.Version;
import dev.maxenceng.lazyclaude.session.published.WaitingFor;

public final class RegistryFixture {

    private static final long SESSION_PID = 17234;
    private static final String SESSION_ID = "70c67de2-4563-47c2-b946-eb5198915ba2";

    private RegistryFixture() {}

    public static long registeredPid() {
        return SESSION_PID;
    }

    public static ProcessId registeredProcess() {
        return new ProcessId(SESSION_PID);
    }

    public static String recordFileName() {
        return SESSION_PID + ".json";
    }

    public static String record(String status) {
        return ("{\"pid\":%d,\"sessionId\":\"%s\",\"cwd\":\"/work/app\",\"startedAt\":1791662734987,\"procStart\":\"175781\","
                        + "\"version\":\"2.1.296\",\"peerProtocol\":1,\"peerFeatures\":[\"notify_idle\"],\"kind\":\"interactive\","
                        + "\"entrypoint\":\"cli\",\"pidDomain\":\"linux:8627a0d904304418a34c85cb0414dcff:pid:[4026531836]\","
                        + "\"messagingSocketPath\":\"/run/user/1000/cc-socks/%d.sock\","
                        + "\"name\":\"lazyclaude-b6\",\"nameSource\":\"derived\",\"updatedAt\":1791667770457,\"status\":\"%s\","
                        + "\"statusUpdatedAt\":1791667770457}")
                .formatted(SESSION_PID, SESSION_ID, SESSION_PID, status);
    }

    public static String waitingRecord(String waitingFor) {
        return record("waiting").replace("}", ",\"waitingFor\":\"%s\"}".formatted(waitingFor));
    }

    public static void writeRecord(Path sessions, String fileName, String json) throws IOException {
        Files.createDirectories(sessions);
        Files.writeString(sessions.resolve(fileName), json, StandardCharsets.UTF_8);
    }

    public static ReportedSession registered(ReportedStatus status, WaitingFor waitingFor) {
        return ReportedSession.builder()
                .name(new SessionName("lazyclaude-b6"))
                .startedAt(new StartedAt(Instant.ofEpochMilli(1791662734987L)))
                .version(new Version("2.1.296"))
                .status(status)
                .waitingFor(waitingFor)
                .build();
    }

    public static ReportedSession registeredBusy() {
        return registered(ReportedStatus.BUSY, new NoWaitingReason());
    }
}
