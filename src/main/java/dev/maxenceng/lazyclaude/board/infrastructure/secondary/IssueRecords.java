package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.Arrays;
import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.GitHubIssue;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/** Reads the JSON {@code gh issue list} writes into entries; the one place Jackson is used for it. */
final class IssueRecords {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String SOURCE = "gh issue list";

    private IssueRecords() {}

    /** @throws BacklogUnavailableException saying what {@code gh issue list} printed that does not parse, or the field it left out */
    static List<BacklogEntry> entries(byte[] json) {
        return Arrays.stream(records(json)).map(IssueRecords::complete).map(IssueRecord::issue).map(GitHubIssue::entry).toList();
    }

    private static IssueRecord[] records(byte[] json) {
        try {
            return MAPPER.readValue(json, IssueRecord[].class);
        } catch (JacksonException e) {
            throw new BacklogUnavailableException("%s does not parse: %s".formatted(SOURCE, e.getOriginalMessage()), e);
        }
    }

    private static IssueRecord complete(IssueRecord record) {
        if (record.missingFields().any()) {
            throw new BacklogUnavailableException("%s wrote an issue without %s".formatted(SOURCE, record.missingFields().listed()));
        }
        return record;
    }
}
