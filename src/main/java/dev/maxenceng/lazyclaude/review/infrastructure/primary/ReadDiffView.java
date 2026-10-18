package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.ReadDiff;

/** The files of the diff, in the order the diff lists them. */
public record ReadDiffView(List<DiffFileView> files) implements PullRequestDiffView {



    static ReadDiffView from(ReadDiff read) {
        return new ReadDiffView(read.diff().files().stream().map(DiffFileView::from).toList());
    }
}
