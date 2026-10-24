package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;

/** Reads the JSON {@code gh pr list} writes into open pull requests, through {@link GhJson}. */
final class PullRequestRecords {

    private PullRequestRecords() {}

    /** @throws PullRequestsUnavailableException saying what {@code source} held that could not be read */
    static List<PullRequest> pullRequests(byte[] json, String source) {
        PullRequestRecord[] records = GhJson.records(json, PullRequestRecord[].class, source);
        return Arrays.stream(records)
                .filter(record -> !record.fromAFork())
                .map(record -> complete(record, source))
                .map(PullRequestRecord::toPullRequest)
                .toList();
    }

    private static PullRequestRecord complete(PullRequestRecord record, String source) {
        if (record.missingFields().any()) {
            throw new PullRequestsUnavailableException(
                    "%s wrote a pull request without %s".formatted(source, record.missingFields().listed()));
        }
        return record;
    }
}
