import { i18n } from '../i18n/i18n';
import type { Client, PendingCommentView, PendingInDiffView, PendingReviewView, PendingReviewWriteView, PullRequestDiffView, ThreadInDiffView, ThreadsView } from '../protocol';
import { Store } from '../shared/store';
import { CommentBox, TargetKind } from './commentBox';
import { DiffRows } from './diffRows';
import { type DiffState, EmptyDiff, FaultedDiff, LoadingDiff, PopulatedDiff, UnavailableDiff } from './diffState';

export interface ReviewDeps {
  client: Client;
  root: string;
  number: number;
  title: string;
  /** The status line's notice: what a gesture of the review did not do, and why. */
  refuse: (text: string) => void;
  /** GitHub took the submit. */
  onSubmitted: () => void;
}

export enum EntryKind {
  Pending = 'pending',
  Thread = 'thread',
}

export type CommentEntry = { kind: EntryKind.Pending; placed: PendingInDiffView } | { kind: EntryKind.Thread; placed: ThreadInDiffView };

const HALF = 2;

const messageOf = (error: unknown): string => (error instanceof Error ? error.message : String(error));

const clamp = (index: number, size: number): number => Math.min(Math.max(index, 0), Math.max(0, size - 1));

/**
 * What the review screen shows of one pull request: its diff, threads and pending review as last read, the cursor over the diff lines
 * and the Files and Comments selections. A read lands only if no later read of the same kind was begun since, and the session is
 * closed when the review is left, which drops every read still under way.
 */
export class ReviewSession {
  readonly changed: Store<number> = new Store(0);
  diff: PullRequestDiffView = { kind: 'loadingDiff' };
  threads: ThreadsView = { kind: 'loadingThreads' };
  pending: PendingReviewView = { kind: 'loadingPendingReview' };
  private diffFault: string | undefined;
  private placedThreads: ThreadInDiffView[] = [];
  private placedPending: PendingInDiffView[] = [];
  private diffRows: DiffRows = DiffRows.of({ kind: 'loadingDiff' }, [], [], false);
  private showResolved = false;
  private line = 0;
  private fileSelected = 0;
  private commentSelected = 0;
  private viewLines = 0;
  private diffReads = 0;
  private threadsReads = 0;
  private pendingReads = 0;
  private placements = 0;
  private closed = false;
  private rangeStart: number | undefined;
  private rangeEnd: number | undefined;
  private dragFrom: number | undefined;
  private open: CommentBox | undefined;
  private typedSummary: string | undefined;
  private confirming = false;
  private writes: Promise<void> = Promise.resolve();
  private waiting = 0;

  constructor(private readonly deps: ReviewDeps) {}

  number(): number {
    return this.deps.number;
  }

  title(): string {
    return this.deps.title;
  }

  /** Throws away what reads are still under way. */
  close(): void {
    this.closed = true;
  }

  diffState(): DiffState {
    if (this.diffFault !== undefined) {
      return new FaultedDiff(this.diffFault);
    }
    switch (this.diff.kind) {
      case 'loadingDiff':
        return new LoadingDiff();
      case 'unavailableDiff':
        return new UnavailableDiff(this.diff.reason, this.diff.tooLarge);
      case 'readDiff':
        return this.diff.files.length === 0 ? new EmptyDiff() : new PopulatedDiff(this.diff.files);
    }
  }

  rows(): DiffRows {
    return this.diffRows;
  }

  cursor(): number {
    return this.line;
  }

  selectedFile(): number {
    return this.fileSelected;
  }

  selectedComment(): number {
    return this.commentSelected;
  }

  resolvedShown(): boolean {
    return this.showResolved;
  }

  /** The pending comments in diff order, the ones on no line last; none until the pending review is read. */
  pendingComments(): PendingInDiffView[] {
    return this.placedPending;
  }

  /** The threads in diff order, the ones on no line last; none until they are read. */
  threadsPlaced(): ThreadInDiffView[] {
    return this.placedThreads;
  }

