import { Ci, type PullRequestCells } from '../design-system';
import type { ReviewCheckOutcome, ReviewPullRequestView } from '../protocol';

const WORST_FIRST: readonly ReviewCheckOutcome[] = ['FAILED', 'PENDING', 'PASSED'];

// deferred: restates `PullRequestView.ciResult()` and the shell's mark cell, not read from `review` — move both onto review's view when the protocol next changes for it.
/** A pull request's CI result: the outcome of its worst check, since `review` lists them worst first; none when it has no check. */
export const ciResultOf = (pullRequest: ReviewPullRequestView): ReviewCheckOutcome | undefined => pullRequest.checks[0]?.outcome;

/** The dim mark beside a ticket's pull request in the TUI: a draft, or one with no check to report. */
export const hasMark = (pullRequest: ReviewPullRequestView): boolean => pullRequest.draft || ciResultOf(pullRequest) === undefined;

const rankOf = (pullRequest: ReviewPullRequestView): number => {
  const result = ciResultOf(pullRequest);
  const rank = result === undefined ? WORST_FIRST.length : WORST_FIRST.indexOf(result);
  return pullRequest.draft ? rank + WORST_FIRST.length + 1 : rank;
};

// deferred: this order is the window's, as the TUI lists no pull requests — move it into `review` when a second frontend lists them.
/** The pull requests failed first, then pending, then passed or unchecked, every draft after; equals keep the order they came in. */
export const worstFirst = (pullRequests: readonly ReviewPullRequestView[]): ReviewPullRequestView[] =>
  [...pullRequests].sort((a, b) => rankOf(a) - rankOf(b));

/** The worst CI result among the pull requests that are not drafts, failed over pending over passed, so it agrees with the list's top row; none when no such one has a check. */
export const worstResult = (pullRequests: readonly ReviewPullRequestView[]): ReviewCheckOutcome | undefined =>
  WORST_FIRST.find((outcome) => pullRequests.some((pullRequest) => !pullRequest.draft && ciResultOf(pullRequest) === outcome));

const CI: Record<ReviewCheckOutcome, Ci> = { FAILED: Ci.Failed, PENDING: Ci.Pending, PASSED: Ci.Passed };

/** An outcome as the glyph that draws it. */
export const ciOf = (outcome: ReviewCheckOutcome | undefined): Ci | undefined => (outcome === undefined ? undefined : CI[outcome]);

/** A ticket's pull request as its row draws it: the mark and the glyph of its CI result. */
export const cellsOf = (pullRequest: ReviewPullRequestView): PullRequestCells => ({ marked: hasMark(pullRequest), ci: ciOf(ciResultOf(pullRequest)) });
