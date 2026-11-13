import type { ReviewCommentInDiffView, ReviewDiffFileView, ReviewDiffLine, ReviewPendingCommentView, ReviewPendingInDiffView, ReviewPullRequestDiffView, ReviewThreadInDiffView, ReviewReviewThreadView } from '../protocol';

export enum RowKind {
  File = 'file',
  Hunk = 'hunk',
  Line = 'line',
  Thread = 'thread',
  Pending = 'pending',
}

export interface FileRow {
  kind: RowKind.File;
  file: ReviewDiffFileView;
}

export interface HunkRow {
  kind: RowKind.Hunk;
  header: string;
}

/** A diff line, numbered from 0 in the order the lines are listed, which is where the cursor is. */
export interface LineRow {
  kind: RowKind.Line;
  line: ReviewDiffLine;
  index: number;
  file: number;
}

/** A thread's card; `lineless` when `review` places it on no line, so it stands at the top of its file. */
export interface ThreadRow {
  kind: RowKind.Thread;
  thread: ReviewReviewThreadView;
  lineless: boolean;
}

export interface PendingRow {
  kind: RowKind.Pending;
  comment: ReviewPendingCommentView;
  lineless: boolean;
}

export type DiffRow = FileRow | HunkRow | LineRow | ThreadRow | PendingRow;

const onLine = (line: ReviewThreadInDiffView['line']): number | undefined => (line.kind === 'onDiffLine' ? line.index : undefined);

const pathOf = (entry: ReviewCommentInDiffView): string => (entry.kind === 'threadInDiff' ? entry.thread.path : entry.comment.anchor.path);

const rowOf = (entry: ReviewCommentInDiffView): DiffRow =>
  entry.kind === 'threadInDiff' ? { kind: RowKind.Thread, thread: entry.thread, lineless: true } : { kind: RowKind.Pending, comment: entry.comment, lineless: true };

const groupedBy = <T>(items: T[], keyOf: (item: T) => number | undefined): Map<number, T[]> => {
  const groups = new Map<number, T[]>();
  items.forEach((item) => {
    const key = keyOf(item);
    if (key !== undefined) {
      groups.set(key, [...(groups.get(key) ?? []), item]);
    }
  });
  return groups;
};

/** The rows of a review's main view, built whenever the diff or `review`'s answer of which comments show changes. */
export class DiffRows {
  private readonly lineRows: LineRow[];
  private readonly starts: number[];

  private constructor(
    private readonly diffFiles: ReviewDiffFileView[],
    private readonly rows: DiffRow[],
    private readonly threadLines: ReadonlySet<number>,
    private readonly pendingLines: ReadonlySet<number>,
  ) {
    this.lineRows = rows.filter((row): row is LineRow => row.kind === RowKind.Line);
    this.starts = this.lineRows.flatMap((row, index) => (index === 0 || this.lineRows[index - 1]?.file !== row.file ? [index] : []));
  }

  /**
   * The rows of `diff` with the threads `review` shows under its lines and the pending comments; none when the diff was not read.
   * What `review` places on no line (`lineless`) stands once under the header of its file, before the first hunk; one whose file the diff does not hold is not shown.
   */
  static of(diff: ReviewPullRequestDiffView, underLines: ReviewThreadInDiffView[], pending: ReviewPendingInDiffView[], lineless: ReviewCommentInDiffView[] = []): DiffRows {
    const files = diff.kind === 'readDiff' ? diff.files : [];
    const threadsAt = groupedBy(underLines, (placed) => onLine(placed.line));
    const pendingAt = groupedBy(pending, (placed) => onLine(placed.line));
    let next = 0;
    const rows = files.flatMap((file, fileIndex) => [
      { kind: RowKind.File, file } satisfies DiffRow,
      ...lineless.filter((entry) => pathOf(entry) === file.path && onLine(entry.line) === undefined).map(rowOf),
      ...file.hunks.flatMap((hunk): DiffRow[] => [
        { kind: RowKind.Hunk, header: hunk.header },
        ...hunk.lines.flatMap((line): DiffRow[] => {
          const index = next;
          next += 1;
          return [
            { kind: RowKind.Line, line, index, file: fileIndex },
            ...(threadsAt.get(index) ?? []).map((placed): DiffRow => ({ kind: RowKind.Thread, thread: placed.thread, lineless: false })),
            ...(pendingAt.get(index) ?? []).map((placed): DiffRow => ({ kind: RowKind.Pending, comment: placed.comment, lineless: false })),
          ];
        }),
      ]),
    ]);
    return new DiffRows(files, rows, new Set(threadsAt.keys()), new Set(pendingAt.keys()));
  }

  all(): DiffRow[] {
    return this.rows;
  }

  files(): ReviewDiffFileView[] {
    return this.diffFiles;
  }

  /** Whether a thread is shown under diff line `line`. */
  hasThread(line: number): boolean {
    return this.threadLines.has(line);
  }

  /** Whether a pending comment is shown under diff line `line`. */
  hasPending(line: number): boolean {
    return this.pendingLines.has(line);
  }

  lineCount(): number {
    return this.lineRows.length;
  }

  fileOfLine(line: number): number {
    return this.lineRows[line]?.file ?? 0;
  }

  /** The first line of the file at `file`; none for a file with no lines, as a binary or a plainly renamed one. */
  firstLineOf(file: number): number | undefined {
    return this.starts.find((start) => this.fileOfLine(start) === file);
  }

  firstLineOfNextFile(line: number): number | undefined {
    return this.starts.find((start) => start > line);
  }

  /** The first line of the file before the one `line` is in. */
  firstLineOfPreviousFile(line: number): number | undefined {
    const current = this.firstLineOf(this.fileOfLine(line)) ?? 0;
    return [...this.starts].reverse().find((start) => start < current);
  }
}

/** How many lines a file adds and how many it removes, over all its hunks. */
export const deltaOf = (file: ReviewDiffFileView): { added: number; removed: number } => {
  const lines = file.hunks.flatMap((hunk) => hunk.lines);
  return { added: lines.filter((line) => line.kind === 'addedLine').length, removed: lines.filter((line) => line.kind === 'removedLine').length };
};
