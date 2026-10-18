package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import dev.maxenceng.lazyclaude.review.domain.DiffReading;
import dev.maxenceng.lazyclaude.review.domain.LoadingDiff;
import dev.maxenceng.lazyclaude.review.domain.ReadDiff;
import dev.maxenceng.lazyclaude.review.domain.UnavailableDiff;

/** A pull request's diff as the shell reads it: not read yet, the diff, or why GitHub could not be asked for it. */
public sealed interface PullRequestDiffView permits LoadingDiffView, ReadDiffView, UnavailableDiffView {

    static PullRequestDiffView from(DiffReading reading) {
        return switch (reading) {
            case LoadingDiff _ -> new LoadingDiffView();
            case ReadDiff read -> ReadDiffView.from(read);
            case UnavailableDiff unavailable -> UnavailableDiffView.from(unavailable);
        };
    }
}
