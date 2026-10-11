package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.now;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.aiTitle;
import static dev.maxenceng.lazyclaude.shared.domain.ProjectFixture.project;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.session.published.AiTitle;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionTitle;
import dev.maxenceng.lazyclaude.shared.domain.WorkingDirectory;

public final class PastSessionFixture {

    private static final String PAST_ID_FORMAT = "11111111-0000-0000-0000-%012d";

    private PastSessionFixture() {}

    public static SessionId pastSessionId(int number) {
        return new SessionId(PAST_ID_FORMAT.formatted(number));
    }

    public static PastSession.PastSessionBuilder pastSessionBuilder() {
        return PastSession.builder()
                .id(pastSessionId(1))
                .title(aiTitle())
                .directory(projectRootDirectory())
                .lastActivity(new LastActivity(now().minus(Duration.ofMinutes(10))))
                .startedAt(new StartedAt(now().minus(Duration.ofMinutes(80))));
    }

    public static PastSession pastSession() {
        return pastSessionBuilder().build();
    }

    public static PastSession pastSessionActiveMinutesAgo(int number, int minutes) {
        return pastSessionBuilder()
                .id(pastSessionId(number))
                .title(new AiTitle("Past session %d".formatted(number)))
                .lastActivity(new LastActivity(now().minus(Duration.ofMinutes(minutes))))
                .build();
    }

    public static List<PastSession> pastSessionsEachOlderThanTheLast(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(number -> pastSessionActiveMinutesAgo(number, number * 10))
                .toList();
    }

    public static PastSession pastSessionTitled(SessionTitle title) {
        return pastSessionBuilder().title(title).build();
    }

    public static PastSession pastSessionIn(WorkingDirectory directory) {
        return pastSessionBuilder().directory(directory).build();
    }

    public static LocatedPastSession.LocatedPastSessionBuilder locatedPastSessionBuilder() {
        return LocatedPastSession.builder().session(pastSession()).location(locatedInProject());
    }

    public static LocatedPastSession locatedPastSession() {
        return locatedPastSessionBuilder().build();
    }

    public static LocatedPastSession locatedIn(PastSession session) {
        return locatedPastSessionBuilder().session(session).build();
    }

    public static LocatedPastSession notLocated(PastSession session) {
        return locatedPastSessionBuilder().session(session).location(new NotLocated()).build();
    }

    public static ProjectLocation locatedInProject() {
        return new LocatedInProject(project());
    }
}
