/** What is said on a line: a thread on the pull request, or your own comment in the pending review. */
export enum CommentKind {
  Thread = 'thread',
  Pending = 'pending',
}

/** The mark each kind carries, in the diff, in Comments and on a file's counts. */
export const COMMENT_MARKS: Readonly<Record<CommentKind, string>> = { thread: '◆', pending: '✎' };
