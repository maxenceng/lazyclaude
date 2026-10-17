package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.earlierStartedAt;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.laterStartedAt;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.startedAt;
import static dev.maxenceng.lazyclaude.session.published.ClaudeVersionFixture.version;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.thirdProcessId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.thirdSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.secondSessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.sessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.thirdSessionName;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.YourTurn;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

public final class LiveSessionFixture {

    private LiveSessionFixture() {}

    public static LiveSession.LiveSessionBuilder liveSessionBuilder() {
        return LiveSession.builder()
                .id(sessionId())
                .pid(processId())
                .name(sessionName())
                .directory(projectRootDirectory())
                .startedAt(startedAt())
                .version(version())
                .state(new Busy());
    }

    public static LiveSession liveSession() {
        return liveSessionBuilder().build();
    }

    public static LiveSession.LiveSessionBuilder newestSessionBuilder() {
        return liveSessionBuilder()
                .id(secondSessionId())
                .pid(secondProcessId())
                .name(secondSessionName())
                .startedAt(laterStartedAt());
    }

    public static LiveSession.LiveSessionBuilder oldestSessionBuilder() {
        return liveSessionBuilder()
                .id(thirdSessionId())
                .pid(thirdProcessId())
                .name(thirdSessionName())
                .startedAt(earlierStartedAt())
                .state(new YourTurn());
    }

    public static LiveSession newestSession() {
        return newestSessionBuilder().build();
    }

    public static LiveSession oldestSession() {
        return oldestSessionBuilder().build();
    }

    public static LiveSession hostedLiveSession() {
        return liveSessionBuilder().hosting(Hosting.HOSTED).build();
    }

    public static LiveSession sessionIn(WorkingDirectory directory) {
        return liveSessionBuilder().directory(directory).build();
    }

    public static LiveSession sessionIn(WorkingDirectory directory, SessionState state) {
        return liveSessionBuilder().directory(directory).state(state).build();
    }

    public static LiveSession sessionWith(SessionState state) {
        return liveSessionBuilder().state(state).build();
    }
}
