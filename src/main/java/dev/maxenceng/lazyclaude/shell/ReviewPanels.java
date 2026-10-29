package dev.maxenceng.lazyclaude.shell;

import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.richText;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dev.tamboui.layout.Rect;
import dev.tamboui.toolkit.element.Element;

/** The panels of an open review: Files, Comments and Review in the left column, the diff in the main view, and a comment box drawn as a popup. */
final class ReviewPanels {

    private static final String FILES_TITLE = "Files";
    private static final String COMMENTS_TITLE = "Comments";
    private static final String REVIEW_PANEL_TITLE = "Review";
    private static final String REVIEW_TITLE = "PR #%d %s";
    private static final int REVIEW_PANEL_HEIGHT = 6;
    private static final int NO_SELECTION = -1;
    private static final int FALLBACK_WIDTH = 80;
    private static final int POPUP_BORDERS = 2;

    private ReviewPanels() {}

    static Element files(ReviewPane pane, Focus focus, Consumer<RowsDrawn> drawn) {
        Element body = new RowsElement(ReviewText.fileLines(pane), pane.selectedFile(), true, drawn);
        return Frames.framed(FILES_TITLE, body, focus == Focus.FILES).fill();
    }

    /** The comments; {@code drawn} is told which row went where, or {@code noticeDrawn} the area its notice was drawn in. */
    static Element comments(ReviewPane pane, Focus focus, Consumer<RowsDrawn> drawn, Consumer<Rect> noticeDrawn) {
        Element body = ReviewText.commentsNotice(pane.threads())
                .<Element>map(notice -> new PaneProbe(richText(notice).wrapWord().dim(), noticeDrawn))
                .orElseGet(() -> new RowsElement(ReviewText.commentLines(pane), pane.selectedComment(), focus == Focus.COMMENTS, drawn));
        return Frames.framed(COMMENTS_TITLE, body, focus == Focus.COMMENTS).fill();
    }

    static Element review(ReviewPane pane) {
        return panel(REVIEW_PANEL_TITLE, new RowsElement(ReviewText.reviewLines(pane.pending(), pane.summaryToSubmit()), NO_SELECTION, false))
                .rounded()
                .length(REVIEW_PANEL_HEIGHT);
    }

    /** The diff, titled with the pull request; {@code drawn} is told the area it was drawn in and {@code linesDrawn} which line went where. */
    static Element diff(ReviewPane pane, Focus focus, Consumer<Rect> drawn, Consumer<DiffDrawn> linesDrawn) {
        Element body = ReviewText.diffNotice(pane.diff())
                .<Element>map(notice -> richText(notice).wrapWord().dim())
                .orElseGet(() -> new DiffElement(pane, linesDrawn));
        return Frames.framed(REVIEW_TITLE.formatted(pane.number().value(), pane.title().value()), new PaneProbe(body, drawn), focus == Focus.MAIN)
                .titleEllipsis()
                .fill();
    }

    /** The box of the summary, or of a comment or reply whose line is not in the diff, over the main view last drawn in {@code mainView}. */
    static Element boxPopup(CommentBox open, Supplier<Optional<Rect>> mainView) {
        int inner = BoxPopupText.inner(mainView.get().map(Rect::width).orElse(FALLBACK_WIDTH));
        List<String> above = BoxPopupText.above(open, inner);
        int room = mainView.get().map(area -> area.height() - POPUP_BORDERS).orElse(Integer.MAX_VALUE);
        return new PopupElement(
                BoxPopupText.title(open),
                BoxText.popupRows(open, inner, above, room),
                Optional.empty(),
                BoxText.HINT,
                mainView,
                open.saving() ? Optional.empty() : Optional.of(BoxText.popupCaret(open, inner, above, room)));
    }
}
