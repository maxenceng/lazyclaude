package dev.maxenceng.lazyclaude.review.published;

public final class CommentBodyFixture {

    private CommentBodyFixture() {}

    public static CommentBody commentBody() {
        return new CommentBody("Does this need its own Element?");
    }

    public static CommentBody editedCommentBody() {
        return new CommentBody("It does, for the cursor.");
    }

    public static CommentBody blankCommentBody() {
        return new CommentBody("  \n");
    }
}