  /** What Comments lists: the pending comments first, then the threads. */
  entries(): CommentEntry[] {
    return [
      ...this.placedPending.map((placed): CommentEntry => ({ kind: EntryKind.Pending, placed })),
      ...this.placedThreads.map((placed): CommentEntry => ({ kind: EntryKind.Thread, placed })),
    ];
  }

  /** Reads the pending review, the diff and the threads again; each lands unless a later read of its kind was begun. */
  read(): void {
    this.diffFault = undefined;
    if (this.diff.kind !== 'readDiff') {
      this.diff = { kind: 'loadingDiff' };
    }
    const params = { root: this.deps.root, number: this.deps.number };
    void this.readPending(params);
    void this.readDiff(params);
    void this.readThreads(params);
    this.touch();
  }

  private async readPending(params: { root: string; number: number }): Promise<void> {
    this.pendingReads += 1;
    const read = this.pendingReads;
    const view = await this.client('review/pendingOf', params).catch((error: unknown): PendingReviewView => ({ kind: 'unavailablePendingReview', reason: messageOf(error) }));
    if (read === this.pendingReads && !this.closed) {
      this.pending = view;
      await this.place();
    }
  }

  private async readDiff(params: { root: string; number: number }): Promise<void> {
    this.diffReads += 1;
    const read = this.diffReads;
    try {
      const view = await this.deps.client.call('review/diffOf', params);
      if (read === this.diffReads && !this.closed) {
        this.diff = view;
        await this.place();
      }
    } catch (error) {
      if (read === this.diffReads && !this.closed) {
        this.diffFault = messageOf(error);
        this.touch();
      }
    }
  }

  private async readThreads(params: { root: string; number: number }): Promise<void> {
    this.threadsReads += 1;
    const read = this.threadsReads;
    const view = await this.client('review/threadsOf', params).catch((error: unknown): ThreadsView => ({ kind: 'unavailableThreads', reason: messageOf(error) }));
    if (read === this.threadsReads && !this.closed) {
      this.threads = view;
      await this.place();
    }
  }

  private client<M extends 'review/pendingOf' | 'review/threadsOf'>(method: M, params: { root: string; number: number }) {
    return this.deps.client.call(method, params);
  }

  /** Asks where the threads and the pending comments sit on the diff, and builds the rows from the answer; only the latest ask lands. */
  private async place(): Promise<void> {
    this.placements += 1;
    const placement = this.placements;
    const diff = this.diff;
    const threads = this.threads;
    const pending = this.pending;
    try {
      const [placedThreads, placedPending] = await Promise.all([
        diff.kind === 'readDiff' && threads.kind === 'readThreads' ? this.deps.client.call('review/threadsInDiff', { diff, threads }) : [],
        diff.kind === 'readDiff' && pending.kind === 'openPendingReview' ? this.deps.client.call('review/pendingInDiff', { diff, pending }) : [],
      ]);
      if (placement === this.placements && !this.closed) {
        this.placedThreads = placedThreads;
        this.placedPending = placedPending;
        this.rebuild();
      }
    } catch (error) {
      if (placement === this.placements && !this.closed) {
        this.deps.refuse(messageOf(error));
      }
    }
  }

  private rebuild(): void {
    this.diffRows = DiffRows.of(this.diff, this.placedThreads, this.placedPending, this.showResolved);
    this.moveTo(this.line);
    this.commentSelected = clamp(this.commentSelected, this.entries().length);
    this.touch();
  }

  /** The cursor on diff line `line`, held within the lines there are; the Files selection goes to the cursor's file. */
  moveTo(line: number): void {
    this.line = clamp(line, this.diffRows.lineCount());
    this.fileSelected = this.diffRows.lineCount() === 0 ? clamp(this.fileSelected, this.diffRows.files().length) : this.diffRows.fileOfLine(this.line);
    this.touch();
  }

  moveLine(delta: number): void {
    this.moveTo(this.line + delta);
  }

