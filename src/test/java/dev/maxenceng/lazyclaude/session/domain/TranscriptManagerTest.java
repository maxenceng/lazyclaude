package dev.maxenceng.lazyclaude.session.domain;

import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.TranscriptEntryFixture.prompt;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.maxenceng.lazyclaude.session.published.Reason;
import dev.maxenceng.lazyclaude.session.published.TranscriptOffset;

@ExtendWith(MockitoExtension.class)
class TranscriptManagerTest {

    @Mock
    private TranscriptReadPort transcripts;

    @Test
    void shouldHandOverWhatTheTranscriptHoldsFromTheOffset() {
        TranscriptRead read = ReadEntries.builder()
                .entries(List.of(prompt("hello")))
                .start(TranscriptOffset.start())
                .next(new TranscriptOffset(40))
                .build();
        when(transcripts.read(sessionId(), projectRootDirectory(), TranscriptOffset.start())).thenReturn(read);

        assertThat(new TranscriptManager(transcripts).read(sessionId(), projectRootDirectory(), TranscriptOffset.start()))
                .isEqualTo(read);
    }

    @Test
    void shouldSayThereIsNoTranscriptYetWhenThePortFindsNone() {
        when(transcripts.read(sessionId(), projectRootDirectory(), TranscriptOffset.start())).thenReturn(new NoTranscript());

        assertThat(new TranscriptManager(transcripts).read(sessionId(), projectRootDirectory(), TranscriptOffset.start()))
                .isEqualTo(new NoTranscript());
    }

    @Test
    void shouldAnswerWhyTheTranscriptCannotBeReadRatherThanFail() {
        when(transcripts.read(sessionId(), projectRootDirectory(), TranscriptOffset.start()))
                .thenThrow(new TranscriptUnreadableException("cannot read /home/max/.claude/projects: access denied", new IOException()));

        assertThat(new TranscriptManager(transcripts).read(sessionId(), projectRootDirectory(), TranscriptOffset.start()))
                .isEqualTo(new UnreadableTranscript(new Reason("cannot read /home/max/.claude/projects: access denied")));
    }
}
