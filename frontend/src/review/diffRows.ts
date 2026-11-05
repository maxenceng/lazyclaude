import type { DiffFileView, DiffLine, PendingCommentView, PendingInDiffView, PullRequestDiffView, ThreadInDiffView, ThreadView } from '../protocol';

export enum RowKind {
  File = 'file',
  Hunk = 'hunk',
  Line = 'line',
  Thread = 'thread',
  Pending = 'pending',
}

export interface FileRow {
  kind: RowKind.File;
  file: DiffFileView;
}

export interface HunkRow {
  kind: RowKind.Hunk;
  header: string;
}

/** A diff line, numbered from 0 in the order the lines are listed, which is where the cursor is. */
export interface LineRow {
  kind: RowKind.Line;
  line: DiffLine;
  index: number;
  file: number;
}

export interface ThreadRow {
  kind: RowKind.Thread;
  thread: ThreadView;
}

export interface PendingRow {
  kind: RowKind.Pending;
  comment: PendingCommentView;
}

export type DiffRow = FileRow | HunkRow | LineRow | ThreadRow | PendingRow;

const onLine = (line: ThreadInDiffView['line']): number | undefined => (line.kind === 'onDiffLine' ? line.index : undefined);

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

/** The rows of a review's main view, built whenever the diff, the threads or whether resolved threads show change. */
export class DiffRows {
  private readonly lineRows: LineRow[];
  private readonly starts: number[];

  private constructor(
    private readonly diffFiles: DiffFileView[],
    private readonly rows: DiffRow[],
  ) {
    this.lineRows = rows.filter((row): row is LineRow => row.kind === RowKind.Line);
    this.starts = this.lineRows.flatMap((row, index) => (index === 0 || this.lineRows[index - 1]?.file !== row.file ? [index] : []));
  }

  /** The rows of `diff` with the threads and pending comments that show under its lines; none when the diff was not read. */
  static of(diff: PullRequestDiffView, threads: ThreadInDiffView[], pending: PendingInDiffView[], showResolved: boolean): DiffRows {
    const files = diff.kind === 'readDiff' ? diff.files : [];
    const threadsAt = groupedBy(
      threads.filter((placed) => showResolved || !placed.thread.resolved),
      (placed) => onLine(placed.line),
    );
    const pendingAt = groupedBy(pending, (placed) => onLine(placed.line));
    let next = 0;
    const rows = files.flatMap((file, fileIndex) => [
      { kind: RowKind.File, file } satisfies DiffRow,
      ...file.hunks.flatMap((hunk): DiffRow[] => [
        { kind: RowKind.Hunk, header: hunk.header },
        ...hunk.lines.flatMap((line): DiffRow[] => {
          const index = next;
          next += 1;
          return [
            { kind: RowKind.Line, line, index, file: fileIndex },
            ...(threadsAt.get(index) ?? []).map((placed): DiffRow => ({ kind: RowKind.Thread, thread: placed.thread })),
            ...(pendingAt.get(index) ?? []).map((placed): DiffRow => ({ kind: RowKind.Pending, comment: placed.comment })),
          ];
        }),
      ]),
    ]);
    return new DiffRows(files, rows);
  }

  all(): DiffRow[] {
    return this.rows;
  }

  files(): DiffFileView[] {
    return this.diffFiles;
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
