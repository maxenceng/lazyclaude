import type { ReviewDiffFileView, ReviewDiffLine, ReviewPendingCommentView, ReviewPendingInDiffView, ReviewPullRequestDiffView, ReviewThreadInDiffView, ReviewThreadLine, ReviewReviewThreadView } from '../protocol';

export enum RowKind {
  File = 'file',
  Hunk = 'hunk',
  Line = 'line',
  Thread = 'thread',
  Pending = 'pending',
  Outside = 'outside',
}

/** A file's rule; `head` when `review` places comments under it, which makes it a stop of the cursor. */
export interface FileRow {
  kind: RowKind.File;
  file: ReviewDiffFileView;
  index: number;
  head: boolean;
}

export interface HunkRow {
  kind: RowKind.Hunk;
  header: string;
}

/** A diff line, numbered from 0 in the order the lines are listed. */
export interface LineRow {
  kind: RowKind.Line;
  line: ReviewDiffLine;
  index: number;
  file: number;
}

/** A thread's card; `lineless` when `review` places it under its file's rule or after the diff rather than under a line. */
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

/** The rule after the diff's last file, over what `review` places on files the diff does not hold. */
export interface OutsideRow {
  kind: RowKind.Outside;
}

export type DiffRow = FileRow | HunkRow | LineRow | ThreadRow | PendingRow | OutsideRow;

export enum StopKind {
  Line = 'line',
  Head = 'head',
  Outside = 'outside',
}

/** Where the cursor can stand: a diff line, the rule of a file with comments under it, or the rule after the diff; `file` is the files rail's row, one past the last for after the diff. */
export type Stop = { kind: StopKind.Line; line: number; file: number } | { kind: StopKind.Head; file: number } | { kind: StopKind.Outside; file: number };

/** Whether `review` places a comment at `line` where the cursor's `stop` is. */
export const placedAt = (line: ReviewThreadLine, stop: Stop): boolean => {
  switch (line.kind) {
    case 'onDiffLine':
      return stop.kind === StopKind.Line && stop.line === line.index;
    case 'atFileHead':
      return stop.kind === StopKind.Head && stop.file === line.file;
    case 'outsideDiff':
      return stop.kind === StopKind.Outside;
  }
};

/** Whether two comments are placed at one place: one line, one file's rule, or after the diff. */
export const samePlace = (one: ReviewThreadLine, other: ReviewThreadLine): boolean => {
  switch (one.kind) {
    case 'onDiffLine':
      return other.kind === 'onDiffLine' && other.index === one.index;
    case 'atFileHead':
      return other.kind === 'atFileHead' && other.file === one.file;
    case 'outsideDiff':
      return other.kind === 'outsideDiff';
  }
};

const sameStop = (one: Stop, other: Stop): boolean => one.kind === other.kind && one.file === other.file && (one.kind !== StopKind.Line || (other.kind === StopKind.Line && one.line === other.line));

const threadRow = (placed: ReviewThreadInDiffView): DiffRow => ({ kind: RowKind.Thread, thread: placed.thread, lineless: placed.line.kind !== 'onDiffLine' });

const pendingRow = (placed: ReviewPendingInDiffView): DiffRow => ({ kind: RowKind.Pending, comment: placed.comment, lineless: placed.line.kind !== 'onDiffLine' });

const isHeadOf =
  (file: number) =>
  (placed: { line: ReviewThreadLine }): boolean =>
    placed.line.kind === 'atFileHead' && placed.line.file === file;

const byLine = <T extends { line: ReviewThreadLine }>(placed: T[]): Map<number, T[]> => {
  const groups = new Map<number, T[]>();
  placed.forEach((entry) => {
    if (entry.line.kind === 'onDiffLine') {
      groups.set(entry.line.index, [...(groups.get(entry.line.index) ?? []), entry]);
    }
  });
  return groups;
};

const isOutside = (placed: { line: ReviewThreadLine }): boolean => placed.line.kind === 'outsideDiff';

/** The rows of a review's main view and the stops of its cursor, built whenever the diff or `review`'s answer of which comments show, and where, changes. */
export class DiffRows {
  private readonly lineRows: LineRow[];
  private readonly cursorStops: Stop[];

