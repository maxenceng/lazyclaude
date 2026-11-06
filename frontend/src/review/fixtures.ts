import type { ReviewCommentsInDiffView, ReviewDiffFileView, ReviewDiffLine, ReviewHunkView, ReviewOpenPendingReviewView, ReviewPendingCommentView, ReviewPendingInDiffView, ReviewPlacedThread, ReviewPullRequestView, ReviewReviewThreadView, ReviewThreadInDiffView, ReviewThreadLine } from '../protocol';

export const added = (newNumber: number, content: string): ReviewDiffLine => ({ kind: 'addedLine', content, newNumber });

export const removed = (oldNumber: number, content: string): ReviewDiffLine => ({ kind: 'removedLine', content, oldNumber });

export const kept = (oldNumber: number, newNumber: number, content: string): ReviewDiffLine => ({ kind: 'contextLine', content, oldNumber, newNumber });

export const hunk = (header: string, lines: ReviewDiffLine[]): ReviewHunkView => ({ header, lines });

export const fileFixture = (path: string, hunks: ReviewHunkView[], status: ReviewDiffFileView['status'] = { kind: 'modifiedFile' }): ReviewDiffFileView => ({ path, status, hunks });

export const diffFixture = (files: ReviewDiffFileView[]) => ({ kind: 'readDiff' as const, files });

export const placed = (first: number, last = first, side: ReviewPlacedThread['side'] = 'RIGHT'): ReviewPlacedThread => ({ kind: 'placedThread', side, first, last });

export const threadFixture = (overrides: Partial<ReviewReviewThreadView> = {}): ReviewReviewThreadView => ({
  id: 'T1',
  path: 'src/a.ts',
  position: placed(1),
  resolved: false,
  open: true,
  comments: [{ author: 'ada', body: 'Why this?' }],
  ...overrides,
});

export const onLine = (index: number): ReviewThreadLine => ({ kind: 'onDiffLine', index });

export const noLine: ReviewThreadLine = { kind: 'noDiffLine' };

export const threadInDiff = (thread: ReviewReviewThreadView, line: ReviewThreadLine): ReviewThreadInDiffView => ({ kind: 'threadInDiff', thread, line });

export const threadsFixture = (threads: ReviewReviewThreadView[]) => ({ kind: 'readThreads' as const, threads });

export const pendingCommentFixture = (overrides: Partial<ReviewPendingCommentView> = {}): ReviewPendingCommentView => ({
  id: 'C1',
  body: 'Rename this',
  anchor: { kind: 'onDiffAnchor', path: 'src/a.ts', position: placed(1) },
  ...overrides,
});

export const pendingInDiff = (comment: ReviewPendingCommentView, line: ReviewThreadLine): ReviewPendingInDiffView => ({ kind: 'pendingInDiff', comment, line });

export const openPending = (comments: ReviewPendingCommentView[], overrides: Partial<ReviewOpenPendingReviewView> = {}): ReviewOpenPendingReviewView => ({
  kind: 'openPendingReview',
  summary: '',
  comments,
  headMove: { kind: 'heldHead' },
  ...overrides,
});

export const pullRequestFixture = (overrides: Partial<ReviewPullRequestView> = {}): ReviewPullRequestView => ({
  number: 7,
  title: 'Add the thing',
  head: 'proj-1-first',
  draft: false,
  checks: [],
  ...overrides,
});

/** What `review/commentsInDiff` answers: every list empty unless given. */
export const commentsFixture = (overrides: Partial<ReviewCommentsInDiffView> = {}): ReviewCommentsInDiffView => ({
  threads: [],
  pending: [],
  underLines: [],
  listed: [],
  lastOnLines: [],
  ...overrides,
});
