package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.board.domain.BacklogUnavailableException;
import dev.maxenceng.lazyclaude.board.domain.GitHubIssue;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Reads the pages {@code gh api graphql --paginate --slurp} writes into entries; the one place Jackson is used for it. */
final class IssueRecords {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String SOURCE = "gh api graphql";
    private static final String ISSUES = "/data/repository/issues/nodes";
    // deferred: only the 1000 newest issues are shown, though gh still fetches every page — close it when a project has that many.
    private static final int LIMIT = 1000;

    private IssueRecords() {}

    /** @throws BacklogUnavailableException saying what {@code gh api graphql} printed that does not parse, or the field it left out */
    static List<BacklogEntry> entries(byte[] json) {
        return pages(json)
                .flatMap(page -> records(page).stream())
                .limit(LIMIT)
                .map(IssueRecords::complete)
                .map(IssueRecord::issue)
                .map(GitHubIssue::entry)
                .toList();
    }

    private static Stream<JsonNode> pages(byte[] json) {
        JsonNode pages = tree(json);
        if (!pages.isArray() || pages.isEmpty()) {
            throw new BacklogUnavailableException("%s wrote no page of issues".formatted(SOURCE));
        }
        return pages.valueStream();
    }

    private static JsonNode tree(byte[] json) {
        try {
            return MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new BacklogUnavailableException("%s does not parse: %s".formatted(SOURCE, e.getOriginalMessage()), e);
        }
    }

    private static List<IssueRecord> records(JsonNode page) {
        JsonNode nodes = page.at(ISSUES);
        if (!nodes.isArray()) {
            throw new BacklogUnavailableException("%s wrote a page without issues".formatted(SOURCE));
        }
        try {
            return nodes.valueStream().map(IssueRecords::record).toList();
        } catch (JacksonException e) {
            throw new BacklogUnavailableException("%s does not parse: %s".formatted(SOURCE, e.getOriginalMessage()), e);
        }
    }

    private static IssueRecord record(JsonNode node) {
        if (!node.isObject()) {
            throw new BacklogUnavailableException("%s wrote an issue that is not an object: %s".formatted(SOURCE, node));
        }
        return MAPPER.treeToValue(node, IssueRecord.class);
    }

    private static IssueRecord complete(IssueRecord record) {
        if (record.missingFields().any()) {
            throw new BacklogUnavailableException("%s wrote an issue without %s".formatted(SOURCE, record.missingFields().listed()));
        }
        return record;
    }
}
