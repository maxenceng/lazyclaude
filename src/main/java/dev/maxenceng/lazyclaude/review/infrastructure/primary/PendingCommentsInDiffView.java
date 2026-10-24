package dev.maxenceng.lazyclaude.review.infrastructure.primary;

import java.util.List;

import dev.maxenceng.lazyclaude.review.domain.PendingCommentsInDiff;

/** The pending comments in diff order, each with its line: by the line they show under, the ones on no line last. */
public record PendingCommentsInDiffView(List<PendingInDiffView> comments) {

    static PendingCommentsInDiffView from(PendingCommentsInDiff placed) {
        return new PendingCommentsInDiffView(placed.comments().stream().map(PendingInDiffView::from).toList());
    }
}
