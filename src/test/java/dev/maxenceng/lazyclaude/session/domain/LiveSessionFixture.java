package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.published.ClaudeVersionFixture.version;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.thirdProcessId;
import static dev.maxenceng.lazyclaude.session.published.SessionDirectoryFixture.sessionDirectory;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.thirdSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.secondSessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.sessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.thirdSessionName;
import static dev.maxenceng.lazyclaude.session.published.StartedAtFixture.earlierStartedAt;
import static dev.maxenceng.lazyclaude.session.published.StartedAtFixture.laterStartedAt;
import static dev.maxenceng.lazyclaude.session.published.StartedAtFixture.startedAt;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.SessionDirectory;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

public final class LiveSessionFixture {

    private LiveSessionFixture() {}

    public static LiveSession.LiveSessionBuilder liveSessionBuilder() {
        return LiveSession.builder()
                .id(sessionId())
                .pid(processId())
                .name(sessionName())
                .directory(sessionDirectory())
                .startedAt(startedAt())
                .version(version())
                .state(new Busy())
                .entrypoint(new OtherEntrypoint());
    }

    public static LiveSession liveSession() {
        return liveSessionBuilder().build();
    }

    public static LiveSession newestSession() {
        return liveSessionBuilder()
                .id(secondSessionId())
                .pid(secondProcessId())
                .name(secondSessionName())
                .startedAt(laterStartedAt())
                .build();
    }

    public static LiveSession oldestSession() {
        return liveSessionBuilder()
                .id(thirdSessionId())
                .pid(thirdProcessId())
                .name(thirdSessionName())
                .startedAt(earlierStartedAt())
                .state(new YourTurn())
                .build();
    }

    public static LiveSession sessionIn(SessionDirectory directory) {
        return liveSessionBuilder().directory(directory).build();
    }

    public static LiveSession sessionWith(SessionState state) {
        return liveSessionBuilder().state(state).build();
    }

    public static LiveSession sdkCliSession() {
        return liveSessionBuilder().entrypoint(new SdkCliEntrypoint()).build();
    }

    public static LiveSession sessionWithoutEntrypoint() {
        return liveSessionBuilder().entrypoint(new NoEntrypoint()).state(new YourTurn()).build();
    }
}