  /** Takes how many diff lines the main view shows, which is what half a page is of. */
  observeLines(lines: number): void {
    this.viewLines = lines;
  }

  halfPage(down: boolean): void {
    const half = Math.max(1, Math.floor(this.viewLines / HALF));
    this.moveLine(down ? half : -half);
  }

  /** To the first line of the next file that has lines; nowhere from the last. */
  nextFile(): void {
    const target = this.diffRows.firstLineOfNextFile(this.line);
    if (target !== undefined) {
      this.moveTo(target);
    }
  }

  previousFile(): void {
    const target = this.diffRows.firstLineOfPreviousFile(this.line);
    if (target !== undefined && this.diffRows.lineCount() > 0) {
      this.moveTo(target);
    }
  }

  /** Selects the file at `index`, held within the files there are. */
  selectFile(index: number): void {
    this.fileSelected = clamp(index, this.diffRows.files().length);
    this.touch();
  }

  goToSelectedFile(): void {
    const target = this.diffRows.firstLineOf(this.fileSelected);
    if (target !== undefined) {
      this.moveTo(target);
    }
  }

  toggleResolved(): void {
    this.showResolved = !this.showResolved;
    this.rebuild();
  }

  /** Whether the diff line `line` is inside the range being marked. */
  inRange(line: number): boolean {
    return this.rangeStart !== undefined && line >= this.rangeLow() && line <= this.rangeHigh();
  }

  /** Drops the range marked; answers whether there was one. */
  clearRange(): boolean {
    const marked = this.rangeStart !== undefined;
    this.rangeStart = undefined;
    this.rangeEnd = undefined;
    this.touch();
    return marked;
  }

  private rangeLow(): number {
    return Math.min(this.rangeStart ?? this.line, this.rangeEnd ?? this.line);
  }

  private rangeHigh(): number {
    return Math.max(this.rangeStart ?? this.line, this.rangeEnd ?? this.line);
  }

  /** `v`: starts a range at the cursor, or ends the one started at it when its lines can take a comment; one that cannot is refused and left as it was. */
  async markRangeKey(): Promise<void> {
    if (this.rangeStart === undefined || this.rangeEnd !== undefined) {
      this.rangeStart = this.line;
      this.rangeEnd = undefined;
      this.touch();
      return;
    }
    await this.markRange(this.rangeStart, this.line);
  }

  /**
   * A drag: the cursor goes to diff line `to`, and the range from `from` to it is marked when its lines can take a comment. One that cannot
   * is refused with the reason, and the range marked before is left as it was.
   */
  async markRange(from: number, to: number): Promise<void> {
    this.moveTo(to);
    const start = clamp(from, this.diffRows.lineCount());
    const answer = await this.anchorOf(Math.min(start, this.line), Math.max(start, this.line));
    if (answer === undefined) {
      return;
    }
    if (answer.kind === 'onDiffAnchor') {
      this.rangeStart = start;
      this.rangeEnd = this.line;
      this.touch();
    } else {
      this.deps.refuse(answer.reason);
    }
  }

  private async anchorOf(first: number, last: number) {
    try {
      return await this.deps.client.call('review/anchorOf', { diff: this.diff, first, last });
    } catch (error) {
      this.deps.refuse(messageOf(error));
      return undefined;
    }
  }

  box(): CommentBox | undefined {
    return this.open;
  }

  submitPrompt(): boolean {
    return this.confirming;
  }

  /** What the summary box starts from and a submit sends: the one typed here, else the body of the adopted review, else nothing. */
  summaryToSubmit(): string {
    return this.typedSummary ?? (this.pending.kind === 'openPendingReview' ? this.pending.summary : '');
  }

  /** `c` on the cursor's line, or on the range marked: the box opens once the sidecar says a comment can hang there. */
  async comment(): Promise<void> {
    if (this.diffRows.lineCount() === 0) {
      return;
    }
    const marked = this.rangeStart !== undefined;
    const answer = await this.anchorOf(marked ? this.rangeLow() : this.line, marked ? this.rangeHigh() : this.line);
    if (answer?.kind === 'onDiffAnchor') {
      this.openBox(new CommentBox({ kind: TargetKind.New, path: answer.path, position: answer.position }));
    } else if (answer !== undefined) {
      this.deps.refuse(answer.reason);
    }
  }

