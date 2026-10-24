package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

/** A hunk line as written, with the numbers the next line of the hunk would take on the old and the new side. */
record ScannedDiffLine(String written, int nextOld, int nextNew) {

    private static final int NO_STEP = 0;
    private static final int ONE_LINE = 1;
    private static final char ADDED = '+';
    private static final char REMOVED = '-';

    boolean added() {
        return startsWith(ADDED);
    }

    boolean removed() {
        return startsWith(REMOVED);
    }

    /** The line after this one: an added line takes no old number, a removed one no new number. */
    ScannedDiffLine then(String next) {
        return new ScannedDiffLine(next, nextOld + (marked(next, ADDED) ? NO_STEP : ONE_LINE), nextNew + (marked(next, REMOVED) ? NO_STEP : ONE_LINE));
    }

    /** What the line holds after its one-character marker. */
    String content() {
        return written.isEmpty() ? written : written.substring(1);
    }

    private boolean startsWith(char marker) {
        return marked(written, marker);
    }

    private static boolean marked(String line, char marker) {
        return !line.isEmpty() && line.charAt(0) == marker;
    }
}
