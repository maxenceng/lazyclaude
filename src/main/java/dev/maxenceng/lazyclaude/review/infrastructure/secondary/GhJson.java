package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.json.JsonMapper;

/** Reads the JSON {@code gh} writes into records; the one place Jackson is used for it. */
final class GhJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private GhJson() {}

    /** @throws PullRequestsUnavailableException saying what {@code source} printed that does not parse */
    static <T> T[] records(byte[] json, Class<T[]> type, String source) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JacksonException e) {
            throw new PullRequestsUnavailableException(
                    "%s does not parse: %s".formatted(source, e.getOriginalMessage()), e);
        }
    }

    /** The values {@code gh --paginate} wrote one after another, a page each; none for no output at all. @throws PullRequestsUnavailableException saying what {@code source} printed that does not parse */
    static <T> List<T> pages(byte[] json, Class<T> type, String source) {
        try (MappingIterator<T> pages = MAPPER.readerFor(type).readValues(json)) {
            return pages.readAll();
        } catch (JacksonException e) {
            throw new PullRequestsUnavailableException(
                    "%s does not parse: %s".formatted(source, e.getOriginalMessage()), e);
        }
    }
}