  openSummary(): void {
    this.openBox(new CommentBox({ kind: TargetKind.Summary }, this.summaryToSubmit()));
  }

  typeInBox(text: string): void {
    this.open?.typed(text);
    this.touch();
  }

  /** Throws the box and what was typed in it away, and the range it was written on. */
  cancelBox(): void {
    this.open = undefined;
    this.rangeStart = undefined;
    this.rangeEnd = undefined;
    this.touch();
  }

  /** Saves what the box holds: the summary is kept until the submit, anything else is written off the render thread. */
  saveBox(): void {
    const box = this.open;
    if (box === undefined || box.saving) {
      return;
    }
    const { root, number } = this.deps;
    const body = box.text;
    switch (box.target.kind) {
      case TargetKind.Summary:
        this.typedSummary = body;
        this.cancelBox();
        this.confirming = true;
        this.touch();
        return;
      case TargetKind.New: {
        const { path, position } = box.target;
        return this.write(box, () => this.deps.client.call('review/comment', { root, number, path, position, body }));
      }
      case TargetKind.Edit: {
        const comment = box.target.comment.id;
        return this.write(box, () => this.deps.client.call('review/edit', { root, number, comment, body }));
      }
      case TargetKind.Reply: {
        const thread = box.target.thread.id;
        return this.write(box, () => this.deps.client.call('review/reply', { root, number, thread, body }));
      }
    }
  }

  /** The confirm after the summary is saved: yes submits the pending review as a Comment, anything else leaves it pending. */
  answerSubmit(yes: boolean): void {
    this.confirming = false;
    this.touch();
    if (yes) {
      const { root, number } = this.deps;
      const summary = this.summaryToSubmit();
      this.inOrder(
        () => this.deps.client.call('review/submit', { root, number, summary }),
        (answer) => {
          if (answer.kind === 'writtenReview') {
            this.deps.onSubmitted();
          } else {
            this.deps.refuse(i18n.t('review:refused.notSubmitted', { reason: answer.reason }));
          }
        },
      );
    }
  }

  /** `enter` on the cursor's line: its pending comment opens in the box. */
  enterInDiff(): void {
    const comment = this.pendingOnCursorLine();
    if (comment !== undefined) {
      this.openBox(new CommentBox({ kind: TargetKind.Edit, comment }, comment.body));
    }
  }

  /** `x` on the cursor's line deletes its pending comment. */
  deleteAtCursor(): void {
    const comment = this.pendingOnCursorLine();
    if (comment !== undefined) {
      this.delete(comment);
    }
  }

  /** The pending comment on the cursor's line that was written last, if the line has one. */
  private pendingOnCursorLine(): PendingCommentView | undefined {
    return this.placedPending.filter((placed) => placed.line.kind === 'onDiffLine' && placed.line.index === this.line).at(-1)?.comment;
  }

  selectComment(index: number): void {
    this.commentSelected = clamp(index, this.entries().length);
    this.touch();
  }

  moveComment(delta: number): void {
    this.selectComment(this.commentSelected + delta);
  }

  selectedEntry(): CommentEntry | undefined {
    return this.entries()[this.commentSelected];
  }

  /** To the line the selected entry shows under; nowhere for one on no line. */
  goToSelectedComment(): void {
    const line = this.selectedEntry()?.placed.line;
    if (line?.kind === 'onDiffLine') {
      this.moveTo(line.index);
    }
  }

  /** `enter` in Comments: goes to the entry's line and, on a pending comment, opens it in the box. */
  enterOnComment(): void {
    const entry = this.selectedEntry();
    if (entry !== undefined) {
      this.goToSelectedComment();
      if (entry.kind === EntryKind.Pending) {
        this.openBox(new CommentBox({ kind: TargetKind.Edit, comment: entry.placed.comment }, entry.placed.comment.body));
      }
    }
  }

