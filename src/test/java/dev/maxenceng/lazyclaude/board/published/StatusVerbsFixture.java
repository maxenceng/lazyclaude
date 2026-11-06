package dev.maxenceng.lazyclaude.board.published;

import java.util.List;

public final class StatusVerbsFixture {

    private StatusVerbsFixture() {}

    public static StatusVerbs inReviewDoneThenRespond() {
        return StatusVerbs.builder().status(Status.IN_REVIEW).verbs(List.of(Verb.DONE, Verb.RESPOND)).build();
    }

    public static StatusVerbs todoWithoutVerbs() {
        return StatusVerbs.builder().status(Status.TODO).verbs(List.of()).build();
    }

    public static StatusVerbs inProgressDesignThenReview() {
        return StatusVerbs.builder().status(Status.IN_PROGRESS).verbs(List.of(Verb.DESIGN, Verb.REVIEW)).build();
    }

    public static StatusVerbs inProgressReviewOnly() {
        return StatusVerbs.builder().status(Status.IN_PROGRESS).verbs(List.of(Verb.REVIEW)).build();
    }
}
