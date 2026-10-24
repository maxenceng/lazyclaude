package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.MergedPullRequest;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;

/** Reads the JSON {@code gh pr list --state merged} writes into merged pull requests, leaving out the ones from a fork. */
final class MergedPullRequestRecords {

    private MergedPullRequestRecords() {}

    /** @throws PullRequestsUnavailableException saying what {@code source} held that could not be read */
    static List<MergedPullRequest> pullRequests(byte[] json, String source) {
        MergedPullRequestRecord[] records = GhJson.records(json, MergedPullRequestRecord[].class, source);
        return Arrays.stream(records)
                .filter(record -> !record.fromAFork())
                .map(record -> complete(record, source))
                .map(MergedPullRequestRecord::toMergedPullRequest)
                .toList();
    }

    private static MergedPullRequestRecord complete(MergedPullRequestRecord record, String source) {
        if (record.missingFields().any()) {
            throw new PullRequestsUnavailableException(
                    "%s wrote a merged pull request without %s".formatted(source, record.missingFields().listed()));
        }
        return record;
    }
}
