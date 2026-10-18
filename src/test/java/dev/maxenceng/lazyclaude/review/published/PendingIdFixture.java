package dev.maxenceng.lazyclaude.review.published;

public final class PendingIdFixture {

    private PendingIdFixture() {}

    public static PendingCommentId pendingCommentId() {
        return new PendingCommentId("PRRC_1");
    }

    public static PendingCommentId secondPendingCommentId() {
        return new PendingCommentId("PRRC_2");
    }

    public static ThreadId threadId() {
        return new ThreadId("PRRT_1");
    }

    public static ThreadId secondThreadId() {
        return new ThreadId("PRRT_2");
    }
}
