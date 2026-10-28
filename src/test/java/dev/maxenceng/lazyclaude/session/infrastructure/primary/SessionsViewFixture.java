package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.published.ClaudeVersionFixture.version;
import static dev.maxenceng.lazyclaude.session.published.LinkedTicketIdFixture.linkedTicketId;
import static dev.maxenceng.lazyclaude.session.published.OpeningPromptFixture.noFirstPrompt;
import static dev.maxenceng.lazyclaude.session.published.OpeningPromptFixture.openingUnder;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.processId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.secondProcessId;
import static dev.maxenceng.lazyclaude.session.published.ProcessIdFixture.thirdProcessId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.thirdSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.secondSessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.sessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionNameFixture.thirdSessionName;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForInput;
import static dev.maxenceng.lazyclaude.session.published.SessionStateFixture.waitingForPermission;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.aiTitle;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.firstPrompt;
import static dev.maxenceng.lazyclaude.session.published.TranscriptReadingFixture.readable;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.session.published.AiTitle;
import dev.maxenceng.lazyclaude.session.published.Busy;
import dev.maxenceng.lazyclaude.session.published.NoVersion;
import dev.maxenceng.lazyclaude.session.published.ProcessId;
import dev.maxenceng.lazyclaude.session.published.SessionAge;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionName;
import dev.maxenceng.lazyclaude.session.published.SessionState;
import dev.maxenceng.lazyclaude.session.published.SessionTitle;
import dev.maxenceng.lazyclaude.session.published.TicketLink;
import dev.maxenceng.lazyclaude.session.published.Unknown;
import dev.maxenceng.lazyclaude.session.published.WaitingForPermission;
import dev.maxenceng.lazyclaude.session.published.YourTurn;

public final class SessionsViewFixture {

    private SessionsViewFixture() {}

    public static LiveSessionView.LiveSessionViewBuilder sessionViewBuilder() {
        return LiveSessionView.builder()
                .id(sessionId())
                .pid(processId())
                .name(sessionName())
                .directory(projectRootDirectory().path())
                .startedAt(LocalDateTime.of(2026, 10, 10, 12, 0))
                .age(new SessionAge(Duration.ofHours(2)))
                .version(version())
                .state(new Busy())
                .links(List.of());
    }

    public static LiveSessionView sessionViewWith(SessionState state) {
        return sessionViewBuilder().state(state).build();
    }

    public static HostedSessionView startingHostedSessionView() {
        return HostedSessionView.builder()
                .id(sessionId())
                .pid(processId())
                .live(Optional.empty())
                .busyOrWaiting(false)
                .build();
    }

    public static HostedSessionView startingTicketHostedSessionView() {
        return HostedSessionView.builder()
                .id(sessionId())
                .pid(processId())
                .live(Optional.empty())
                .busyOrWaiting(false)
                .ticket(Optional.of(linkedTicketId()))
                .build();
    }

    public static HostedSessionView liveHostedSessionView(LiveSessionView live, boolean busyOrWaiting) {
        return HostedSessionView.builder()
                .id(sessionId())
                .pid(live.pid())
                .live(Optional.of(live))
                .busyOrWaiting(busyOrWaiting)
                .build();
    }

    public static HostedSessionView hostedTab(int number, String name, SessionState state) {
        LiveSessionView live = sessionViewBuilder()
                .id(sessionIdOf(number))
                .pid(new ProcessId(1000L + number))
                .name(new SessionName(name))
                .state(state)
                .build();
        return HostedSessionView.builder()
                .id(hostedSessionIdOf(number))
                .pid(live.pid())
                .live(Optional.of(live))
                .busyOrWaiting(state instanceof Busy || state instanceof WaitingForPermission)
                .build();
    }

    public static HostedSessionView startingTabNumbered(int number) {
        return HostedSessionView.builder()
                .id(hostedSessionIdOf(number))
                .pid(new ProcessId(1000L + number))
                .live(Optional.empty())
                .busyOrWaiting(false)
                .build();
    }

    public static SessionId hostedSessionIdOf(int number) {
        return new SessionId("00000000-0000-0000-0000-%012d".formatted(number));
    }

    public static SessionsView sessionsViewWithTabs(HostedSessionView... tabs) {
        return sessionsViewHosting(
                List.of(tabs).stream().flatMap(tab -> tab.live().stream()).toList(), List.of(tabs));
    }

    public static SessionsView sessionsViewHosting(List<LiveSessionView> live, List<HostedSessionView> hosted) {
        return SessionsView.builder().live(live).past(List.of()).hosted(hosted).build();
    }

    public static List<LiveSessionView> liveViewsOf(SessionsView view) {
        return view.live();
    }

    public static LiveSessionView sessionView() {
        return sessionViewBuilder().build();
    }

