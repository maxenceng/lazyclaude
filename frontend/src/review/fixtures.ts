import type {
  DiffFileView,
  DiffLine,
  HunkView,
  OpenPendingReviewView,
  PendingCommentView,
  PendingInDiffView,
  PlacedThread,
  PullRequestView,
  ThreadInDiffView,
  ThreadLine,
  ThreadView,
} from '../protocol';

export const added = (newNumber: number, content: string): DiffLine => ({ kind: 'addedLine', content, newNumber });

export const removed = (oldNumber: number, content: string): DiffLine => ({ kind: 'removedLine', content, oldNumber });

export const kept = (oldNumber: number, newNumber: number, content: string): DiffLine => ({ kind: 'contextLine', content, oldNumber, newNumber });

export const hunk = (header: string, lines: DiffLine[]): HunkView => ({ header, lines });

export const fileFixture = (path: string, hunks: HunkView[], status: DiffFileView['status'] = { kind: 'modifiedFile' }): DiffFileView => ({ path, status, hunks });

export const diffFixture = (files: DiffFileView[]) => ({ kind: 'readDiff' as const, files });

export const placed = (first: number, last = first, side: PlacedThread['side'] = 'RIGHT'): PlacedThread => ({ kind: 'placedThread', side, first, last });

export const threadFixture = (overrides: Partial<ThreadView> = {}): ThreadView => ({
  id: 'T1',
  path: 'src/a.ts',
  position: placed(1),
  resolved: false,
  open: true,
  comments: [{ author: 'ada', body: 'Why this?' }],
  ...overrides,
});

export const onLine = (index: number): ThreadLine => ({ kind: 'onDiffLine', index });

export const noLine: ThreadLine = { kind: 'noDiffLine' };

export const threadInDiff = (thread: ThreadView, line: ThreadLine): ThreadInDiffView => ({ thread, line });

export const threadsFixture = (threads: ThreadView[]) => ({ kind: 'readThreads' as const, threads });

export const pendingCommentFixture = (overrides: Partial<PendingCommentView> = {}): PendingCommentView => ({
  id: 'C1',
  body: 'Rename this',
  anchor: { kind: 'onDiffAnchor', path: 'src/a.ts', position: placed(1) },
  ...overrides,
});

export const pendingInDiff = (comment: PendingCommentView, line: ThreadLine): PendingInDiffView => ({ comment, line });

export const openPending = (comments: PendingCommentView[], overrides: Partial<OpenPendingReviewView> = {}): OpenPendingReviewView => ({
  kind: 'openPendingReview',
  summary: '',
  comments,
  headMove: { kind: 'heldHead' },
  ...overrides,
});

export const pullRequestFixture = (overrides: Partial<PullRequestView> = {}): PullRequestView => ({
  number: 7,
  title: 'Add the thing',
  head: 'proj-1-first',
  draft: false,
  checks: [],
  ...overrides,
});
