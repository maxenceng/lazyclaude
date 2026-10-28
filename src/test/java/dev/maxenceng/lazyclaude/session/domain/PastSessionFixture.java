package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.noRuns;
import static dev.maxenceng.lazyclaude.session.domain.LinkFactsFixture.ran;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.now;
import static dev.maxenceng.lazyclaude.session.published.OpeningPromptFixture.noFirstPrompt;
import static dev.maxenceng.lazyclaude.session.published.OpeningPromptFixture.openingUnder;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.aiTitle;
import static dev.maxenceng.lazyclaude.session.published.SessionTitleFixture.firstPrompt;
import static dev.maxenceng.lazyclaude.session.published.TranscriptReadingFixture.readable;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.session.published.AiTitle;
import dev.maxenceng.lazyclaude.session.published.SessionId;
import dev.maxenceng.lazyclaude.session.published.SessionTitle;

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
                .opening(firstPrompt())
                .reading(readable())
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
                .title(new AiTitle("Past session %d".formatted(number))).opening(noFirstPrompt())
                .lastActivity(new LastActivity(now().minus(Duration.ofMinutes(minutes))))
                .build();
    }

    public static List<PastSession> pastSessionsEachOlderThanTheLast(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(number -> pastSessionActiveMinutesAgo(number, number * 10))
                .toList();
    }

    public static PastSession pastSessionTitled(SessionTitle title) {
        return pastSessionBuilder().title(title).opening(openingUnder(title)).build();
    }

    public static LaunchEntry pastLaunchEntry(int number) {
        return interruptedLaunchEntry(number).endedAt(new EndedAt(now().minus(Duration.ofMinutes(5))));
    }

    public static LaunchEntry interruptedLaunchEntry(int number) {
        return LaunchEntry.builder()
                .command(new NewClaude(pastSessionId(number)))
                .directory(projectRootDirectory())
                .startedAt(new StartedAt(now().minus(Duration.ofMinutes(80))))
                .build();
    }

    public static TranscriptFacts pastTranscriptFacts() {
        return pastTranscriptFactsRunning();
    }

    public static TranscriptFacts pastTranscriptFactsRunning(TicketRun... runs) {
        return TranscriptFacts.builder()
                .title(aiTitle())
                .opening(firstPrompt())
                .reading(readable())
                .lastActivity(new LastActivity(now().minus(Duration.ofMinutes(10))))
                .links(ran(runs))
                .build();
    }

    public static TranscriptFacts pastTranscriptFactsActiveMinutesAgo(int number, int minutes) {
        return TranscriptFacts.builder()
                .title(new AiTitle("Past session %d".formatted(number)))
                .opening(noFirstPrompt())
                .reading(readable())
                .lastActivity(new LastActivity(now().minus(Duration.ofMinutes(minutes))))
                .links(noRuns())
                .build();
    }

    public static LoggedTranscripts transcriptsRead(Map<SessionId, TranscriptFacts> read) {
        return LoggedTranscripts.builder().read(read).build();
    }

    public static LoggedTranscripts transcriptsGone(SessionId... gone) {
        return LoggedTranscripts.builder().gone(List.of(gone)).build();
    }

    public static LoggedTranscripts noTranscripts() {
        return LoggedTranscripts.builder().build();
    }

    public static PastSessionPort quietPastSessions() {
        PastSessionPort past = mock(PastSessionPort.class);
        lenient().when(past.transcriptsOf(any())).thenReturn(noTranscripts());
        return past;
    }
}
