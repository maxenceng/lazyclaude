package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.domain.ReviewThread;

/** Reads the pages {@code gh api graphql --paginate} writes into review threads, through {@link GhJson}. */
final class ThreadRecords {

    private ThreadRecords() {}

    /** @throws PullRequestsUnavailableException saying what {@code source} held that could not be read */
    static List<ReviewThread> threads(byte[] json, String source) {
        List<ThreadRecord> records = GhJson.pages(json, ThreadPageRecord.class, source).stream()
                .flatMap(page -> nodesOf(page).orElseThrow(() -> new PullRequestsUnavailableException("%s holds no review threads".formatted(source))).stream())
                .toList();
        try {
            return records.stream().filter(ThreadRecord::hasSubmittedComments).map(ThreadRecord::toThread).toList();
        } catch (DomainException refused) {
            throw new PullRequestsUnavailableException(
                    "%s is not a review thread: %s".formatted(source, refused.getMessage()), refused);
        }
    }

    private static Optional<List<ThreadRecord>> nodesOf(ThreadPageRecord page) {
        return Optional.ofNullable(page.data())
                .map(ThreadPageDataRecord::repository)
                .map(ThreadPageRepositoryRecord::pullRequest)
                .map(ThreadPagePullRequestRecord::reviewThreads)
                .map(ThreadConnectionRecord::nodes);
    }
}
