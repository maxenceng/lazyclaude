package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;

/** Reads the JSON {@code gh pr list} writes into open pull requests, through {@link GhJson}. */
final class PullRequestRecords {

    private PullRequestRecords() {}

    /** @throws PullRequestsUnavailableException saying what {@code source} held that could not be read */
    static List<PullRequest> pullRequests(byte[] json, String source) {
        PullRequestRecord[] records = GhJson.records(json, PullRequestRecord[].class, source);
        try {
            return Arrays.stream(records)
                    .filter(record -> !record.fromAFork())
                    .map(PullRequestRecord::toPullRequest).toList();
        } catch (DomainException refused) {
            throw new PullRequestsUnavailableException(
                    "%s is not a pull request: %s".formatted(source, refused.getMessage()), refused);
        }
    }
}
