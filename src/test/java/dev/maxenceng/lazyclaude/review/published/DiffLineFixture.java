package dev.maxenceng.lazyclaude.review.published;

public final class DiffLineFixture {

    private DiffLineFixture() {}

    public static AddedLine added(String content, int newNumber) {
        return AddedLine.builder().content(new DiffLineContent(content)).newNumber(new DiffLineNumber(newNumber)).build();
    }

    public static RemovedLine removed(String content, int oldNumber) {
        return RemovedLine.builder().content(new DiffLineContent(content)).oldNumber(new DiffLineNumber(oldNumber)).build();
    }

    public static ContextLine context(String content, int oldNumber, int newNumber) {
        return ContextLine.builder()
                .content(new DiffLineContent(content))
                .oldNumber(new DiffLineNumber(oldNumber))
                .newNumber(new DiffLineNumber(newNumber))
                .build();
    }
}
