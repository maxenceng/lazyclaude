package dev.maxenceng.lazyclaude.shell;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.DiffFileView;
import dev.maxenceng.lazyclaude.review.published.AddedFile;
import dev.maxenceng.lazyclaude.review.published.AddedLine;
import dev.maxenceng.lazyclaude.review.published.ContextLine;
import dev.maxenceng.lazyclaude.review.published.DeletedFile;
import dev.maxenceng.lazyclaude.review.published.DiffLine;
import dev.maxenceng.lazyclaude.review.published.DiffLineContent;
import dev.maxenceng.lazyclaude.review.published.DiffLineNumber;
import dev.maxenceng.lazyclaude.review.published.FileStatus;
import dev.maxenceng.lazyclaude.review.published.ModifiedFile;
import dev.maxenceng.lazyclaude.review.published.RemovedLine;
import dev.maxenceng.lazyclaude.review.published.RenamedFile;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/** How the review words the rows of a diff: a rule for a file, a dim hunk header, and a line between two number gutters. */
final class DiffText {

    private static final String CURSOR_MARK = "▌";
    private static final String NO_MARK = " ";
    private static final String SPACE = " ";
    private static final String RULE = "─";
    private static final String RULE_LEAD = "── ";
    private static final String RENAMED = "%s → %s";
    private static final String ADDED_SIGN = "+ ";
    private static final String REMOVED_SIGN = "- ";
    private static final String CONTEXT_SIGN = "  ";
    private static final String TAB = "\t";
    private static final String TAB_STOP = "    ";
    private static final String CARRIAGE_RETURN = "\r";
    private static final String NOTHING = "";

    private DiffText() {}

    /** The cells a line row spends before its text: the cursor mark, the old number, the new number and their gaps. */
    static int gutterWidth(int numberWidth) {
        return CURSOR_MARK.length() + numberWidth + SPACE.length() + numberWidth + SPACE.length();
    }

    /** The screen rows of {@code row} in {@code width} cells, the cursor row reversed edge to edge. */
    static List<List<StatusSpan>> rows(DiffRow row, int width, int numberWidth, boolean cursor) {
        return switch (row) {
            case FileHeaderRow header -> List.of(clipped(fileHeader(header.file(), width), width));
            case HunkHeaderRow header ->
                List.of(clipped(List.of(new StatusSpan(header.header().value(), Style.EMPTY.dim())), width));
            case LineRow line -> List.of(lineRow(line.line(), width, numberWidth, cursor));
            case ThreadRow thread -> ThreadText.rows(thread.thread(), width, gutterWidth(numberWidth)).stream()
                    .map(spans -> clipped(spans, width))
                    .toList();
            case PendingRow pending -> PendingText.rows(pending.comment(), width, gutterWidth(numberWidth)).stream()
                    .map(spans -> clipped(spans, width))
                    .toList();
        };
    }

    /** How many screen rows {@code row} takes in {@code width} cells. */
    static int height(DiffRow row, int width, int numberWidth) {
        return switch (row) {
            case ThreadRow thread -> ThreadText.rows(thread.thread(), width, gutterWidth(numberWidth)).size();
            case PendingRow pending -> PendingText.rows(pending.comment(), width, gutterWidth(numberWidth)).size();
            case FileHeaderRow _, HunkHeaderRow _, LineRow _ -> 1;
        };
    }

    /** A file's name as the Files panel and the rule name it: {@code old → new} for a renamed one. */
    static String label(DiffFileView file) {
        return file.status() instanceof RenamedFile renamed
                ? RENAMED.formatted(renamed.from().value(), file.path().value())
                : file.path().value();
    }

    /** The letter that says what the pull request does to a file. */
    static String letter(FileStatus status) {
        return switch (status) {
            case AddedFile _ -> "A";
            case ModifiedFile _ -> "M";
            case DeletedFile _ -> "D";
            case RenamedFile _ -> "R";
        };
    }

