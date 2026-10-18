package dev.maxenceng.lazyclaude.review.infrastructure.secondary;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.PullRequestDiff;
import dev.maxenceng.lazyclaude.review.domain.PullRequestDiffPort;
import dev.maxenceng.lazyclaude.review.domain.PullRequestDiffTooLargeException;
import dev.maxenceng.lazyclaude.review.domain.PullRequestsUnavailableException;
import dev.maxenceng.lazyclaude.review.published.PullRequestNumber;
import dev.maxenceng.lazyclaude.shared.domain.Project;

/**
 * Asks {@code gh pr diff} for a pull request's unified diff of the repository the project's directory belongs to, in one call. Any way
 * {@code gh} fails to answer is the one unavailable reason, in the words {@code gh} used; GitHub's refusal of a diff over its limits is
 * told apart by the status it answers with, since the screen words it differently.
 */
public final class GhPullRequestDiffRepository implements PullRequestDiffPort {

    private static final String TOO_LARGE_STATUS = "HTTP 406";

    private final GhCommand gh;

    public GhPullRequestDiffRepository(String executable, Duration patience) {
        this.gh = new GhCommand(executable, patience);
    }

    @Override
    public PullRequestDiff diff(Project project, PullRequestNumber number) {
        List<String> arguments = List.of("pr", "diff", String.valueOf(number.value()), "--color", "never");
        try {
            return UnifiedDiff.parse(new String(gh.output(project, arguments), StandardCharsets.UTF_8));
        } catch (PullRequestsUnavailableException noAnswer) {
            if (noAnswer.getMessage().contains(TOO_LARGE_STATUS)) {
                throw new PullRequestDiffTooLargeException(noAnswer.getMessage(), noAnswer);
            }
            throw noAnswer;
        }
    }
}