    public static LiveSessionView newestSessionView() {
        return sessionViewBuilder()
                .id(secondSessionId())
                .pid(secondProcessId())
                .name(secondSessionName())
                .startedAt(LocalDateTime.of(2026, 10, 10, 13, 48))
                .age(new SessionAge(Duration.ofMinutes(12)))
                .build();
    }

    public static LiveSessionView oldestSessionView() {
        return sessionViewBuilder()
                .id(thirdSessionId())
                .pid(thirdProcessId())
                .name(thirdSessionName())
                .startedAt(LocalDateTime.of(2026, 10, 9, 12, 0))
                .age(new SessionAge(Duration.ofHours(26)))
                .state(new YourTurn())
                .build();
    }

    public static LiveSessionView waitingSessionView() {
        return sessionViewWith(waitingForPermission());
    }

    public static SessionsView sessionsViewOf(List<LiveSessionView> sessions) {
        return sessionsViewOf(sessions, List.of());
    }

    public static SessionsView sessionsViewOf(List<LiveSessionView> sessions, List<PastSessionView> past) {
        return SessionsView.builder().live(sessions).past(past).build();
    }

    public static SessionsView pastSessionsViewOf(List<PastSessionView> past) {
        return sessionsViewOf(List.of(), past);
    }

    public static PastSessionView.PastSessionViewBuilder pastSessionViewBuilder() {
        return PastSessionView.builder()
                .id(pastSessionIdOf(1))
                .title(aiTitle())
                .opening(firstPrompt())
                .reading(readable())
                .directory(projectRootDirectory().path())
                .lastActivity(LocalDateTime.of(2026, 10, 10, 13, 50))
                .age(new SessionAge(Duration.ofMinutes(10)))
                .startedAt(LocalDateTime.of(2026, 10, 10, 12, 40))
                .links(List.of());
    }

    public static PastSessionView pastSessionView() {
        return pastSessionViewBuilder().build();
    }

    public static PastSessionView pastSessionViewTitled(SessionTitle title) {
        return pastSessionViewBuilder().title(title).opening(openingUnder(title)).build();
    }

    public static PastSessionView pastSessionViewNumbered(int number) {
        return pastSessionViewBuilder()
                .id(pastSessionIdOf(number))
                .title(new AiTitle("Past session %d".formatted(number))).opening(noFirstPrompt())
                .lastActivity(LocalDateTime.of(2026, 10, 10, 14, 0).minusMinutes(number * 10L))
                .age(new SessionAge(Duration.ofMinutes(number * 10L)))
                .build();
    }

    public static PastSessionView linkedPastSessionView(int number, TicketLink... links) {
        return pastSessionViewBuilder()
                .id(pastSessionIdOf(number))
                .title(new AiTitle("Past %d".formatted(number))).opening(noFirstPrompt())
                .lastActivity(LocalDateTime.of(2026, 10, 10, 14, 0).minusMinutes(number * 10L))
                .age(new SessionAge(Duration.ofMinutes(number * 10L)))
                .links(List.of(links))
                .build();
    }

    public static List<PastSessionView> pastSessionViewsNumbered(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(SessionsViewFixture::pastSessionViewNumbered).toList();
    }

    public static SessionId pastSessionIdOf(int number) {
        return new SessionId("11111111-0000-0000-0000-%012d".formatted(number));
    }

    public static SessionsView emptySessionsView() {
        return sessionsViewOf(List.of());
    }

    public static List<LiveSessionView> everyStateSessionViews() {
        return List.of(
                namedSessionView(sessionIdOf(1), "busy-one", new Busy()),
                namedSessionView(sessionIdOf(2), "your-turn-one", new YourTurn()),
                namedSessionView(sessionIdOf(3), "permission-one", waitingForPermission()),
                namedSessionView(sessionIdOf(4), "input-one", waitingForInput()),
                namedSessionView(sessionIdOf(5), "unknown-one", new Unknown()));
    }

    public static LiveSessionView inputSessionView() {
        return sessionViewWith(waitingForInput());
    }

    public static LiveSessionView namedSessionView(SessionId id, String name, SessionState state) {
        return sessionViewBuilder().id(id).name(new SessionName(name)).state(state).build();
    }

    public static LiveSessionView linkedSessionView(SessionId id, SessionState state, TicketLink... links) {
        return sessionViewBuilder().id(id).state(state).links(List.of(links)).build();
    }

    public static LiveSessionView linkedNamedSessionView(int number, String name, SessionState state, TicketLink... links) {
        return sessionViewBuilder()
                .id(sessionIdOf(number))
                .name(new SessionName(name))
                .state(state)
                .links(List.of(links))
                .build();
    }

    public static LiveSessionView sessionViewWithoutVersion() {
        return sessionViewBuilder().version(new NoVersion()).build();
    }

    public static SessionId sessionIdOf(int number) {
        return new SessionId("00000000-0000-0000-0000-00000000000" + number);
    }
}
