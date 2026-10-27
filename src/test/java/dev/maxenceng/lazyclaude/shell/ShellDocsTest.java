package dev.maxenceng.lazyclaude.shell;

import static dev.maxenceng.lazyclaude.board.infrastructure.primary.BoardViewFixture.boardViewOf;
import static dev.maxenceng.lazyclaude.board.infrastructure.primary.TicketViewFixture.ticketView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.adrFolderView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.docsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.docsViewOf;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.docsViewOfDocuments;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.documentView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.emptyDocumentView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.longDocumentView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.longEditedDocumentView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.namedDocumentView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.nestedDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.secondDocumentView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.unavailableDocsView;
import static dev.maxenceng.lazyclaude.docs.infrastructure.primary.DocsViewFixture.unreadableDocumentView;
import static dev.maxenceng.lazyclaude.session.infrastructure.primary.SessionsViewFixture.emptySessionsView;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.alt;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.ctrl;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.enter;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.key;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.newShell;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.pressed;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellFocusedOnMainViewOfDocs;
import static dev.maxenceng.lazyclaude.shell.ShellFixture.shellOnDocs;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ShellDocsTest {

    @Test
    void shouldListTheFilesDirectlyUnderDocsBeforeEachFolderWithItsCount() {
        assertThat(Screen.of(shellOnDocs(docsView())).boardPanelText())
                .containsSubsequence(
                        "context-map.md",
                        "glossary.md",
                        "▾ adr (2)",
                        "  0001-record.md",
                        "  0002-enforce.md",
                        "▾ research (1)",
                        "  tamboui.md");
    }

    @Test
    void shouldIndentNestedFoldersAndCountEveryDocumentBeneath() {
        assertThat(Screen.of(shellOnDocs(nestedDocsView())).boardPanelText())
                .containsSubsequence("▾ adr (2)", "  0001-record.md", "  ▾ superseded (1)", "    0000-old.md");
    }

    @Test
    void shouldFoldAFolderWithEnterOnItsHeaderAndUnfoldItAgain() {
        Shell shell = pressed(shellOnDocs(docsView()), key('j'), 2);

        shell.press(enter());
        assertThat(Screen.of(shell).boardPanelText()).contains("▸ adr (2)").doesNotContain("0001-record.md");
        shell.press(enter());

        assertThat(Screen.of(shell).boardPanelText()).contains("▾ adr (2)", "0001-record.md");
    }

    @Test
    void shouldLeaveADocumentRowAloneOnEnter() {
        Shell shell = shellOnDocs(docsView());

        shell.press(enter());

        assertThat(Screen.of(shell).boardPanelText()).contains("▾ adr (2)", "context-map.md");
    }

    @Test
    void shouldMoveTheSelectionWithJAndKWhileDocsHasFocus() {
        Shell shell = shellOnDocs(docsView());

        pressed(shell, key('j'), 3);
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("0001-record.md");
        shell.press(key('k'));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("▾ adr (2)");
    }

    @Test
    void shouldStopTheSelectionAtBothEnds() {
        Shell shell = shellOnDocs(docsView());

        shell.press(key('k'));
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("context-map.md");
        pressed(shell, key('j'), 40);

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("tamboui.md");
    }

    @Test
    void shouldNotMoveTheBoardSelectionWhileDocsHasFocus() {
        Shell shell = shellOnDocs(docsView());

        pressed(shell, key('j'), 3);

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().doesNotContain("PROJ-1");
    }

    @Test
    void shouldShowTheSelectedDocumentRenderedAsMarkdownInTheMainView() {
        Shell shell = shellOnDocs(docsView());

        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).contains("Context map"));
    }

    @Test
    void shouldShowNothingInTheMainViewOnAFolderHeader() {
        Shell shell = pressed(shellOnDocs(docsView()), key('j'), 2);

        assertThat(Screen.of(shell).mainViewRows()).allSatisfy(row -> assertThat(row).isBlank());
    }

    @Test
    void shouldShowAnotherDocumentWhenTheSelectionMoves() {
        Shell shell = pressed(shellOnDocs(docsView()), key('j'), 1);

        assertThat(Screen.of(shell).mainViewRows())
                .anySatisfy(row -> assertThat(row).contains("Glossary"))
                .noneSatisfy(row -> assertThat(row).contains("Context map"));
    }

    @Test
    void shouldScrollTheDocumentWithTheTicketDetailsKeys() {
        Shell shell = shellFocusedOnMainViewOfDocs(docsViewOfDocuments(longDocumentView()));
        assertThat(Screen.of(shell).mainViewRows().getFirst()).contains("Row 01");

        shell.press(key('j'));
        assertThat(Screen.of(shell).mainViewRows().getFirst()).contains("Row 02");
        shell.press(key('k'));
        assertThat(Screen.of(shell).mainViewRows().getFirst()).contains("Row 01");
        shell.press(key('G'));
        assertThat(Screen.of(shell).mainViewRows().getLast()).contains("Row 60");
        shell.press(key('g'));
        assertThat(Screen.of(shell).mainViewRows().getFirst()).contains("Row 01");
        shell.press(ctrl('d'));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).doesNotContain("Row 01");
    }

    @Test
    void shouldStopScrollingAtBothEnds() {
        Shell shell = shellFocusedOnMainViewOfDocs(docsViewOfDocuments(longDocumentView()));

        shell.press(key('k'));
        assertThat(Screen.of(shell).mainViewRows().getFirst()).contains("Row 01");
        pressed(shell, key('j'), 200);
        Screen.of(shell);

        assertThat(Screen.of(shell).mainViewRows().getLast()).contains("Row 60");
        shell.press(key('k'));
        assertThat(Screen.of(shell).mainViewRows().getLast()).contains("Row 59");
    }

    @Test
    void shouldGoBackToTheTopWhenAnotherDocumentIsSelected() {
        Shell shell = shellFocusedOnMainViewOfDocs(docsViewOfDocuments(longDocumentView(), secondDocumentView()));
        shell.press(key('G'));
        Screen.of(shell);
        shell.press(key('h'));

        shell.press(key('j'));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).contains("Glossary");
    }

    @Test
    void shouldShowTheSelectedTicketAgainWhenFocusMovesBackToTheBoard() {
        Shell shell = shellOnDocs(docsView());

        shell.press(alt('k'));
        shell.press(alt('k'));
        shell.press(key('j'));

        assertThat(Screen.of(shell).mainViewRows())
                .anySatisfy(row -> assertThat(row).startsWith("PROJ-1 · draft"))
                .noneSatisfy(row -> assertThat(row).contains("Context map"));
    }

    @Test
    void shouldShowTheSelectedDocumentAgainWhenFocusMovesBackToDocs() {
        Shell shell = pressed(shellOnDocs(docsView()), key('j'), 1);
        pressed(shell, alt('k'), 2);

        pressed(shell, alt('j'), 2);

        assertThat(Screen.of(shell).mainViewRows()).anySatisfy(row -> assertThat(row).contains("Glossary"));
    }

    @Test
    void shouldGiveDocsTheRemainingHeightWhileItHasFocus() {
        Shell shell = shellOnDocs(docsView());

        assertThat(Screen.of(shell).boardPanelText()).contains("tamboui.md");
    }

    @Test
    void shouldShrinkDocsToAFewRowsWhileTheBoardHasFocusKeepingItsSelectionInView() {
        Shell shell = pressed(shellOnDocs(docsView()), key('j'), 6);

        pressed(shell, alt('k'), 2);

        assertThat(Screen.of(shell).boardPanelText())
                .as("the selected row stays in view in the shrunk panel")
                .contains("tamboui.md")
                .doesNotContain("context-map.md");
    }

    @Test
    void shouldGiveTheBoardTheRemainingHeightAgainWhenItRegainsFocus() {
        Shell shell = shellOnDocs(docsView());

        pressed(shell, alt('k'), 2);

        assertThat(Screen.of(shell).boardPanelText()).contains("▾ draft (1)", "PROJ-1").doesNotContain("0002-enforce.md");
    }

    @Test
    void shouldShowADimEmptyDocumentForAFileWithNothingInIt() {
        Shell shell = shellOnDocs(docsViewOfDocuments(emptyDocumentView()));

        assertThat(Screen.of(shell).dimText()).contains("empty document");
    }

    @Test
    void shouldShowThePathAndTheReasonOfAnUnreadableDocument() {
        Shell shell = shellOnDocs(docsViewOfDocuments(unreadableDocumentView()));

        assertThat(Screen.of(shell).boardPanelText()).contains("glossary.md");
        assertThat(Screen.of(shell).mainViewRows())
                .anySatisfy(row -> assertThat(row).contains("glossary.md"))
                .anySatisfy(row -> assertThat(row).contains("cannot be read: AccessDeniedException"));
    }

    @Test
    void shouldShowADimNoDocsWhenThereAreNoDocuments() {
        Shell shell = shellOnDocs(emptyDocsView());

        assertThat(Screen.of(shell).dimText()).contains("no docs");
    }

    @Test
    void shouldShowNothingInTheMainViewWhenThereAreNoDocuments() {
        Shell shell = shellOnDocs(emptyDocsView());

        assertThat(Screen.of(shell).mainViewRows()).allSatisfy(row -> assertThat(row).isBlank());
    }

    @Test
    void shouldIgnoreMovementAndEnterWhenThereAreNoDocuments() {
        Shell shell = shellOnDocs(emptyDocsView());

        pressed(shell, key('j'), 2);
        shell.press(enter());
        shell.press(key('k'));

        assertThat(Screen.of(shell).dimText()).contains("no docs");
    }

    @Test
    void shouldShowADimDocsUnavailableAndTheReasonWhenTheDocsCannotBeListed() {
        Shell shell = shellOnDocs(unavailableDocsView());

        assertThat(Screen.of(shell).dimText())
                .as("lazyclaude starts, and the panel says why there is no tree")
                .contains("docs unavailable")
                .anySatisfy(line -> assertThat(line).startsWith("cannot list"));
    }

    @Test
    void shouldIgnoreMovementAndEnterWhileTheDocsAreUnavailable() {
        Shell shell = shellOnDocs(unavailableDocsView());

        pressed(shell, key('j'), 2);
        shell.press(enter());

        assertThat(Screen.of(shell).dimText()).contains("docs unavailable");
        assertThat(Screen.of(shell).mainViewRows()).allSatisfy(row -> assertThat(row).isBlank());
    }

    @Test
    void shouldSayWhyWhenTheDocsBecomeUnavailableWhileRunning() {
        Shell shell = shellOnDocs(docsView());

        shell.docsChanged(unavailableDocsView());

        assertThat(Screen.of(shell).dimText()).contains("docs unavailable");
        assertThat(Screen.of(shell).boardPanelText()).doesNotContain("context-map.md");
    }

    @Test
    void shouldShowTheTreeAgainOnceTheDocsCanBeListed() {
        Shell shell = shellOnDocs(unavailableDocsView());

        shell.docsChanged(docsView());

        assertThat(Screen.of(shell).boardPanelText()).contains("context-map.md").doesNotContain("docs unavailable");
        assertThat(Screen.of(shell).reversedText())
                .as("the first row is selected, as at start")
                .anySatisfy(line -> assertThat(line).contains("context-map.md"));
    }

    @Test
    void shouldShowNoDocsOnlyInTheDocsPanelWhileTheBoardHasFocus() {
        Shell shell = newShell(boardViewOf(ticketView()), emptyDocsView(), emptySessionsView());

        assertThat(Screen.of(shell).dimText()).contains("no docs");
    }

    @Test
    void shouldListTheDocsKeysWhileDocsHasFocus() {
        Shell shell = shellOnDocs(docsView());

        assertThat(Screen.of(shell).lines().getLast())
                .contains("j/k move", "h/l panel", "alt+h/j/k/l focus", "q quit")
                .doesNotContain("enter fold");
    }

    @Test
    void shouldOfferFoldOnAFolderHeader() {
        Shell shell = pressed(shellOnDocs(docsView()), key('j'), 2);

        assertThat(Screen.of(shell).lines().getLast()).contains("j/k move", "enter fold");
    }

    @Test
    void shouldFollowTheDocumentsPathWhenARowIsAddedAboveIt() {
        Shell shell = pressed(shellOnDocs(docsView()), key('j'), 1);

        shell.docsChanged(docsViewOf(List.of(namedDocumentView("a-new.md"), documentView(), secondDocumentView()), List.of()));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("glossary.md");
    }

    @Test
    void shouldKeepFoldedFoldersFoldedWhenTheTreeChanges() {
        Shell shell = pressed(shellOnDocs(docsView()), key('j'), 2);
        shell.press(enter());

        shell.docsChanged(docsView());

        assertThat(Screen.of(shell).boardPanelText()).contains("▸ adr (2)").doesNotContain("0001-record.md");
    }

    @Test
    void shouldShowAFolderAddedWhileRunningUnfolded() {
        Shell shell = shellOnDocs(docsViewOfDocuments(documentView()));

        shell.docsChanged(docsView());

        assertThat(Screen.of(shell).boardPanelText()).contains("▾ adr (2)", "0001-record.md");
    }

    @Test
    void shouldRedrawAnOpenDocumentAtItsScrollPosition() {
        Shell shell = shellFocusedOnMainViewOfDocs(docsViewOfDocuments(longDocumentView()));
        pressed(shell, key('j'), 3);
        Screen.of(shell);

        shell.docsChanged(docsViewOfDocuments(longEditedDocumentView()));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).contains("Row 04");
    }

    @Test
    void shouldLandOnTheRowThatTookThePlaceOfTheDeletedSelection() {
        Shell shell = pressed(shellOnDocs(docsViewOfDocuments(namedDocumentView("a.md"), namedDocumentView("b.md"), namedDocumentView("c.md"))), key('j'), 1);

        shell.docsChanged(docsViewOfDocuments(namedDocumentView("a.md"), namedDocumentView("c.md")));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("c.md");
    }

    @Test
    void shouldLandOnTheNewLastRowWhenTheLastSelectionIsDeleted() {
        Shell shell = pressed(shellOnDocs(docsViewOfDocuments(namedDocumentView("a.md"), namedDocumentView("b.md"))), key('j'), 1);

        shell.docsChanged(docsViewOfDocuments(namedDocumentView("a.md")));

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("a.md");
    }

    @Test
    void shouldGoBackToTheTopOfTheDocumentThatTookThePlaceOfTheDeletedOne() {
        Shell shell = shellFocusedOnMainViewOfDocs(docsViewOfDocuments(longDocumentView(), secondDocumentView()));
        shell.press(key('G'));
        Screen.of(shell);

        shell.docsChanged(docsViewOfDocuments(secondDocumentView()));

        assertThat(Screen.of(shell).mainViewRows().getFirst()).contains("Glossary");
    }

    @Test
    void shouldShowNoDocsOnceTheLastDocumentIsDeleted() {
        Shell shell = shellOnDocs(docsViewOfDocuments(documentView()));

        shell.docsChanged(emptyDocsView());

        assertThat(Screen.of(shell).dimText()).contains("no docs");
        assertThat(Screen.of(shell).mainViewRows()).allSatisfy(row -> assertThat(row).isBlank());
    }

    @Test
    void shouldSelectTheFirstRowWhenDocumentsAppearInAnEmptyTree() {
        Shell shell = shellOnDocs(emptyDocsView());

        shell.docsChanged(docsView());

        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("context-map.md");
    }

    @Test
    void shouldApplyAChangeThatArrivedBeforeAnyKeyWasPressed() {
        Shell shell = shellOnDocs(docsViewOfDocuments(documentView()));

        shell.docsChanged(docsViewOf(List.of(documentView()), List.of(adrFolderView())));

        assertThat(shell.press(key('j'))).isTrue();
        assertThat(Screen.of(shell).reversedText()).singleElement().asString().contains("adr");
    }

    @Test
    void shouldSayNothingWhenTheTreeChanges() {
        Shell shell = shellOnDocs(docsView());
        List<String> before = Screen.of(shell).lines();

        shell.docsChanged(docsView());

        assertThat(Screen.of(shell).lines()).as("an unchanged tree redraws exactly as it was").isEqualTo(before);
    }
}
