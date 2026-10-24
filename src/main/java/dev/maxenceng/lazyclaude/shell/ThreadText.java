package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.ReviewThreadView;
import dev.maxenceng.lazyclaude.review.published.PlacedThread;
import dev.maxenceng.lazyclaude.review.published.ThreadComment;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/** How the review words a thread shown under its line: its range in the thread colour, then its comments as plain wrapped text. */
final class ThreadText {

    private static final String NO_AUTHOR = "";
    static final Color COLOUR = Color.MAGENTA;

    private static final String MARK = "◆ ";
    private static final String LINE = "L";
    private static final String RANGE_DASH = "–";
    private static final String SPACE = " ";
    private static final String AUTHOR_GAP = "  ";
    private static final int BODY_INDENT = 2;
    private static final int REPLY_INDENT = 4;

    private ThreadText() {}

    /** The line the thread's range is shown as: {@code L12} for one line, {@code L12–18} from its start line to its line. */
    static String range(PlacedThread placed) {
        return LINE + lines(placed);
    }

    /** The line numbers of the range: {@code 12} for one line, {@code 12–18} from its start line to its line. */
    static String lines(PlacedThread placed) {
        return placed.first().equals(placed.last())
                ? String.valueOf(placed.last().value())
                : placed.first().value() + RANGE_DASH + placed.last().value();
    }

    /** The screen rows of {@code thread} in {@code width} cells, set in past {@code gutter} cells; a resolved thread is dimmed. */
    static List<List<StatusSpan>> rows(ReviewThreadView thread, int width, int gutter) {
        Style dimmed = thread.resolved() ? Style.EMPTY.dim() : Style.EMPTY;
        String indent = SPACE.repeat(gutter);
        PlacedThread placed = (PlacedThread) thread.position();
        List<ThreadComment> comments = thread.comments();
        Stream<List<StatusSpan>> header = Stream.of(List.of(
                new StatusSpan(indent, Style.EMPTY),
                new StatusSpan(MARK + range(placed), dimmed.patch(Style.EMPTY.fg(COLOUR))),
                new StatusSpan(
                        comments.isEmpty() ? NO_AUTHOR : SPACE + comments.getFirst().author().value(), dimmed.patch(Style.EMPTY.bold()))));
        Stream<List<StatusSpan>> first = comments.stream()
                .limit(1)
                .flatMap(comment -> Wrapping.lines(comment.body().value(), width - gutter - BODY_INDENT).stream())
                .map(line -> List.of(new StatusSpan(indent + SPACE.repeat(BODY_INDENT) + line, dimmed)));
        Stream<List<StatusSpan>> replies = comments.stream().skip(1).flatMap(reply -> replyRows(reply, width, gutter, dimmed));
        return Stream.of(header, first, replies).flatMap(rows -> rows).toList();
    }

    /** The author in bold and the body beside it, wrapped to the room left of the author, indented under the first comment. */
    private static Stream<List<StatusSpan>> replyRows(ThreadComment reply, int width, int gutter, Style dimmed) {
        String author = reply.author().value();
        int lead = gutter + REPLY_INDENT;
        int hang = author.length() + AUTHOR_GAP.length();
        List<String> lines = Wrapping.lines(reply.body().value(), width - lead - hang);
        if (lines.isEmpty()) {
            return Stream.of(List.of(
                    new StatusSpan(SPACE.repeat(lead), Style.EMPTY), new StatusSpan(author, dimmed.patch(Style.EMPTY.bold()))));
        }
        return IntStream.range(0, lines.size())
                .mapToObj(index -> index == 0
                        ? List.of(
                                new StatusSpan(SPACE.repeat(lead), Style.EMPTY),
                                new StatusSpan(author, dimmed.patch(Style.EMPTY.bold())),
                                new StatusSpan(AUTHOR_GAP + lines.get(index), dimmed))
                        : List.of(new StatusSpan(SPACE.repeat(lead + hang) + lines.get(index), dimmed)));
    }
}
