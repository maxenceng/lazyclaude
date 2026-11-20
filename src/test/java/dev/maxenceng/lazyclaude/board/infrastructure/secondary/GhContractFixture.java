package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.shared.domain.Project;

public final class GhContractFixture {

    private static final Duration PATIENCE = Duration.ofSeconds(30);

    private GhContractFixture() {}

    public static Project thisRepository() {
        return new Project(Path.of("").toAbsolutePath());
    }

    public static List<BacklogEntry> issuesOfThisRepository() {
        GhResult listed = new GhCommand("gh", PATIENCE).run(thisRepository(), IssuesRepository.ARGUMENTS);
        return IssueRecords.entries(listed.output());
    }
}
