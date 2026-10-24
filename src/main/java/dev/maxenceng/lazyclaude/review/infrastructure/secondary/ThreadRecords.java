package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;
import java.util.Optional;

import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.domain.ReviewThread;

/** Reads the pages {@code gh api graphql --paginate} writes into review threads, through {@link GhJson}. */
final class ThreadRecords {

    private ThreadRecords() {}

    /** @throws PullRequestsUnavailableException saying what {@code source} held that could not be read */
    static List<ReviewThread> threads(byte[] json, String source) {
        List<ThreadRecord> records = GhJson.pages(json, source).stream()
                .flatMap(page -> nodesOf(page).orElseThrow(() -> new PullRequestsUnavailableException("%s holds no review threads".formatted(source))).stream())
                .toList();
        return records.stream()
                .map(record -> complete(record, source))
                .filter(ThreadRecord::hasSubmittedComments)
                .map(ThreadRecord::toThread)
                .toList();
    }

    private static ThreadRecord complete(ThreadRecord record, String source) {
        if (record.missingFields().any()) {
            throw new PullRequestsUnavailableException(
                    "%s wrote a review thread without %s".formatted(source, record.missingFields().listed()));
        }
        return record;
    }

    private static Optional<List<ThreadRecord>> nodesOf(ThreadPageRecord page) {
        return Optional.ofNullable(page.data())
                .map(ThreadPageDataRecord::repository)
                .map(ThreadPageRepositoryRecord::pullRequest)
                .map(ThreadPagePullRequestRecord::reviewThreads)
                .map(ThreadConnectionRecord::nodes);
    }
}
