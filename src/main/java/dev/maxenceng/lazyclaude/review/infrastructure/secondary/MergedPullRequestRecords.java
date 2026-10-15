package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.review.domain.MergedPullRequest;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;

/** Reads the JSON {@code gh pr list --state merged} writes into merged pull requests, leaving out the ones from a fork. */
final class MergedPullRequestRecords {

    private MergedPullRequestRecords() {}

    /** @throws PullRequestsUnavailableException saying what {@code source} held that could not be read */
    static List<MergedPullRequest> pullRequests(byte[] json, String source) {
        MergedPullRequestRecord[] records = GhJson.records(json, MergedPullRequestRecord[].class, source);
        try {
            return Arrays.stream(records)
                    .filter(record -> !record.fromAFork())
                    .map(MergedPullRequestRecord::toMergedPullRequest)
                    .toList();
        } catch (DomainException refused) {
            throw new PullRequestsUnavailableException(
                    "%s is not a pull request: %s".formatted(source, refused.getMessage()), refused);
        }
    }
}
