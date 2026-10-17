package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.elsewhereGroupViewNamed;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.hostedLiveSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.namedSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.pastSessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionIdOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewElsewhere;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.sessionsViewOf;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.TranscriptViewFixture.continuingTranscriptView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.TranscriptViewFixture.readTranscriptView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.TranscriptViewFixture.transcriptViewOfMoreReplies;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.TranscriptViewFixture.transcriptViewOfReplies;
import static dev.maxenceng.lazyclaude.session.published.ReasonFixture.reason;
import static dev.maxenceng.lazyclaude.session.published.SessionIdFixture.sessionId;
import static dev.maxenceng.lazyclaude.session.published.TranscriptEntryFixture.prompt;
import static dev.maxenceng.lazyclaude.session.published.TranscriptEntryFixture.reply;
import static dev.maxenceng.lazyclaude.session.published.TranscriptEntryFixture.toolCall;
import static dev.maxenceng.lazyclaude.session.published.TranscriptEntryFixture.toolCallWithoutArgument;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.screensReadingTranscripts;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnSessionsReading;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellWithSessionsFocused;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.transcriptsAsked;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.maxenceng.lazyclaude.session.infrastructure.primary.NoTranscriptView;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.OutsideSessionCommand;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionQuery;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.TranscriptPayload;
import dev.maxenceng.lazyclaude.session.infrastructure.primary.UnreadableTranscriptView;
import dev.maxenceng.lazyclaude.session.published.Busy;

class ShellTranscriptTest {

    @Test
    void shouldShowAnOutsideSessionsPromptsRepliesAndToolCallsInTheMainView() {
        Shell shell = shellOnSessionsReading(
                sessionsViewOf(List.of(sessionView())),
                screensReadingTranscripts(readTranscriptView(
                        prompt("fix the status line"),
                        reply("Looking at it."),
                        toolCall("Bash", "make test"),
                        reply("All green."))));

        assertThat(Screen.of(shell).mainViewRows())
                .containsSubsequence("› fix the status line", "Looking at it.", "⏺ Bash  make test", "All green.");
    }

    @Test
    void shouldDrawAToolCallDimAndAPromptAndAReplyPlain() {
        Shell shell = shellOnSessionsReading(
                sessionsViewOf(List.of(sessionView())),
                screensReadingTranscripts(readTranscriptView(prompt("go"), toolCall("Bash", "make test"), reply("Done."))));

        assertThat(Screen.of(shell).dimText())
                .anySatisfy(line -> assertThat(line).contains("⏺ Bash  make test"))
                .noneSatisfy(line -> assertThat(line).containsAnyOf("› go", "Done."));
    }

