package dev.maxenceng.lazyclaude.board.infrastructure.secondary;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import dev.maxenceng.lazyclaude.board.domain.BacklogEntry;
import dev.maxenceng.lazyclaude.shared.domain.Project;

public final class GhContractFixture {

    private static final Duration PATIENCE = Duration.ofSeconds(30);
    private static final String REPOSITORY_WITH_ISSUES = "maxenceng/next-suggestions";
    private static final String REPOSITORY_FLAG = "-R";

    private GhContractFixture() {}

    public static Project thisRepository() {
        return new Project(Path.of("").toAbsolutePath());
    }

    public static String aRepositoryWithIssues() {
        return REPOSITORY_WITH_ISSUES;
    }

    public static List<BacklogEntry> issuesOf(String repository) {
        GhResult listed = new GhCommand("gh", PATIENCE)
                .run(thisRepository(), Stream.concat(IssuesRepository.ARGUMENTS.stream(), Stream.of(REPOSITORY_FLAG, repository)).toList());
        return IssueRecords.entries(listed.output());
    }
}
