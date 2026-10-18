package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingAnchorView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingCommentView;
import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReplyAnchorView;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/** How the review words a pending comment shown under its line: its mark and lines in the pending colour, you as its author, then its text. */
final class PendingText {

    static final Color COLOUR = Color.LIGHT_BLUE;

    private static final String MARK = "✎ ";
    private static final String REPLY = "reply";
    private static final String YOU = " you";
    private static final String SPACE = " ";
    private static final int BODY_INDENT = 2;

    private PendingText() {}

    /** The screen rows of {@code comment} in {@code width} cells, set in past {@code gutter} cells. */
    static List<List<StatusSpan>> rows(PendingCommentView comment, int width, int gutter) {
        String indent = SPACE.repeat(gutter);
        Stream<List<StatusSpan>> header = Stream.of(List.of(
                new StatusSpan(indent, Style.EMPTY),
                new StatusSpan(MARK + where(comment.anchor()), Style.EMPTY.fg(COLOUR)),
                new StatusSpan(YOU, Style.EMPTY.bold())));
        Stream<List<StatusSpan>> body = Wrapping.lines(comment.body().value(), width - gutter - BODY_INDENT).stream()
                .map(line -> List.of(new StatusSpan(indent + SPACE.repeat(BODY_INDENT) + line, Style.EMPTY)));
        return Stream.concat(header, body).toList();
    }

    private static String where(PendingAnchorView anchor) {
        return anchor instanceof ReplyAnchorView || !(anchor.position() instanceof PlacedThread placed)
                ? REPLY
                : ThreadText.range(placed);
    }
}
