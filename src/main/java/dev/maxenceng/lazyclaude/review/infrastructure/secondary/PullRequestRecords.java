package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.error.domain.DomainException;
import dev.maxenceng.lazyclaude.review.domain.PullRequest;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Reads the JSON {@code gh pr list} writes into pull requests; the one place Jackson is used for them. */
final class PullRequestRecords {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private PullRequestRecords() {}

    /** @throws PullRequestsUnavailableException saying what {@code source} held that could not be read */
    static List<PullRequest> pullRequests(byte[] json, String source) {
        PullRequestRecord[] records = records(json, source);
        try {
            return Arrays.stream(records).map(PullRequestRecord::toPullRequest).toList();
        } catch (DomainException refused) {
            throw new PullRequestsUnavailableException(
                    "%s is not a pull request: %s".formatted(source, refused.getMessage()), refused);
        }
    }

    private static PullRequestRecord[] records(byte[] json, String source) {
        try {
            return MAPPER.readValue(json, PullRequestRecord[].class);
        } catch (JacksonException e) {
            throw new PullRequestsUnavailableException(
                    "%s does not parse: %s".formatted(source, e.getOriginalMessage()), e);
        }
    }
}
