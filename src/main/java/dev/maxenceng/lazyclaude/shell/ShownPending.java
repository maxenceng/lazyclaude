package dev.maxenceng.lazyclaude.shell;

import dev.maxenceng.lazyclaude.review.infrastructure.primary.PendingReviewView;
import dev.maxenceng.lazyclaude.session.published.LinkedTicketId;

/** The pending review last read for a ticket's pull request, kept with the ticket it is about. */
record ShownPending(LinkedTicketId ticket, PendingReviewView view) {}
