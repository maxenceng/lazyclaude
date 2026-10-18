package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.PullRequestDiff;
import dev.maxenceng.lazyclaude.review.domain.ReviewThread;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.shared.domain.Project;

public final class GhContractFixture {

    private static final Duration PATIENCE = Duration.ofSeconds(30);

    private GhContractFixture() {}

    public static Project thisRepository() {
        return new Project(Path.of("").toAbsolutePath());
    }

    public static PullRequestNumber aMergedPullRequestWithAThread() {
        return new PullRequestNumber(32);
    }

    public static PullRequestDiff realDiff() {
        return new GhPullRequestDiffRepository("gh", PATIENCE).diff(thisRepository(), aMergedPullRequestWithAThread());
    }

    public static List<ReviewThread> realThreads() {
        return new GhThreadRepository("gh", PATIENCE).threads(thisRepository(), aMergedPullRequestWithAThread());
    }
}