    @Test
    void shouldShowAToolCallWithNoArgumentAsItsNameAlone() {
        Shell shell = shellOnSessionsReading(
                sessionsViewOf(List.of(sessionView())),
                screensReadingTranscripts(readTranscriptView(toolCallWithoutArgument("EnterPlanMode"))));

        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).isEqualTo("⏺ EnterPlanMode"));
    }

    @Test
    void shouldCutAToolCallToTheWidthOfTheMainViewRatherThanWrapIt() {
        Shell shell = shellOnSessionsReading(
                sessionsViewOf(List.of(sessionView())),
                screensReadingTranscripts(readTranscriptView(toolCall("Bash", "echo ".concat("x".repeat(120))))));

        assertThat(Screen.of(shell).mainViewRows())
                .filteredOn(row -> row.startsWith("⏺ Bash"))
                .singleElement()
                .satisfies(row -> assertThat(row).endsWith("…"));
    }

    @Test
    void shouldWrapALongReplyWithinTheMainViewWithoutLosingAWord() {
        String words = "alpha bravo charlie delta echo foxtrot golf hotel india juliet kilo lima mike november oscar papa";
        Shell shell = shellOnSessionsReading(
                sessionsViewOf(List.of(sessionView())), screensReadingTranscripts(readTranscriptView(reply(words))));

        List<String> rows = Screen.of(shell).mainViewRows().stream().filter(row -> !row.isBlank()).toList();

        assertThat(rows).hasSizeGreaterThan(1);
        assertThat(String.join(" ", rows)).isEqualTo(words);
    }

    @Test
    void shouldIndentTheLinesAPromptWrapsOntoUnderItsPrefix() {
        Shell shell = shellOnSessionsReading(
                sessionsViewOf(List.of(sessionView())),
                screensReadingTranscripts(readTranscriptView(
                        prompt("first line\nsecond line that is long enough to wrap around the edge of the main view for sure"))));

        assertThat(Screen.of(shell).mainViewRows())
                .anySatisfy(row -> assertThat(row).isEqualTo("› first line"))
                .anySatisfy(row -> assertThat(row).startsWith("  second line"));
    }

    @Test
    void shouldShowAPastSessionsTranscriptToo() {
        Shell shell = shellOnSessionsReading(
                pastSessionsViewOf(List.of(pastSessionView())),
                screensReadingTranscripts(readTranscriptView(prompt("refine it"), reply("Refined."))));

        assertThat(Screen.of(shell).mainViewRows()).containsSubsequence("› refine it", "Refined.");
    }

    @Test
    void shouldKeepTheFactsOfASessionLazyclaudeHosts() {
        SessionQuery screens = screensReadingTranscripts(readTranscriptView(reply("not shown")));
        Shell shell = shellOnSessionsReading(sessionsViewOf(List.of(hostedLiveSessionView())), screens);

        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).contains("lazyclaude-b6"));
        verify(screens, never()).transcriptOf(any());
    }

    @Test
    void shouldKeepTheFactsOfASessionElsewhere() {
        SessionQuery screens = screensReadingTranscripts(readTranscriptView(reply("not shown")));
        Shell shell = shellWithSessionsFocused(
                sessionsViewElsewhere(elsewhereGroupViewNamed("next-suggestions", namedSessionView(sessionIdOf(1), "refactor", new Busy()))),
                screens,
                mock(OutsideSessionCommand.class),
                Runnable::run,
                3);

        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).contains("project  next-suggestions"));
        verify(screens, never()).transcriptOf(any());
    }

    @Test
    void shouldAskForTheSelectedSessionsTranscriptFromTheStart() {
        SessionQuery screens = screensReadingTranscripts(readTranscriptView(reply("hello")));
        Shell shell = shellOnSessionsReading(sessionsViewOf(List.of(sessionView())), screens);

        Screen.of(shell);

        TranscriptPayload asked = transcriptsAsked(screens, 1).getFirst();
        assertThat(asked.session()).isEqualTo(sessionId());
        assertThat(asked.directory()).isEqualTo(sessionView().directory());
        assertThat(asked.offset().value()).isZero();
    }

    @Test
    void shouldAskOnlyForTheSelectedSession() {
        SessionQuery screens = screensReadingTranscripts(readTranscriptView(reply("hello")));
        Shell shell = shellOnSessionsReading(
                sessionsViewOf(List.of(sessionView(), namedSessionView(sessionIdOf(2), "other", new Busy()))),
                screens);

        Screen.of(shell);

        verify(screens, times(1)).transcriptOf(any());
    }

    @Test
    void shouldSayTheSessionHasNoTranscriptYet() {
        Shell shell = shellOnSessionsReading(sessionsViewOf(List.of(sessionView())), ShellFixture.blankScreens());

        Screen screen = Screen.of(shell);

        assertThat(screen.mainViewRows()).anySatisfy(row -> assertThat(row).contains("no transcript yet"));
        assertThat(screen.dimText()).anySatisfy(line -> assertThat(line).contains("no transcript yet"));
    }

    @Test
    void shouldFillInOnceTheTranscriptAppears() {
        SessionQuery screens = screensReadingTranscripts(new NoTranscriptView(), readTranscriptView(prompt("hello")));
        Shell shell = shellOnSessionsReading(sessionsViewOf(List.of(sessionView())), screens);
        Screen.of(shell);

        shell.sessionsChanged(sessionsViewOf(List.of(sessionView())));

        assertThat(Screen.of(shell).mainViewRows())
                .anySatisfy(row -> assertThat(row).contains("› hello"))
                .noneSatisfy(row -> assertThat(row).contains("no transcript yet"));
    }

    @Test
    void shouldSayWhyATranscriptCannotBeRead() {
        Shell shell = shellOnSessionsReading(
                sessionsViewOf(List.of(sessionView())),
                screensReadingTranscripts(new UnreadableTranscriptView(reason())));

        assertThat(Screen.of(shell).mainViewRows())
                .anySatisfy(row -> assertThat(row).contains("transcript unreadable: no registry"));
    }

    @Test
    void shouldAskAgainFromWhereTheLastReadEndedWhenSessionsChange() {
        SessionQuery screens = screensReadingTranscripts(
                readTranscriptView(prompt("first")), continuingTranscriptView(100, 180, reply("second")));
        Shell shell = shellOnSessionsReading(sessionsViewOf(List.of(sessionView())), screens);
        Screen.of(shell);

        shell.sessionsChanged(sessionsViewOf(List.of(sessionView())));

        assertThat(Screen.of(shell).mainViewRows()).containsSubsequence("› first", "second");
        assertThat(transcriptsAsked(screens, 2).get(1).offset().value()).isEqualTo(100);
    }

    @Test
    void shouldShowTheNewTranscriptInPlaceOfTheOldWhenItStartsAgain() {
        SessionQuery screens = screensReadingTranscripts(
                readTranscriptView(prompt("old first")), readTranscriptView(prompt("rewritten")));
        Shell shell = shellOnSessionsReading(sessionsViewOf(List.of(sessionView())), screens);
        Screen.of(shell);

        shell.sessionsChanged(sessionsViewOf(List.of(sessionView())));

        assertThat(Screen.of(shell).mainViewRows())
                .anySatisfy(row -> assertThat(row).contains("› rewritten"))
                .noneSatisfy(row -> assertThat(row).contains("old first"));
    }

    @Test
    void shouldStartAnotherSessionsTranscriptFromItsOwnStart() {
        SessionQuery screens = screensReadingTranscripts(readTranscriptView(prompt("one")), readTranscriptView(prompt("two")));
        Shell shell = shellOnSessionsReading(
                sessionsViewOf(List.of(sessionView(), namedSessionView(sessionIdOf(2), "other", new Busy()))),
                screens);
        Screen.of(shell);

        shell.press(key('j'));

        assertThat(Screen.of(shell).mainViewRows())
                .anySatisfy(row -> assertThat(row).contains("› two"))
                .noneSatisfy(row -> assertThat(row).contains("› one"));
        TranscriptPayload second = transcriptsAsked(screens, 2).get(1);
        assertThat(second.offset().value()).isZero();
        assertThat(second.session()).isEqualTo(sessionIdOf(2));
    }

    @Test
    void shouldFollowNewEntriesWhileScrolledToTheBottom() {
        SessionQuery screens = screensReadingTranscripts(transcriptViewOfReplies(40), transcriptViewOfMoreReplies(40, 5));
        Shell shell = shellOnSessionsReading(sessionsViewOf(List.of(sessionView())), screens);
        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).isEqualTo("reply 40"));

        shell.sessionsChanged(sessionsViewOf(List.of(sessionView())));

        assertThat(Screen.of(shell).mainViewRows())
                .anySatisfy(row -> assertThat(row).isEqualTo("reply 45"))
                .noneSatisfy(row -> assertThat(row).isEqualTo("reply 1"));
    }

    @Test
    void shouldHoldItsPlaceWhenScrolledUpWhileEntriesArrive() {
        SessionQuery screens = screensReadingTranscripts(transcriptViewOfReplies(40), transcriptViewOfMoreReplies(40, 5));
        Shell shell = shellOnSessionsReading(sessionsViewOf(List.of(sessionView())), screens);
        Screen.of(shell);
        shell.press(key('l'));
        shell.press(key('k'));
        List<String> before = Screen.of(shell).mainViewRows();

        shell.sessionsChanged(sessionsViewOf(List.of(sessionView())));

        assertThat(Screen.of(shell).mainViewRows()).isEqualTo(before);
    }

    @Test
    void shouldFollowAgainOnCapitalGAfterScrollingUp() {
        SessionQuery screens = screensReadingTranscripts(transcriptViewOfReplies(40), transcriptViewOfMoreReplies(40, 5));
        Shell shell = shellOnSessionsReading(sessionsViewOf(List.of(sessionView())), screens);
        Screen.of(shell);
        shell.press(key('l'));
        shell.press(key('g'));
        shell.sessionsChanged(sessionsViewOf(List.of(sessionView())));
        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).isEqualTo("reply 1"));

        shell.press(key('G'));

        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).isEqualTo("reply 45"));
    }

    @Test
    void shouldReadTheTranscriptOffTheRenderThread() {
        HeldTasksFixture held = new HeldTasksFixture();
        SessionQuery screens = screensReadingTranscripts(readTranscriptView(prompt("hello")));
        Shell shell = shellWithSessionsFocused(
                sessionsViewOf(List.of(sessionView())), screens, mock(OutsideSessionCommand.class), held, 1);

        Screen.of(shell);

        verify(screens, never()).transcriptOf(any());
        held.runNewestFirst();
        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).contains("› hello"));
    }

    @Test
    void shouldAskOnceWhileAReadIsStillRunning() {
        HeldTasksFixture held = new HeldTasksFixture();
        SessionQuery screens = screensReadingTranscripts(readTranscriptView(prompt("hello")));
        Shell shell = shellWithSessionsFocused(
                sessionsViewOf(List.of(sessionView())), screens, mock(OutsideSessionCommand.class), held, 1);

        Screen.of(shell);
        shell.sessionsChanged(sessionsViewOf(List.of(sessionView())));
        Screen.of(shell);
        held.runNewestFirst();

        verify(screens, times(1)).transcriptOf(any());
    }

    @Test
    void shouldIgnoreAReadThatLandsAfterAnotherSessionWasSelected() {
        HeldTasksFixture held = new HeldTasksFixture();
        SessionQuery screens = screensReadingTranscripts(readTranscriptView(prompt("from the first")));
        Shell shell = shellWithSessionsFocused(
                sessionsViewOf(List.of(sessionView(), namedSessionView(sessionIdOf(2), "other", new Busy()))),
                screens,
                mock(OutsideSessionCommand.class),
                held,
                1);
        Screen.of(shell);
        shell.press(key('j'));

        held.runNewestFirst();

        assertThat(Screen.of(shell).mainViewRows()).noneSatisfy(row -> assertThat(row).contains("from the first"));
    }
}