  /** `x` in Comments deletes a pending comment. */
  deleteSelectedComment(): void {
    const entry = this.selectedEntry();
    if (entry?.kind === EntryKind.Pending) {
      this.goToSelectedComment();
      this.delete(entry.placed.comment);
    }
  }

  /** `c` in Comments replies to a thread. */
  commentOnSelectedComment(): void {
    const entry = this.selectedEntry();
    if (entry?.kind === EntryKind.Thread) {
      this.openBox(new CommentBox({ kind: TargetKind.Reply, thread: entry.placed.thread }));
    }
  }

  /** A press on a diff line: the cursor goes to it and a drag starts from it; a second press on the cursor's line in the focused diff is `enter`. */
  pressLine(line: number, focused: boolean): void {
    const second = focused && this.line === line;
    this.moveTo(line);
    this.dragFrom = line;
    if (second) {
      this.enterInDiff();
    }
  }

  /** The release of a drag, on the line nearest to where the pointer let go: the range between the two lines is marked unless they are one. */
  async releaseLine(line: number): Promise<void> {
    const from = this.dragFrom;
    this.dragFrom = undefined;
    if (from !== undefined && from !== line) {
      await this.markRange(from, line);
    }
  }

  /** A press on a file: it is selected, and a second press on the selected one goes to its first line. */
  pressFile(index: number, focused: boolean): void {
    const second = focused && index === this.fileSelected;
    this.selectFile(index);
    if (second) {
      this.goToSelectedFile();
    }
  }

  /** A press on a comment: it is selected, and a second press on the selected one is `enter`. */
  pressComment(index: number, focused: boolean): void {
    const second = focused && index === this.commentSelected;
    this.selectComment(index);
    if (second) {
      this.enterOnComment();
    }
  }

  private openBox(box: CommentBox): void {
    this.open = box;
    this.touch();
  }

  private delete(comment: PendingCommentView): void {
    const { root, number } = this.deps;
    this.inOrder(
      () => this.deps.client.call('review/delete', { root, number, comment: comment.id }),
      (answer) => {
        if (answer.kind === 'writtenReview') {
          this.applyWritten(answer.pending);
        } else {
          this.deps.refuse(i18n.t('review:refused.notDeleted', { reason: answer.reason }));
        }
      },
    );
  }

  /** Writes off the render thread; GitHub's answer closes the box, or keeps it with the reason it was refused. */
  private write(box: CommentBox, write: () => Promise<PendingReviewWriteView>): void {
    box.saveBegun();
    this.touch();
    this.inOrder(write, (answer) => {
      if (answer.kind === 'writtenReview') {
        this.applyWritten(answer.pending);
        this.cancelBox();
      } else {
        box.saveRefused(answer.reason);
        this.touch();
      }
    });
  }

  /** What a write answered with replaces what was read, and any read begun before it. */
  private applyWritten(pending: PendingReviewView): void {
    this.pending = pending;
    this.pendingReads += 1;
    void this.place();
  }

  /** Writes to the pending review one after the other; an answer for a review since left is dropped. */
  private inOrder(write: () => Promise<PendingReviewWriteView>, answered: (answer: PendingReviewWriteView) => void): void {
    this.waiting += 1;
    this.writes = this.waiting === 1 ? this.perform(write, answered) : this.writes.then(() => this.perform(write, answered));
  }

  private async perform(write: () => Promise<PendingReviewWriteView>, answered: (answer: PendingReviewWriteView) => void): Promise<void> {
    const answer = await write().catch((error: unknown): PendingReviewWriteView => ({ kind: 'refusedWrite', reason: messageOf(error) }));
    this.waiting -= 1;
    if (!this.closed) {
      answered(answer);
    }
  }

  private touch(): void {
    this.changed.set(this.changed.get() + 1);
  }
}
