package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewThreadView;
import dev.maxenceng.lazyclaude.review.published.ThreadComment;

/** How the review words a comment box drawn as a popup over the main view: its title, and the thread drawn above a reply to it. */
final class BoxPopupText {

    private static final String SUMMARY = "Summary";
    private static final String REPLY = "Reply";
    private static final String COMMENT = "Pending comment";
    private static final String AUTHOR_GAP = "  ";
    private static final int MIN_INNER = 20;
    private static final int MAX_INNER = 70;
    private static final int POPUP_FRAME = 6;

    private BoxPopupText() {}

    /** The room the popup's text has when the main view is {@code width} cells wide. */
    static int inner(int width) {
        return Math.clamp(width - POPUP_FRAME, MIN_INNER, MAX_INNER);
    }

    /** The summary's title, or the first line of the thread a reply answers. */
    static String title(CommentBox box) {
        return switch (box.target()) {
            case SummaryTarget _ -> SUMMARY;
            case ReplyTarget reply -> firstLine(reply.thread());
            case EditCommentTarget _, NewCommentTarget _ -> COMMENT;
        };
    }

    /** The thread a reply answers, drawn above the box; nothing above the summary or a comment. */
    static List<String> above(CommentBox box, int inner) {
        return switch (box.target()) {
            case ReplyTarget reply ->
                reply.thread().comments().stream().flatMap(comment -> lines(comment, inner)).toList();
            case SummaryTarget _, EditCommentTarget _, NewCommentTarget _ -> List.of();
        };
    }

    private static String firstLine(ReviewThreadView thread) {
        return thread.comments().stream()
                .findFirst()
                .map(comment -> comment.body().value().lines().findFirst().orElse(REPLY))
                .orElse(REPLY);
    }

    private static Stream<String> lines(ThreadComment comment, int inner) {
        return Wrapping.lines(comment.author().value() + AUTHOR_GAP + comment.body().value(), inner).stream();
    }
}
