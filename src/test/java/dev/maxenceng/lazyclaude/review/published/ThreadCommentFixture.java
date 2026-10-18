package dev.maxenceng.lazyclaude.review.published;

public final class ThreadCommentFixture {

    private ThreadCommentFixture() {}

    public static ThreadComment comment(String author, String body) {
        return ThreadComment.builder().author(new CommentAuthor(author)).body(new CommentBody(body)).build();
    }

    public static ThreadComment question() {
        return comment("alice", "Why not reuse it?");
    }

    public static ThreadComment answer() {
        return comment("bob", "It assumes markdown.");
    }

    public static ThreadComment replyFromALongAuthor() {
        return comment("dependabot[bot]", "Bumps the version of the parser.");
    }
}
