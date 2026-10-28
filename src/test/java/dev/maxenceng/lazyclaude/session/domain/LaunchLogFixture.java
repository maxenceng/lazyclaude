package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.earlierStartedAt;
import static dev.maxenceng.lazyclaude.session.domain.StartedAtFixture.now;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.secondSessionId;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.function.UnaryOperator;

import org.mockito.ArgumentCaptor;

import dev.maxenceng.lazyclaude.board.published.Verb;
import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.SessionId;

public final class LaunchLogFixture {

    private LaunchLogFixture() {}

    public static LaunchEntry.LaunchEntryBuilder launchEntryBuilder() {
        return LaunchEntry.builder()
                .command(new NewClaude(sessionId()))
                .directory(projectRootDirectory())
                .startedAt(earlierStartedAt());
    }

    public static LaunchEntry launchEntry() {
        return launchEntryBuilder().build();
    }

    public static LaunchEntry launchEntryOf(SessionId id) {
        return launchEntryBuilder().command(new NewClaude(id)).build();
    }

    public static LaunchEntry secondLaunchEntry() {
        return launchEntryOf(secondSessionId());
    }

    public static LaunchEntry endedLaunchEntry() {
        return launchEntry().endedAt(endedAt());
    }

    public static EndedAt endedAt() {
        return new EndedAt(now());
    }

    public static LaunchLog launchLogOf(LaunchEntry... entries) {
        return LaunchLog.builder().entries(List.of(entries)).build();
    }

    public static LaunchLog emptyLaunchLog() {
        return launchLogOf();
    }

    public static LaunchLog afterChange(ArgumentCaptor<UnaryOperator<LaunchLog>> change, int call, LaunchLog before) {
        return change.getAllValues().get(call).apply(before);
    }

    public static Reason logUnavailable() {
        return new Reason("cannot write /state/lazyclaude/launch.log: Permission denied");
    }

    public static LaunchEntry ticketLaunchEntry(int number, Verb verb) {
        return ticketLaunchEntry(PastSessionFixture.pastSessionId(number), verb);
    }

    public static LaunchEntry ticketLaunchEntry(SessionId id, Verb verb) {
        return launchEntryBuilder().command(HostedSessionFixture.ticketClaude(id, verb)).build();
    }

    public static LaunchLogPort emptyLaunchLogPort() {
        LaunchLogPort log = mock(LaunchLogPort.class);
        lenient().when(log.read(any(), any())).thenReturn(emptyLaunchLog());
        return log;
    }
}
