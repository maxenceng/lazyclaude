package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.published.ClaudeVersionFixture.version;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.thirdProcessId;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.thirdSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.secondSessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.sessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.thirdSessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.SessionAge;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

public final class SessionViewFixture {

    private SessionViewFixture() {}

    public static SessionView.SessionViewBuilder sessionViewBuilder() {
        return SessionView.builder()
                .id(sessionId())
                .pid(processId())
                .name(sessionName())
                .directory(projectRootDirectory().path())
                .startedAt(LocalDateTime.of(2026, 10, 10, 12, 0))
                .age(new SessionAge(Duration.ofHours(2)))
                .version(version())
                .state(new Busy());
    }

    public static SessionView sessionView() {
        return sessionViewBuilder().build();
    }

    public static SessionView newestSessionView() {
        return sessionViewBuilder()
                .id(secondSessionId())
                .pid(secondProcessId())
                .name(secondSessionName())
                .startedAt(LocalDateTime.of(2026, 10, 10, 13, 48))
                .age(new SessionAge(Duration.ofMinutes(12)))
                .build();
    }

    public static SessionView oldestSessionView() {
        return sessionViewBuilder()
                .id(thirdSessionId())
                .pid(thirdProcessId())
                .name(thirdSessionName())
                .startedAt(LocalDateTime.of(2026, 10, 9, 12, 0))
                .age(new SessionAge(Duration.ofHours(26)))
                .state(new YourTurn())
                .build();
    }

    public static SessionView sessionViewWith(SessionState state) {
        return sessionViewBuilder().state(state).build();
    }

    public static SessionView waitingSessionView() {
        return sessionViewWith(new WaitingForPermission());
    }

    public static SessionsView sessionsViewOf(List<SessionView> sessions) {
        return new LiveSessionsView(sessions);
    }

    public static SessionsView emptySessionsView() {
        return sessionsViewOf(List.of());
    }

    public static SessionsView unavailableSessionsView() {
        return new UnavailableSessionsView(reason());
    }

    public static List<SessionView> everyStateSessionViews() {
        return List.of(
                namedSessionView(sessionIdOf(1), "busy-one", new Busy()),
                namedSessionView(sessionIdOf(2), "your-turn-one", new YourTurn()),
                namedSessionView(sessionIdOf(3), "permission-one", new WaitingForPermission()),
                namedSessionView(sessionIdOf(4), "input-one", waitingForInput()),
                namedSessionView(sessionIdOf(5), "unknown-one", new Unknown()));
    }

    public static SessionView inputSessionView() {
        return sessionViewWith(waitingForInput());
    }

    public static SessionView namedSessionView(SessionId id, String name, SessionState state) {
        return sessionViewBuilder().id(id).name(new SessionName(name)).state(state).build();
    }

    public static SessionView sessionViewWithoutVersion() {
        return sessionViewBuilder().version(new NoVersion()).build();
    }

    public static SessionId sessionIdOf(int number) {
        return new SessionId("00000000-0000-0000-0000-00000000000" + number);
    }
}