  private constructor(
    private readonly diffFiles: ReviewDiffFileView[],
    private readonly rows: DiffRow[],
    private readonly threadLines: ReadonlySet<number>,
    private readonly pendingLines: ReadonlySet<number>,
  ) {
    this.lineRows = rows.filter((row): row is LineRow => row.kind === RowKind.Line);
    this.cursorStops = rows.flatMap((row): Stop[] => {
      switch (row.kind) {
        case RowKind.File:
          return row.head ? [{ kind: StopKind.Head, file: row.index }] : [];
        case RowKind.Line:
          return [{ kind: StopKind.Line, line: row.index, file: row.file }];
        case RowKind.Outside:
          return [{ kind: StopKind.Outside, file: diffFiles.length }];
        default:
          return [];
      }
    });
  }

  /**
   * The rows of `diff`, each thread `review` shows and each pending comment where `review` places it: under its line, under its file's rule
   * before the first hunk, or under a rule after the last file; none when the diff was not read.
   */
  static of(diff: ReviewPullRequestDiffView, shown: ReviewThreadInDiffView[], pending: ReviewPendingInDiffView[]): DiffRows {
    if (diff.kind !== 'readDiff') {
      return new DiffRows([], [], new Set(), new Set());
    }
    const files = diff.files;
    const threadsAt = byLine(shown);
    const pendingAt = byLine(pending);
    let next = 0;
    const fileRows = files.flatMap((file, fileIndex): DiffRow[] => {
      const head = [...shown.filter(isHeadOf(fileIndex)).map(threadRow), ...pending.filter(isHeadOf(fileIndex)).map(pendingRow)];
      return [
        { kind: RowKind.File, file, index: fileIndex, head: head.length > 0 },
        ...head,
        ...file.hunks.flatMap((hunk): DiffRow[] => [
          { kind: RowKind.Hunk, header: hunk.header },
          ...hunk.lines.flatMap((line): DiffRow[] => {
            const index = next;
            next += 1;
            return [{ kind: RowKind.Line, line, index, file: fileIndex }, ...(threadsAt.get(index) ?? []).map(threadRow), ...(pendingAt.get(index) ?? []).map(pendingRow)];
          }),
        ]),
      ];
    });
    const outside = [...shown.filter(isOutside).map(threadRow), ...pending.filter(isOutside).map(pendingRow)];
    return new DiffRows(files, [...fileRows, ...(outside.length > 0 ? [{ kind: RowKind.Outside } satisfies DiffRow, ...outside] : [])], new Set(threadsAt.keys()), new Set(pendingAt.keys()));
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

  stops(): Stop[] {
    return this.cursorStops;
  }

  /** The stop of diff line `line`; none when the diff has no such line. */
  stopOfLine(line: number): number | undefined {
    const index = this.cursorStops.findIndex((stop) => stop.kind === StopKind.Line && stop.line === line);
    return index === -1 ? undefined : index;
  }

  /** Where `stop` is among these stops, if it still is one. */
  indexOf(stop: Stop): number | undefined {
    const index = this.cursorStops.findIndex((candidate) => sameStop(candidate, stop));
    return index === -1 ? undefined : index;
  }

  /** The first stop of the file at `file`: its rule when comments hang there, else its first line; none for a file with neither. */
  firstStopOf(file: number): number | undefined {
    const index = this.cursorStops.findIndex((stop) => stop.file === file);
    return index === -1 ? undefined : index;
  }

  /** The first stop of the next file that has one, after the diff's rule included; none from the last. */
  firstStopOfNextFile(stop: number): number | undefined {
    const file = this.cursorStops[stop]?.file ?? 0;
    const index = this.cursorStops.findIndex((candidate) => candidate.file > file);
    return index === -1 ? undefined : index;
  }

  /** The first stop of the file before the one `stop` is in. */
  firstStopOfPreviousFile(stop: number): number | undefined {
    const file = this.cursorStops[stop]?.file ?? 0;
    const before = this.cursorStops.findLast((candidate) => candidate.file < file);
    return before === undefined ? undefined : this.firstStopOf(before.file);
  }
}

/** How many lines a file adds and how many it removes, over all its hunks. */
export const deltaOf = (file: ReviewDiffFileView): { added: number; removed: number } => {
  const lines = file.hunks.flatMap((hunk) => hunk.lines);
  return { added: lines.filter((line) => line.kind === 'addedLine').length, removed: lines.filter((line) => line.kind === 'removedLine').length };
};