    static Color letterColour(FileStatus status) {
        return switch (status) {
            case AddedFile _ -> Color.GREEN;
            case ModifiedFile _ -> Color.YELLOW;
            case DeletedFile _ -> Color.RED;
            case RenamedFile _ -> Color.BLUE;
        };
    }

    private static List<StatusSpan> fileHeader(DiffFileView file, int width) {
        String lead = RULE_LEAD + label(file) + SPACE;
        return List.of(new StatusSpan(lead + RULE.repeat(Math.max(0, width - lead.codePointCount(0, lead.length()))), Style.EMPTY.bold()));
    }

    private static List<StatusSpan> lineRow(DiffLine line, int width, int numberWidth, boolean cursor) {
        Style reversal = cursor ? Style.EMPTY.reversed() : Style.EMPTY;
        StatusSpan gutter = new StatusSpan(
                (cursor ? CURSOR_MARK : NO_MARK) + number(oldNumber(line), numberWidth) + SPACE + number(newNumber(line), numberWidth) + SPACE,
                Style.EMPTY.dim().patch(reversal));
        StatusSpan text = new StatusSpan(sign(line) + printable(content(line)), colour(line).patch(reversal));
        List<StatusSpan> spans = clipped(List.of(gutter, text), width);
        int used = spans.stream().mapToInt(StatusSpan::width).sum();
        return cursor
                ? Stream.concat(spans.stream(), Stream.of(new StatusSpan(SPACE.repeat(width - used), reversal))).toList()
                : spans;
    }

    private static String number(Optional<DiffLineNumber> number, int width) {
        String digits = number.map(found -> String.valueOf(found.value())).orElse(NOTHING);
        return SPACE.repeat(Math.max(0, width - digits.length())) + digits;
    }

    private static Optional<DiffLineNumber> oldNumber(DiffLine line) {
        return switch (line) {
            case AddedLine _ -> Optional.empty();
            case RemovedLine removed -> Optional.of(removed.oldNumber());
            case ContextLine kept -> Optional.of(kept.oldNumber());
        };
    }

    private static Optional<DiffLineNumber> newNumber(DiffLine line) {
        return switch (line) {
            case AddedLine added -> Optional.of(added.newNumber());
            case RemovedLine _ -> Optional.empty();
            case ContextLine kept -> Optional.of(kept.newNumber());
        };
    }

    private static DiffLineContent content(DiffLine line) {
        return switch (line) {
            case AddedLine added -> added.content();
            case RemovedLine removed -> removed.content();
            case ContextLine kept -> kept.content();
        };
    }

    private static String sign(DiffLine line) {
        return switch (line) {
            case AddedLine _ -> ADDED_SIGN;
            case RemovedLine _ -> REMOVED_SIGN;
            case ContextLine _ -> CONTEXT_SIGN;
        };
    }

    private static Style colour(DiffLine line) {
        return switch (line) {
            case AddedLine _ -> Style.EMPTY.fg(Color.GREEN);
            case RemovedLine _ -> Style.EMPTY.fg(Color.RED);
            case ContextLine _ -> Style.EMPTY;
        };
    }

    /** The content as a terminal can draw it: tabs spread to a stop, the carriage return of a CRLF file left out. */
    private static String printable(DiffLineContent content) {
        return content.value().replace(CARRIAGE_RETURN, NOTHING).replace(TAB, TAB_STOP);
    }

    /** The spans cut to {@code width} cells. */
    static List<StatusSpan> clipped(List<StatusSpan> spans, int width) {
        if (spans.isEmpty() || width <= 0) {
            return List.of();
        }
        StatusSpan first = spans.getFirst();
        return first.width() >= width
                ? List.of(first.clipped(width))
                : Stream.concat(Stream.of(first), clipped(spans.subList(1, spans.size()), width - first.width()).stream())
                        .toList();
    }
}
