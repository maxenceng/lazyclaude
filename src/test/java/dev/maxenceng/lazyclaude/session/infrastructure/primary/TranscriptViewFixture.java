package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.published.TranscriptEntryFixture.reply;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import dev.maxenceng.lazyclaude.session.published.TranscriptEntry;
import dev.maxenceng.lazyclaude.session.published.TranscriptOffset;

public final class TranscriptViewFixture {

    private static final long FIRST_END = 100;

    private TranscriptViewFixture() {}

    public static ReadTranscriptView readTranscriptView(TranscriptEntry... entries) {
        return continuingTranscriptView(0, FIRST_END, entries);
    }

    public static ReadTranscriptView continuingTranscriptView(long start, long next, TranscriptEntry... entries) {
        return ReadTranscriptView.builder()
                .entries(List.of(entries))
                .start(new TranscriptOffset(start))
                .next(new TranscriptOffset(next))
                .build();
    }

    public static ReadTranscriptView transcriptViewOfReplies(int count) {
        return ReadTranscriptView.builder()
                .entries(repliesNumbered(1, count))
                .start(TranscriptOffset.start())
                .next(new TranscriptOffset(FIRST_END))
                .build();
    }

    public static ReadTranscriptView transcriptViewOfMoreReplies(int already, int more) {
        return ReadTranscriptView.builder()
                .entries(repliesNumbered(already + 1, more))
                .start(new TranscriptOffset(FIRST_END))
                .next(new TranscriptOffset(FIRST_END * 2))
                .build();
    }

    public static AddRemoteControlView addRemoteControlView() {
        return AddRemoteControlView.builder()
                .file(Path.of(System.getProperty("user.home"), ".config/kitty/kitty.conf"))
                .lines(List.of("# added by lazyclaude", "allow_remote_control socket-only", "listen_on unix:@kitty-{kitty_pid}"))
                .build();
    }

    public static AddRemoteControlView addRemoteControlViewOutsideHome() {
        return AddRemoteControlView.builder()
                .file(Path.of("/etc/kitty-here/kitty.conf"))
                .lines(List.of("# added by lazyclaude", "listen_on unix:@kitty-{kitty_pid}"))
                .build();
    }

    public static ConflictingRemoteControlView conflictingRemoteControlView() {
        return ConflictingRemoteControlView.builder()
                .file(Path.of(System.getProperty("user.home"), ".config/kitty/user.conf"))
                .line(12)
                .setting("allow_remote_control yes")
                .wanted("allow_remote_control socket-only")
                .build();
    }

    private static List<TranscriptEntry> repliesNumbered(int first, int count) {
        return IntStream.range(first, first + count)
                .mapToObj(number -> reply("reply %d".formatted(number)))
                .toList();
    }
}
