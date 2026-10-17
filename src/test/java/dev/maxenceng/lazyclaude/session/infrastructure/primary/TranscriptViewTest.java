package dev.maxenceng.lazyclaude.session.infrastructure.primary;

import static dev.maxenceng.lazyclaude.session.application.SessionApplicationServiceFixture.transcriptReadingService;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.OutsideSessionPayloadFixture.transcriptPayload;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.TranscriptEntryFixture.prompt;
import static dev.maxenceng.lazyclaude.session.published.TranscriptEntryFixture.reply;
import static dev.maxenceng.lazyclaude.shared.domain.WorkingDirectoryFixture.projectRootDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.domain.NoTranscript;
import dev.maxenceng.lazyclaude.session.domain.ReadEntries;
import dev.maxenceng.lazyclaude.session.domain.TranscriptReadPort;
import dev.maxenceng.lazyclaude.session.domain.TranscriptUnreadableException;
import dev.maxenceng.lazyclaude.session.published.TranscriptOffset;

class TranscriptViewTest {

    @Test
    void shouldViewTheEntriesReadWhereTheyStartAndEnd() {
        TranscriptReadPort transcripts = mock(TranscriptReadPort.class);
        when(transcripts.read(sessionId(), projectRootDirectory(), TranscriptOffset.start()))
                .thenReturn(ReadEntries.builder()
                        .entries(List.of(prompt("hello"), reply("hi")))
                        .start(TranscriptOffset.start())
                        .next(new TranscriptOffset(90))
                        .build());

        assertThat(new SessionQuery(transcriptReadingService(transcripts)).transcriptOf(transcriptPayload()))
                .isEqualTo(ReadTranscriptView.builder()
                        .entries(List.of(prompt("hello"), reply("hi")))
                        .start(TranscriptOffset.start())
                        .next(new TranscriptOffset(90))
                        .build());
    }

    @Test
    void shouldViewASessionWithNoTranscriptYet() {
        TranscriptReadPort transcripts = mock(TranscriptReadPort.class);
        when(transcripts.read(sessionId(), projectRootDirectory(), TranscriptOffset.start())).thenReturn(new NoTranscript());

        assertThat(new SessionQuery(transcriptReadingService(transcripts)).transcriptOf(transcriptPayload()))
                .isEqualTo(new NoTranscriptView());
    }

    @Test
    void shouldViewWhyATranscriptCannotBeRead() {
        TranscriptReadPort transcripts = mock(TranscriptReadPort.class);
        when(transcripts.read(sessionId(), projectRootDirectory(), TranscriptOffset.start()))
                .thenThrow(new TranscriptUnreadableException(reason().value(), new IOException()));

        assertThat(new SessionQuery(transcriptReadingService(transcripts)).transcriptOf(transcriptPayload()))
                .isEqualTo(new UnreadableTranscriptView(reason()));
    }
}
