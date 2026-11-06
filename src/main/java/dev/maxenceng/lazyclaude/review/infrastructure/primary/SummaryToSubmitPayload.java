package dev.maxenceng.lazyclaude.review.infrastructure.primary;

/** What {@link ReviewQuery} is asked the summary to submit with: the pending review as last read, and the summary typed, if any. */
public record SummaryToSubmitPayload(PendingReviewView pending, SummaryDraftPayload draft) {}
