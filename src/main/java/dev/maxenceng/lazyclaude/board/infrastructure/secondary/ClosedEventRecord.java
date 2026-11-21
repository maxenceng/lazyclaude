package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

/** One {@code CLOSED_EVENT} of an issue's {@code timelineItems}: what closed it, null where the JSON lacks it. */
record ClosedEventRecord(CloserRecord closer) {}
